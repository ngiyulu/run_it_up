# syntax=docker/dockerfile:1.7
FROM eclipse-temurin:21-jre-alpine

# Non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

WORKDIR /app
# Copy your built JAR into the image
COPY build/libs/RunItUp-0.0.1-SNAPSHOT.jar /app/app.jar

# Tweak memory for containers; set your active profile if needed
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080
# Optional (requires spring-boot-starter-actuator):
# HEALTHCHECK --interval=30s --timeout=3s --retries=3 CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java","-jar","/app/app.jar"]
