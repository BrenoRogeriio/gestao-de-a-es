# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app
RUN groupadd --system spring && useradd --system --gid spring spring
COPY --from=build --chown=spring:spring /workspace/target/*.jar app.jar
USER spring:spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]