# Multi-stage Dockerfile for Spring Boot 3.2.5 (Java 17)
# Stage 1: Build JAR using Maven
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Compile and package application
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Minimal, secure JRE 17 runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy the generated JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose Spring Boot port
EXPOSE 8080

# Set Spring profile to prod for cloud deployment
ENV SPRING_PROFILES_ACTIVE=prod

# Run Spring Boot
ENTRYPOINT ["java", "-jar", "app.jar"]
