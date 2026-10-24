package de.musterversicherung.vehicle.repository;

import de.musterversicherung.vehicle.domain.Vehicle;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Feste, unveränderliche Liste erfundener Fahrzeuge. Bewusst keine Datenbank (siehe README).
 */
@Repository
public class InMemoryVehicleRepository implements VehicleRepository {

    private static final List<Vehicle> VEHICLES = List.of(
            new Vehicle(1, "Autowerk Nord", "Kompakt K1", 1),
            new Vehicle(2, "Autowerk Nord", "Familia Kombi", 2),
            new Vehicle(3, "Rheinmobil", "Stadtflitzer", 1),
            new Vehicle(4, "Rheinmobil", "Touring 500", 3),
            new Vehicle(5, "Alpenwagen", "Gipfel SUV", 4),
            new Vehicle(6, "Alpenwagen", "Bergsport GT", 5),
            new Vehicle(7, "Küstenwerk", "Elektra E", 2),
            new Vehicle(8, "Küstenwerk", "Lastesel Van", 3)
    );

    @Override
    public List<Vehicle> findAll() {
        return VEHICLES;
    }

    @Override
    public Optional<Vehicle> findById(long id) {
        return VEHICLES.stream()
                .filter(vehicle -> vehicle.id() == id)
                .findFirst();
    }
}
