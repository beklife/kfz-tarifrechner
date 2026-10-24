package de.musterversicherung.vehicle.domain;

public class VehicleNotFoundException extends RuntimeException {

    private final long vehicleId;

    public VehicleNotFoundException(long vehicleId) {
        super("Vehicle not found: " + vehicleId);
        this.vehicleId = vehicleId;
    }

    public long getVehicleId() {
        return vehicleId;
    }
}
