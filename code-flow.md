_Last Updated: 2026-11-01_

# Code Flow: from `git push` to a running app

This guide follows the TinyURL app in time order, from a code change to a running app at `http://<host>:8080`, on the two pathways this repo supports:

| Pathway | Postgres runs in | The app runs in | Started by |
|---|---|---|---|
| **1. LOCAL** | Docker on your machine (`compose.yaml` → `postgres`) | Your local JVM (`./mvnw spring-boot:run` or the IDE) | You |
| **1b. LOCAL full stack** (optional) | Docker on your machine | Docker on your machine (built from the `Dockerfile`) | You |
| **2. ONLINE (EC2)** | Docker on the EC2 instance | Docker on the EC2 instance (built **on EC2** from the `Dockerfile`) | GitHub Actions over SSH |

Throughout this guide:

> 🆕 **INITIAL RUN**: happens only the first time (empty volume, no images, no cache)
> 🔁 **SUBSEQUENT UPDATE**: what happens on later runs or pushes
> ✅ **TODO**: something you have to do by hand

---

## 0. The building blocks and how they connect

```
                         ┌───────────────────────────── pom.xml ─────────────────────────────┐
                         │ dependencies + spring-boot-maven-plugin (fat jar, spring-boot:run)│
                         └───────────────┬───────────────────────────────┬───────────────────┘
                                         │ used by                       │ used by
                 ┌───────────────────────▼──────────┐      ┌─────────────▼──────────────────────┐
                 │ ./mvnw  (Maven Wrapper)          │      │ Dockerfile (stage 1: `mvn package`)│
                 │ .mvn/wrapper/maven-wrapper.props │      │ stage 2: `java -jar /app/app.jar`  │
                 │ downloads the pinned Maven once  │      └─────────────┬──────────────────────┘
                 └───────────────┬──────────────────┘                    │ build: .
                                 │                                       │
     LOCAL: spring-boot:run ─────┤                         ┌─────────────▼──────────────────────┐
     (spring-boot-docker-compose)│  runs `docker compose   │ compose.yaml  (project url_shortner)│
                                 └─ up` on ───────────────►│  • postgres        (postgres:17)   │
                                                           │  • spring-boot-url-shortener       │
     EC2: `docker compose up --build -d                    │      profiles: ["app"]             │
           spring-boot-url-shortener` ────────────────────►│  • volume postgres_data            │
                                                           └─────────────▲──────────────────────┘
                                                                         │ ${VAR:-default}
                                                           ┌─────────────┴──────────────────────┐
                                                           │ .env (git-ignored, per machine)    │
                                                           │ POSTGRES_DB/USER/PASSWORD,         │
                                                           │ APP_BASE_URL                       │
                                                           └────────────────────────────────────┘

     .github/workflows/deploy.yml ── job test-and-verify (./mvnw -B verify)
                                 └─ job deploy-to-ec2 (SSH → git reset → docker compose up --build)
```

| File | What it does | Read by |
|---|---|---|
| `pom.xml` | Lists dependencies, Java 25 and `spring-boot-maven-plugin` | `./mvnw`, `mvn` in the Dockerfile |
| `mvnw`, `mvnw.cmd`, `.mvn/wrapper/` | Maven Wrapper: downloads the pinned Maven version into `~/.m2/wrapper`, so no global Maven is needed | You, GitHub Actions |
| `src/main/resources/application.properties` | Local-dev defaults (`localhost:5432`, `postgres/postgres`, `app.base-url`) | Spring Boot at startup (env vars override it) |
| `src/main/resources/db/migration/V*.sql` | Flyway schema and seed data, applied in order and recorded in `flyway_schema_history` | Flyway at app startup |
| `compose.yaml` | Defines the `postgres` and `spring-boot-url-shortener` services, the network and the `postgres_data` volume | `docker compose`, `spring-boot-docker-compose` |
| `.env` / `.env.example` | Per-machine secrets. `docker compose` loads `.env` from the project folder automatically | `docker compose` |
| `Dockerfile` | Two-stage image build: compile with Maven, then run on a slim JRE | `docker compose up --build` |
| `.dockerignore` | Keeps `.git`, `target/`, IDE folders and **`.env`** out of the build context | `docker build` |
| `.github/workflows/deploy.yml` | CI (tests) and CD (SSH deploy to EC2) | GitHub Actions |
| `src/test/.../TestcontainersConfiguration.java` | Starts a throwaway `postgres:17` for the tests | `./mvnw verify` |

