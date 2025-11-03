# syntax=docker/dockerfile:1.4

# ======================
# Build Stage
# ======================
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app

COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -q -DskipTests dependency:go-offline

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -q -DskipTests package

# ======================
# Runtime Stage
# ======================
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the repackaged fat JAR explicitly
COPY --from=builder /app/target/app.jar /app/app.jar

RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser \
    && chown -R appuser:appgroup /app

USER appuser

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"
EXPOSE 8080

# Entrypoint/command intentionally left to docker-compose service commands
