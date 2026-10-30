# In a Spring Boot application,
#   a Dockerfile is used to package your Java code into a standalone container image, while
#   a compose.yml file is used to run and connect that image with other infrastructure services (like databases or caches)
# They are not competing tools; rather, they work together to containerize and orchestrate your application.
# (Note: Dockerfile is typically a extensionless plain-text file, while compose.yml or docker-compose.yml uses the YAML format)


# Place this file in your project root. It will execute directly inside your EC2 CPU instead of a GitHub runner:

# --- Stage 1: Build Context ---
# Uses a JDK 25 Maven base image matching your runner context
FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /build

# Cache your Maven project dependencies (this layer is reused until pom.xml changes)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy local source structures and run clean packaging.
# Uses the image's own `mvn`: mvnw/.mvn are not copied into this stage.
COPY src ./src
RUN mvn -B clean package -DskipTests

# --- Stage 2: Minimal Execution Runtime ---
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# spring-boot-maven-plugin builds an executable fat jar: app classes + exactly the
# runtime dependencies (BOOT-INF/lib) + the main class in its manifest
COPY --from=builder /build/target/*.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