---

## 1. Maven / `./mvnw` commands

`./mvnw <args>` works exactly like `mvn <args>`, except that it first makes sure the Maven version pinned in `.mvn/wrapper/maven-wrapper.properties` is installed.
In Windows PowerShell use `.\mvnw.cmd <args>`. Git Bash, WSL, macOS and Linux use `./mvnw`.

Maven's default lifecycle runs in a fixed order. Asking for a phase runs every phase before it:

```
 validate ──► compile ──► test ──► package ──► integration-test ──► verify ──► install ──► deploy
               │            │         │             │                    │
               │            │         │             │                    └─ failsafe checks the IT results,
               │            │         │             │                       jacoco writes target/site/jacoco/index.html
               │            │         │             └─ failsafe runs *IT (full app + Testcontainers postgres:17)
               │            │         └─ target/spring-boot-url-shortener-v0.0.1-SNAPSHOT.jar
               │            │            then spring-boot-maven-plugin:repackage → executable fat jar
               │            └─ surefire runs *Test (unit + slice tests; repository slices start postgres:17 → needs Docker!)
               └─ javac → target/classes
```

| Command | What it does | Used where |
|---|---|---|
| `./mvnw spring-boot:run` | Compiles, then starts `UrlShortnerApp` from `target/classes` with devtools and docker-compose support on the classpath | LOCAL pathway |
| `./mvnw clean` | Deletes `target/` | Before a clean build |
| `./mvnw test` | compile + unit and slice tests (`*Test`, Surefire); no integration tests | Quick local check |
| `./mvnw -B verify` | Full lifecycle up to `verify`: compile, unit/slice tests, package, integration tests (`*IT`, Failsafe), coverage report. `-B` = batch mode (no colours or progress bars, suited to CI logs) | GitHub job `test-and-verify` |
| `./mvnw package -DskipTests` | Builds the fat jar without running tests | Optional local jar build |
| `java -jar target/*.jar` | Runs the fat jar. No docker-compose support inside it (see the note below), so start Postgres yourself first | Manual run |
| `mvn dependency:go-offline -B` | Downloads every dependency and plugin into `~/.m2` without compiling | Dockerfile stage 1 (cache layer) |
| `mvn -B clean package -DskipTests` | Builds the fat jar inside the image. Tests already ran in CI | Dockerfile stage 1 |

> **Why `spring-boot:run` and `java -jar` behave differently:** `spring-boot-docker-compose` and `spring-boot-devtools` are declared `optional`/`runtime`, and `spring-boot-maven-plugin` leaves both **out of the repackaged fat jar** by default.
> So `spring-boot:run` (or running from the IDE) starts Postgres for you, while the jar in the container makes no Docker calls and just connects to whatever `SPRING_DATASOURCE_URL` points at.

---

## 2. Docker / compose commands

| Command | What it does |
|---|---|
| `docker compose up -d postgres` | Starts only Postgres (creates the network and volume on the first run) |
| `docker compose up --build -d spring-boot-url-shortener` | Builds the app image from the `Dockerfile`, starts `postgres` first (through `depends_on`), waits until it is **healthy**, then starts the app. Naming the service activates its `app` profile |
| `docker compose --profile app up -d` | Same as above, started through the profile instead of the service name |
| `docker compose ps` / `docker compose --profile app ps` | Lists containers (without `--profile app`, the app container is hidden) |
| `docker logs -f shortener-app` | Follows the app's logs |
| `docker compose logs -f postgres` | Follows Postgres's logs |
| `docker compose --profile app down` | Stops and removes the containers and network. **The volume is kept** (data survives) |
| `docker compose --profile app down -v` | ⚠️ Also deletes `postgres_data`: **all data is gone**. Flyway re-seeds on the next start |
| `docker image prune -f` | Deletes *dangling* images (the old untagged app image after a rebuild). Keeps the build cache |
| `docker builder prune` | Clears the build cache. The next build re-downloads every dependency |

Names derived from `name: url_shortner` in `compose.yaml` (the same on every machine):

| Object | Name |
|---|---|
| Network | `url_shortner_default` (inside it, the app reaches the DB at `postgres:5432`) |
| Volume | `url_shortner_postgres_data` |
| Postgres container | `url_shortner-postgres-1` |
| App container | `shortener-app` (fixed by `container_name`) |
| App image | `url_shortner-spring-boot-url-shortener:latest` |

