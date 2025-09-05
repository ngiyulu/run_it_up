# syntax=docker/dockerfile:1.7

########################
# 1) Build stage (JDK 17)
########################
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

# Receive creds from build args
ARG GPR_USER
ARG GPR_TOKEN
# Expose them to Gradle during build
ENV GPR_USER=$GPR_USER
ENV GPR_TOKEN=$GPR_TOKEN

# Copy wrapper + config first for caching (supports Groovy or Kotlin DSL)
COPY gradlew ./gradlew
COPY gradle ./gradle
COPY build.gradle* settings.gradle* gradle.properties* ./

# Fix Windows line endings and ensure wrapper is executable
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# Warm Gradle and verify environment (cached between builds)
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon --version

# Copy sources
COPY src ./src
# If you need extra resources at build time, copy them here (example):
# COPY firebase/firebase-dev.json src/main/resources/firebase-dev.json

# Build fat jar (skip tests to avoid build-time secrets), show more logs for debugging
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon clean bootJar -x test --stacktrace --info

# Normalize jar name for the runtime stage
RUN sh -c 'JAR=$(ls build/libs/*.jar | head -n1) && cp "$JAR" /workspace/app.jar'

########################
# 2) Runtime stage (JRE 17)
########################
FROM eclipse-temurin:17-jre
WORKDIR /app

# Run as non-root
RUN useradd -ms /bin/bash spring
USER spring

COPY --from=build /workspace/app.jar ./app.jar


EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
