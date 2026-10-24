package de.musterversicherung.vehicle.domain;

/**
 * Ein Fahrzeug aus unserem (erfundenen) Katalog.
 *
 * @param riskClass 1 = geringes Risiko, 5 = hohes Risiko
 */
public record Vehicle(long id, String manufacturer, String model, int riskClass) {

    public Vehicle {
        if (riskClass < 1 || riskClass > 5) {
            throw new IllegalArgumentException("riskClass must be between 1 and 5, was " + riskClass);
        }
    }
}