---

## 3. `compose.yaml` explained

```yaml
name: url_shortner                 # pinned project name → stable volume/container names everywhere

services:
  postgres:
    image: 'postgres:17'           # pulled from Docker Hub, not built
    environment:                   # ${VAR:-default}: from .env if set, otherwise the fallback
      POSTGRES_DB: ${POSTGRES_DB:-postgresDB}
      POSTGRES_USER: ${POSTGRES_USER:-postgres}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-postgres}
    ports:
      - '127.0.0.1:5432:5432'      # host-only: your local JVM can reach it, the internet cannot (EC2)
    volumes:
      - postgres_data:/var/lib/postgresql/data   # the data lives OUTSIDE the container
    healthcheck:                   # pg_isready every 5s, up to 10 tries → "healthy"
      ...
    restart: unless-stopped        # comes back after a reboot or a Docker restart

  spring-boot-url-shortener:
    container_name: shortener-app
    profiles: ["app"]              # NOT started by a plain `docker compose up` or by spring-boot:run
    build: { context: ., dockerfile: Dockerfile }
    environment:                   # Spring Boot maps SPRING_DATASOURCE_URL → spring.datasource.url, etc.
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB:-postgresDB}
      SPRING_DATASOURCE_USERNAME / _PASSWORD: from POSTGRES_USER / POSTGRES_PASSWORD
      APP_BASE_URL: ${APP_BASE_URL:-http://localhost:8080}   # → app.base-url (prefix of short links)
    ports: ["8080:8080"]
    depends_on:
      postgres: { condition: service_healthy }   # the app only starts once the DB accepts connections

volumes:
  postgres_data:                   # named volume: survives `down`, rebuilds and redeploys
```

🆕 **INITIAL RUN, volume empty:** the `postgres` image's entrypoint runs `initdb` and creates the database and user **from `POSTGRES_*` at this moment only**.
🔁 **SUBSEQUENT:** the volume already contains a database, so the `POSTGRES_*` values are **ignored**. Changing `POSTGRES_PASSWORD` in `.env` later does *not* change the DB password: change it with `ALTER USER` in `psql`, or wipe the volume with `down -v`. This is why the server `.env` must be written **before** the first deploy.

---

## 4. `Dockerfile` explained

```
 ┌──────────────── Stage 1: builder  (maven:3.9-eclipse-temurin-25, ~JDK + Maven) ────────────────┐
 │ WORKDIR /build                                                                                  │
 │ COPY pom.xml .                          ─┐ layer cached until pom.xml changes                   │
 │ RUN mvn dependency:go-offline -B        ─┘ → all jars in /root/.m2 *inside this layer*          │
 │ COPY src ./src                          ─┐ re-runs whenever anything in src/ changes            │
 │ RUN mvn -B clean package -DskipTests    ─┘ → /build/target/spring-boot-url-shortener-*.jar      │
 └───────────────────────────────────────────────────┬─────────────────────────────────────────────┘
                                                     │ COPY --from=builder /build/target/*.jar
 ┌──────────────── Stage 2: runtime  (eclipse-temurin:25-jre-alpine, small, no JDK/Maven) ────────┐
 │ WORKDIR /app                                                                                    │
 │ /app/app.jar   = fat jar: BOOT-INF/classes (your code + templates + SQL migrations)             │
 │                         + BOOT-INF/lib (runtime dependencies) + manifest Main-Class (launcher)  │
 │ EXPOSE 8080                                                                                     │
 │ ENTRYPOINT ["java", "-jar", "/app/app.jar"]                                                     │
 └─────────────────────────────────────────────────────────────────────────────────────────────────┘
   Only stage 2 ends up in the final image; stage 1 (Maven, sources, ~/.m2) is thrown away.
```

- **Layer caching is the dependency cache.** Docker caches each instruction and reuses it while its inputs are unchanged. The `pom.xml` → `go-offline` layer is therefore reused on every push that only touches `src/`, so no dependencies are downloaded. The build does **not** use a mounted `~/.m2` from the host.
- `mvnw` and `.mvn/` are excluded by `.dockerignore`. Stage 1 uses the `mvn` that ships with the base image.
- `.env` is excluded too, so secrets never end up in an image layer. They reach the container at **runtime** through `compose.yaml`'s `environment:`.
- Tests are skipped (`-DskipTests`) because GitHub Actions already ran them, and EC2 has no Testcontainers setup.

