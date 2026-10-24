package de.musterversicherung.quote.client;

public class UnknownVehicleException extends RuntimeException {

    private final long vehicleId;

    public UnknownVehicleException(long vehicleId) {
        super("Unknown vehicle: " + vehicleId);
        this.vehicleId = vehicleId;
    }

    public long getVehicleId() {
        return vehicleId;
    }
}
