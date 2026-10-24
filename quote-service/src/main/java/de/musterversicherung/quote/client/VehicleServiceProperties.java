package de.musterversicherung.quote.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Objects;

/**
 * Verbindungsdaten zum vehicle-service (Präfix "vehicle-service" in application.yml).
 */
@ConfigurationProperties(prefix = "vehicle-service")
public record VehicleServiceProperties(String baseUrl, Duration connectTimeout, Duration readTimeout) {

    public VehicleServiceProperties {
        Objects.requireNonNull(baseUrl, "vehicle-service.base-url is missing");
        Objects.requireNonNull(connectTimeout, "vehicle-service.connect-timeout is missing");
        Objects.requireNonNull(readTimeout, "vehicle-service.read-timeout is missing");
    }
}