---

## 5. Pathway 1: LOCAL (Postgres in local Docker + app in your local JVM)

### ✅ Prerequisites
- [ ] **JDK 25** installed (`java -version`)
- [ ] **Docker Desktop running** (`docker info` must succeed). Both `spring-boot:run` and the tests need it
- [ ] Optional: `cp .env.example .env` to change the DB credentials. Without a `.env`, the `:-` fallbacks (`postgres/postgres`, `postgresDB`) are used and `APP_BASE_URL` stays `http://localhost:8080`

### Execution flow of `./mvnw spring-boot:run`

```
 [YOU]  ./mvnw spring-boot:run          (or ▶ Run UrlShortnerApp in the IDE, working dir = repo root)
   │
   ├── ① Maven Wrapper   🆕 downloads Maven into ~/.m2/wrapper     🔁 reuses it
   ├── ② Dependencies    🆕 downloads every jar from pom.xml into ~/.m2/repository
   │                     🔁 nothing (only new/changed versions after a pom.xml edit)
   ├── ③ compile         src/main/java → target/classes (incremental)
   │
   ▼  JVM starts UrlShortnerApp.main()
   │
   ├── ④ spring-boot-docker-compose finds compose.yaml and runs `docker compose up` for you
   │     • lifecycle-management=start_only → it starts the services, but does NOT stop them on exit
   │     • the app service has profiles: ["app"] → skipped; ONLY postgres starts
   │     🆕 pulls postgres:17, creates url_shortner_default + url_shortner_postgres_data, initdb
   │     🔁 container already running → nothing to do (fast start)
   │     • waits until the postgres healthcheck reports healthy
   │
   ├── ⑤ Service connection: Spring reads the container's POSTGRES_* env and port, and builds the
   │     DataSource from them (this overrides spring.datasource.* in application.properties)
   │
   ├── ⑥ Flyway          🆕 empty DB → runs V1 (tables), V2 (seed users/urls), V3 (passwords)
   │                     🔁 checks flyway_schema_history → runs only NEW V4__… files, if any
   ├── ⑦ Hibernate/JPA validates entities, Spring Security + Thymeleaf start
   ├── ⑧ Tomcat listens on :8080 (server.port=${PORT:8080})
   │
   └── ✔ http://localhost:8080   (log in as admin@gmail.com / admin)
         devtools: recompiling a class in the IDE triggers an automatic restart
```

### 🔁 Subsequent updates (local)
| You changed | Do this |
|---|---|
| Java / templates / CSS | Stop and re-run `./mvnw spring-boot:run`, or rebuild in the IDE (devtools restarts the app) |
| Added a migration `V4__….sql` | Restart the app. Flyway applies only V4. **Never edit an applied migration** (checksum mismatch → startup fails) |
| `pom.xml` | Re-run. Maven downloads only the new or changed dependencies |
| Want a fresh DB | `docker compose down -v`, then run again (the 🆕 path) |

Postgres keeps running after you stop the app (`start_only`). Stop it with `docker compose down`.

### Tests (local, and the same thing CI runs)
```
./mvnw verify
   ├─ test phase (Surefire, *Test)
   │    ├─ services/*Test             plain JUnit + Mockito, no Spring context (milliseconds)
   │    ├─ web/controller/*Test       @WebMvcTest: MVC + Thymeleaf + real WebSecurityConfig, services mocked
   │    └─ repository/*Test           @DataJpaTest / @JdbcTest + @Import(TestcontainersConfiguration)
   │                                    → THROWAWAY postgres:17, Flyway-migrated, each test rolled back
   └─ integration-test phase (Failsafe, *IT, all extend AbstractIntegrationTest)
        └─ @SpringBootTest + MockMvc + @Import(TestcontainersConfiguration)
             └─ @ServiceConnection → the DataSource points at the container (application.properties is ignored)
                  └─ whole app, nothing mocked: login, create, redirect, private links, admin access
   (spring-boot-docker-compose is skipped in tests by default, so your dev DB is never touched)
```

---

## 6. Pathway 1b: LOCAL full stack (app also in Docker)

This is a dress rehearsal of what EC2 runs, on your machine:

```bash
docker compose up --build -d spring-boot-url-shortener   # build the image + start postgres → app
docker logs -f shortener-app                              # watch Spring Boot start
# http://localhost:8080
docker compose --profile app down                         # stop both (data kept)
```

