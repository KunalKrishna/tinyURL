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

# Cache your Maven project dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy local source structures and run clean packaging
COPY src ./src
RUN ./mvnw clean package -DskipTests

# --- Stage 2: Minimal Execution Runtime ---
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Extract structural layers to maintain efficient cache boundaries on EC2
COPY --from=builder /build/target/classes /app/classes
COPY --from=builder /root/.m2/repository /app/lib

EXPOSE 8080

# Execute using Classpath flag pointing directly to your Spring Boot main class
# (Remember to change com.example.Main to your real Application main class path)
ENTRYPOINT ["java", "-cp", "/app/classes:/app/lib/*", "com.abitmanipulator.UrlShortnerApp"]
