_Last Updated: 2026-10-26_

# TinyURL
## A project to learn System Design

Planned Technology Stack 
- Spring Boot 3.5.10
- bootstrap@5.3.8 -> later React
- DB : Docker/PostgreSQL with Flyway Migrations
- Redis cache
- 

## Features
- Shorten long URLs with a randomly generated short key (optional check that the original URL exists)
- Redirect `/s/{shortKey}` to the original URL, with a click counter
- Private short URLs (visible/accessible only to their creator) and a custom expiry (1–365 days, default 30)
- Paginated public URL list, "My URLs" page and admin dashboard with Prev/Next navigation
- User registration and form login with Spring Security (BCrypt passwords)
- Role-based, method-level security (`@PreAuthorize`) with a role hierarchy `ROLE_ADMIN > ROLE_USER`
- Users can delete their own short URLs
- Custom 404 / 500 error pages

## Architecture
Single Spring Boot MVC app rendered server-side with Thymeleaf + Bootstrap.

| Layer | Package / Location | Notes |
|---|---|---|
| Web | `web.controller` (`HomeController`, `UserController`, `AdminController`), `GlobalExceptionHandler` | Thymeleaf views in `src/main/resources/templates` |
| Security | `config.WebSecurityConfig`, `services.SecurityUserDetailsService` | Form login, role hierarchy, method security |
| Service | `services` (`ShortUrlService`, `UserService`, `UrlExistenceValidator`, `EntityMapper`) | |
| Persistence | `repository` | `ShortUrlRepository` uses Spring Data JPA; `UserRepository` uses Spring `JdbcClient` |
| Database | PostgreSQL 17 | Schema & seed data via Flyway (`src/main/resources/db/migration`) |

## Prerequisites
- Java 25
- Docker (PostgreSQL container, and optionally running the app as an image)
- Maven (or the bundled `mvnw` wrapper)

## Installation & Running Locally
```bash
./mvnw spring-boot:run
```
`spring-boot-docker-compose` starts PostgreSQL from `compose.yaml` automatically. The app runs at http://localhost:8080.

Sample accounts seeded by Flyway:

| Email | Password | Role |
|---|---|---|
| `admin@gmail.com` | `admin` | ADMIN |
| `siva@gmail.com` | `secret` | USER |

### Configuration (`application.properties`)
| Property | Default | Purpose |
|---|---|---|
| `app.base-url` | `http://localhost:8080` | Prefix for generated short links |
| `app.default-expiry-days` | `30` | Expiry when none is given |
| `app.validate-original-url` | `true` | Check the original URL is reachable |
| `app.page-size` | `10` | Page size for paginated lists |

## Usage
| Endpoint | Access | Description |
|---|---|---|
| `GET /?page=1` | Public | Home: create form + public short URLs |
| `POST /short-urls` | Public | Create a short URL (`originalUrl`, `expirationInDays`, `isPrivate`) |
| `GET /s/{shortKey}` | Public (private links: owner only) | Redirect to the original URL |
| `GET/POST /register` | Public | Register a new user |
| `GET /login`, `POST /logout` | Public | Login / logout |
| `GET /my-urls?page=1` | Authenticated | Current user's short URLs |
| `POST /delete-urls` | USER | Delete selected URLs (`ids`) |
| `GET /admin/dashboard?page=1` | ADMIN | All short URLs |

## Deployment (Docker)
Build an OCI image with Cloud Native Buildpacks (image name `kunalkrishna/spring-boot-url-shortener`), then run the app and PostgreSQL together:
```bash
./mvnw spring-boot:build-image
docker compose --profile app up -d
```
A single `compose.yaml` at the project root is used for both cases: without a profile (and via `spring-boot:run`) only PostgreSQL starts; the `app` profile adds the app container. Its datasource is set via the `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD` environment variables in `compose.yaml`.