Here the app reaches the DB at `postgres:5432` over the compose network, not at `localhost`.
Stop any `./mvnw spring-boot:run` first, because both want port 8080.

---

## 7. Pathway 2: ONLINE (EC2 via GitHub Actions)

### ✅ One-time setup (🆕 before the first deploy)
- [ ] EC2 Ubuntu 24.04, `t3.small` (Maven builds on the instance and needs RAM). Attach an **Elastic IP**
- [ ] Security group: **22** (your IP plus GitHub Actions) and **8080** (anywhere). **Not** 5432
- [ ] Install Docker and Git on EC2 (`curl -fsSL https://get.docker.com | sudo sh`, `sudo usermod -aG docker ubuntu`, `sudo apt-get install -y git`)
- [ ] `git clone` the repo to `~/spring-boot-url-shortener`, then `cp .env.example .env` and set **a real `POSTGRES_PASSWORD`** and `APP_BASE_URL=http://<elastic-ip>:8080` *before* the first start (see the note in §3)
- [ ] GitHub repo secrets: `EC2_HOST`, `EC2_USERNAME` (`ubuntu`), `EC2_SSH_KEY` (the contents of the `.pem` file)

### The workflow design (`.github/workflows/deploy.yml`)

```
 ┌───────────────────────────────────────────────────────────────────┐
 │     GITHUB ACTIONS: "CI & EC2 Zero-Credit Deploy"                 │
 │     on: push to main  |  pull_request to main                     │
 └──────────────────────────────┬────────────────────────────────────┘
                                │
                                ▼
               ┌────────────────────────────────────┐
               │  Job 1: test-and-verify            │  runs for PRs AND pushes
               │  - ubuntu-latest runner            │
               │  - actions/checkout                │
               │  - setup-java 25 (temurin) +       │
               │    cache: maven (~/.m2 by pom hash)│
               │  - ./mvnw -B verify                │
               │    (Testcontainers → Docker on the │
               │     runner)                        │
               └──────────────┬─────────────────────┘
                              │
                  ┌───────────┴────────────┐
           (Pass) │                 (Fail) │
                  ▼                        ▼
   ┌────────────────────────────────┐   🛑 STOP: nothing deployed,
   │  Job 2: deploy-to-ec2          │      EC2 keeps the old version
   │  needs: test-and-verify        │
   │  if: github.event_name=='push' │──► PR? skipped (PRs never deploy)
   │  - appleboy/ssh-action → EC2   │
   │  - NO image registry: the image│
   │    is BUILT ON EC2             │
   └────────────────────────────────┘
```

### Chronological lifecycle of one deploy

