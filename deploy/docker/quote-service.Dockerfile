# syntax=docker/dockerfile:1
# Bauen (aus dem Repo-Hauptverzeichnis):
#   docker build -f deploy/docker/quote-service.Dockerfile -t kfz-tarifrechner/quote-service .
# Gleicher Aufbau wie vehicle-service.Dockerfile (Erklärungen siehe dort).

# ---------- Stufe 1: bauen ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY quote-service/.mvn/ .mvn/
COPY quote-service/mvnw quote-service/pom.xml ./
RUN --mount=type=cache,target=/root/.m2 ./mvnw -q -B dependency:go-offline

COPY quote-service/src/ src/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -q -B package -DskipTests \
    && cp target/quote-service-*.jar app.jar

# ---------- Stufe 2: ausführen ----------
FROM eclipse-temurin:21-jre

RUN useradd --system --uid 10001 --gid 0 --no-create-home app

WORKDIR /app
COPY --from=build --chown=10001:0 /workspace/app.jar app.jar

USER 10001
EXPOSE 8082

# Wo der vehicle-service ist, kommt von außen (docker compose / OpenShift ConfigMap).
ENV VEHICLE_SERVICE_URL=http://vehicle-service:8081

HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=3 \
    CMD curl -fsS http://localhost:8082/actuator/health/readiness || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
