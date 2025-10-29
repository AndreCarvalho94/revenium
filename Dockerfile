# syntax=docker/dockerfile:1.4

# Etapa de build
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Baixa dependências no cache
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -q -DskipTests dependency:go-offline

# Copia código e empacota
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -q -DskipTests package

# Etapa de runtime
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copia o jar final do Spring Boot (repackage)
COPY --from=build /app/target/*.jar /app/app.jar

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"
EXPOSE 8080

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]