```
==================================================================================================
  CHRONOLOGICAL PIPELINE LIFECYCLE  (build on EC2: multi-stage Dockerfile → fat jar + COMPOSE)
==================================================================================================

 [PHASE 0: ON YOUR MACHINE]
   │
   ├── ⓪ Change code → run ./mvnw verify locally → commit
   └── ① `git push origin main` (or merge a PR into main). GitHub reads .github/workflows/deploy.yml
   │
 [PHASE 1: GITHUB RUNNER, job test-and-verify]
   │
   ├── ② A fresh ubuntu-latest VM is provisioned; actions/checkout clones the repo (root = project)
   ├── ③ setup-java installs Temurin 25 and restores ~/.m2 from cache (🔁 cache hit if pom.xml unchanged)
   └── ④ ./mvnw -B verify → compile, tests against a Testcontainers postgres:17, package
   │
   ▼ (tests pass; on a PR the run ends here)
   │
 [PHASE 2: THE SSH HANDSHAKE, job deploy-to-ec2]
   │
   ├── ⑤ A second runner reads secrets EC2_HOST, EC2_USERNAME, EC2_SSH_KEY (masked in logs)
   └── ⑥ appleboy/ssh-action opens an SSH session to EC2 and runs the script with `set -e`
   │     (any failing command → job fails → red ✗ in GitHub)
   │
 [PHASE 3: SYNC THE CODE ON EC2]
   │
   ├── ⑦ 🆕 no ~/spring-boot-url-shortener/.git → `git clone https://github.com/<repo>.git`
   │     🔁 `git fetch origin main` (only the changed objects) + `git reset --hard origin/main`
   │        (the working tree now matches main exactly; .env is git-ignored → untouched)
   └── ⑧ Guard: no .env → "Missing …/.env" → exit 1 (refuses to run on the fallback password)
   │
 [PHASE 4: docker compose up --build -d spring-boot-url-shortener]
   │
   ├── ⑨ Compose loads compose.yaml + .env and activates the "app" profile (service named on the CLI)
   │
   ├── ⑩ BUILD the app image from the Dockerfile
   │     Stage 1 (maven:3.9-eclipse-temurin-25):
   │       🆕 pulls the base images, runs `go-offline` (downloads ALL dependencies), takes minutes
   │       🔁 pom.xml unchanged → "CACHED" for COPY pom.xml + go-offline (zero downloads)
   │          src/ changed → COPY src + `mvn package` re-run → new fat jar
   │     Stage 2 (eclipse-temurin:25-jre-alpine):
   │       copies the fat jar to /app/app.jar → image tagged url_shortner-spring-boot-url-shortener
   │       (the previous image loses its tag → becomes "dangling")
   │
   ├── ⑪ POSTGRES
   │     🆕 creates network + volume url_shortner_postgres_data, initdb from .env POSTGRES_*
   │     🔁 container already running with the same config → "Running", left alone. NO DATA LOSS
   │        (if its compose config changed, the container is recreated but the VOLUME is reused)
   │     • depends_on: service_healthy → waits for pg_isready
   │
   └── ⑫ APP CONTAINER (shortener-app)
         🔁 image ID changed → the old container is stopped and removed, a new one is created
            image identical (nothing in src/pom changed) → container left running as is
         • environment: SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/<db>, APP_BASE_URL …
         • publishes 8080:8080, restart: unless-stopped
   │
 [PHASE 5: INSIDE THE NEW CONTAINER]
   │
   ├── ⑬ ENTRYPOINT  java -jar /app/app.jar  → Spring Boot's launcher reads BOOT-INF/classes + lib
   ├── ⑭ Env vars override application.properties (localhost DB → postgres:5432, base URL → public IP)
   ├── ⑮ Flyway: 🆕 V1..V3 on the empty DB   🔁 only new V*.sql files
   └── ⑯ Tomcat on :8080 → http://<elastic-ip>:8080   health: /actuator/health → {"status":"UP"}
   │
 [PHASE 6: CLEAN UP]
   │
   └── ⑰ `docker image prune -f` deletes the dangling old app image. The build cache (with the
         dependency layer) is KEPT, so the next deploy is fast again
```

The app is unavailable for a few seconds between ⑫ (old container removed) and ⑯ (new Spring context ready).
That is acceptable for a single-instance setup.

### 🔁 Subsequent updates (EC2): what you do
| You want to… | Do this |
|---|---|
| Ship a code change | `git push origin main`. Nothing else |
| Re-deploy without a new commit (e.g. after fixing `.env`) | `gh run rerun <run-id> --failed`, or *Actions → Re-run jobs* |
| Change `APP_BASE_URL` or other `.env` values | Edit `~/spring-boot-url-shortener/.env` on EC2, then `docker compose up -d spring-boot-url-shortener` (the container is **recreated** with the new env). `docker restart` keeps the old env |
| See logs | `ssh ubuntu@<ip>` → `docker logs -f shortener-app` |
| Check status | `docker compose --profile app ps`, `curl -s localhost:8080/actuator/health` |

---

## 8. Where each value comes from

```
                      LOCAL (spring-boot:run)                 EC2 / local full stack (container)
 DB URL     application.properties (localhost:5432)  →     compose.yaml SPRING_DATASOURCE_URL
            overridden by the docker-compose service          = jdbc:postgresql://postgres:5432/${POSTGRES_DB}
            connection (container's host/port)
 DB user/pw .env POSTGRES_* → postgres container env →       .env POSTGRES_* → SPRING_DATASOURCE_USERNAME/PASSWORD
            read back by the service connection
 Base URL   application.properties app.base-url              .env APP_BASE_URL → app.base-url
            (http://localhost:8080)                           (http://<elastic-ip>:8080)
 Port       server.port=${PORT:8080}                          same, published as 8080:8080
```

Spring Boot's *relaxed binding* turns the env var `SPRING_DATASOURCE_URL` into `spring.datasource.url` and `APP_BASE_URL` into `app.base-url`.
Env vars take precedence over `application.properties`.
