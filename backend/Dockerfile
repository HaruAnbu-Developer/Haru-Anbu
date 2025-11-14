# Use OpenJDK 17 as base image
FROM openjdk:17-jdk-slim

# Set working directory
WORKDIR /app

# Copy gradle wrapper and build files
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Copy source code
COPY src src

# Make gradlew executable
RUN chmod +x ./gradlew

# Build the application
RUN ./gradlew bootJar --no-daemon

# Expose port
EXPOSE 8080

# Set environment variable for Spring Boot
ENV SPRING_PROFILES_ACTIVE=prod

# Run the application
CMD ["java", "-jar", "build/libs/backend-0.0.1-SNAPSHOT.jar"]