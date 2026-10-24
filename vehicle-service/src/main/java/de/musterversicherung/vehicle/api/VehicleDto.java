package de.musterversicherung.vehicle.api;

import de.musterversicherung.vehicle.domain.Vehicle;

/**
 * Das, was die API nach außen zeigt. Bewusst getrennt vom Domain-Objekt {@link Vehicle}.
 */
public record VehicleDto(long id, String manufacturer, String model, int riskClass) {

    public static VehicleDto from(Vehicle vehicle) {
        return new VehicleDto(vehicle.id(), vehicle.manufacturer(), vehicle.model(), vehicle.riskClass());
    }
}
