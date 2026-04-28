# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
# Copy the source code
COPY src/ ./src/
# Build the application skipping tests
RUN mvn clean package -DskipTests

# Stage 2: Create the runtime image
FROM eclipse-temurin:17-jre-alpine AS runtime

# Add metadata
LABEL maintainer="admin@smartagri.com"
LABEL version="1.0.0"

# Create a non-root user and group for security
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

# Copy the built jar from the build stage and assign ownership to the non-root user
COPY --from=build --chown=spring:spring target/*.jar /app/app.jar

# Run as the non-root user
USER spring

# Expose the application port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
