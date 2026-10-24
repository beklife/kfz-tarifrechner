package de.musterversicherung.quote.client;

/**
 * vehicle-service ist nicht erreichbar, zu langsam oder antwortet mit einem Fehler.
 */
public class VehicleServiceUnavailableException extends RuntimeException {

    public VehicleServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
