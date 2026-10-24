# syntax=docker/dockerfile:1
# Bauen (aus dem Repo-Hauptverzeichnis):
#   docker build -f deploy/docker/vehicle-service.Dockerfile -t kfz-tarifrechner/vehicle-service .

# ---------- Stufe 1: bauen (JDK + Maven, groß) ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Erst nur die Build-Dateien kopieren und Abhängigkeiten laden.
# Docker speichert jeden Schritt als Layer. Ändert sich nur der Code, wird dieser Schritt aus dem Cache genommen.
COPY vehicle-service/.mvn/ .mvn/
COPY vehicle-service/mvnw vehicle-service/pom.xml ./
RUN --mount=type=cache,target=/root/.m2 ./mvnw -q -B dependency:go-offline

COPY vehicle-service/src/ src/
# Tests laufen in der CI (mvn verify), nicht beim Image-Bau.
RUN --mount=type=cache,target=/root/.m2 ./mvnw -q -B package -DskipTests \
    && cp target/vehicle-service-*.jar app.jar

# ---------- Stufe 2: ausführen (nur JRE, klein) ----------
FROM eclipse-temurin:21-jre

# Eigener Nutzer ohne Root-Rechte. Feste Nummer (UID), damit Kubernetes "runAsNonRoot" prüfen kann.
# Gruppe 0 (root-Gruppe): OpenShift startet Container mit zufälliger UID, aber immer in Gruppe 0.
RUN useradd --system --uid 10001 --gid 0 --no-create-home app

WORKDIR /app
COPY --from=build --chown=10001:0 /workspace/app.jar app.jar

USER 10001
EXPOSE 8081

# Docker prüft regelmäßig, ob der Dienst wirklich bereit ist (nicht nur, ob der Prozess läuft).
HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=3 \
    CMD curl -fsS http://localhost:8081/actuator/health/readiness || exit 1

# MaxRAMPercentage: Java richtet den Speicher nach dem Container-Limit aus, nicht nach dem ganzen Rechner.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
