package de.musterversicherung.vehicle.repository;

import de.musterversicherung.vehicle.domain.Vehicle;

import java.util.List;
import java.util.Optional;

/**
 * Zugriff auf Fahrzeugdaten. Heute in-memory, später z. B. eine Datenbank.
 * Der Service kennt nur dieses Interface, nicht die Implementierung.
 */
public interface VehicleRepository {

    List<Vehicle> findAll();

    Optional<Vehicle> findById(long id);
}
