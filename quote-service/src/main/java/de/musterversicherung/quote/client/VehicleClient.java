package de.musterversicherung.quote.client;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Ruft den vehicle-service per HTTP auf und übersetzt seine Fehler in unsere eigenen Exceptions.
 */
@Component
public class VehicleClient {

    private final RestClient restClient;

    public VehicleClient(RestClient vehicleRestClient) {
        this.restClient = vehicleRestClient;
    }

    public VehicleInfo getVehicle(long vehicleId) {
        try {
            return restClient.get()
                    .uri("/api/vehicles/{id}", vehicleId)
                    .retrieve()
                    // 404 ist kein technischer Fehler, sondern "dieses Fahrzeug gibt es nicht".
                    .onStatus(status -> status.isSameCodeAs(HttpStatus.NOT_FOUND), (request, response) -> {
                        throw new UnknownVehicleException(vehicleId);
                    })
                    .body(VehicleInfo.class);
        } catch (RestClientException exception) {
            // Alles andere: keine Verbindung, Timeout (ResourceAccessException),
            // 5xx-Antwort oder kaputtes JSON. Für den Nutzer heißt das immer: "gerade nicht erreichbar".
            throw new VehicleServiceUnavailableException(
                    "vehicle-service call failed for vehicle " + vehicleId, exception);
        }
    }
}
