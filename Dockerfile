# Use a base image with Java installed
FROM openjdk:17-jdk-slim

# Set the working directory inside the container
WORKDIR /app

# Copy the Gradle wrapper and build files
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Copy the source code
COPY src src

# Build the Spring Boot application
RUN ./gradlew bootJar

# Expose the port your Spring Boot application runs on (e.g., 8080)
EXPOSE 8080

# Specify the command to run the application when the container starts
ENTRYPOINT ["java", "-jar", "build/libs/*.jar"]