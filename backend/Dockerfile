FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn --batch-mode --no-transfer-progress dependency:go-offline
COPY src src
RUN mvn --batch-mode --no-transfer-progress clean package

FROM registry.access.redhat.com/ubi9/openjdk-17-runtime:latest
WORKDIR /deployments
COPY --from=build /workspace/target/security-api-0.0.1-SNAPSHOT.jar application.jar
EXPOSE 8080
ENV JAVA_OPTS_APPEND="-XX:MaxRAMPercentage=75.0"
CMD ["java", "-jar", "/deployments/application.jar"]

