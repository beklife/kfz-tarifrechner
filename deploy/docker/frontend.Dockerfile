# syntax=docker/dockerfile:1
# Bauen (aus dem Repo-Hauptverzeichnis):
#   docker build -f deploy/docker/frontend.Dockerfile -t kfz-tarifrechner/frontend .

# ---------- Stufe 1: Angular bauen (Node, groß) ----------
FROM node:24-alpine AS build
WORKDIR /app

# Erst nur package*.json: "npm ci" läuft dann nur neu, wenn sich Abhängigkeiten ändern.
COPY frontend/package.json frontend/package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci --no-audit --no-fund

COPY frontend/ ./
RUN npm run build

# ---------- Stufe 2: ausliefern mit nginx (klein) ----------
# "unprivileged": nginx läuft ohne Root und auf Port 8080 (nicht 80).
# Grund: OpenShift startet Container mit einer zufälligen Nutzer-ID ohne Root-Rechte.
# Das normale nginx-Image will als Root starten und Port 80 öffnen – das klappt dort nicht.
FROM nginxinc/nginx-unprivileged:1.29-alpine

# Dateien in /etc/nginx/templates werden beim Start mit envsubst verarbeitet:
# ${VEHICLE_SERVICE_URL} und ${QUOTE_SERVICE_URL} werden durch die Umgebungsvariablen ersetzt.
COPY deploy/docker/nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html

ENV VEHICLE_SERVICE_URL=http://vehicle-service:8081 \
    QUOTE_SERVICE_URL=http://quote-service:8082

EXPOSE 8080

# 127.0.0.1 statt localhost: In Alpine zeigt "localhost" zuerst auf IPv6 (::1), nginx hört aber nur auf IPv4.
HEALTHCHECK --interval=10s --timeout=3s --start-period=5s --retries=3 \
    CMD wget -q -O /dev/null http://127.0.0.1:8080/healthz || exit 1
