package de.musterversicherung.quote.client;

/**
 * Antwort des vehicle-service, aus Sicht des quote-service.
 * Bewusst eine eigene Kopie und keine geteilte Bibliothek: Jeder Service besitzt seine Klassen selbst.
 * Unbekannte JSON-Felder ignoriert Spring Boot standardmäßig – neue Felder im vehicle-service brechen hier nichts.
 */
public record VehicleInfo(long id, String manufacturer, String model, int riskClass) {

    public String displayName() {
        return manufacturer + " " + model;
    }
}
