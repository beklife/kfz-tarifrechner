package de.musterversicherung.vehicle.repository;

import de.musterversicherung.vehicle.domain.Vehicle;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryVehicleRepositoryTest {

    private final InMemoryVehicleRepository repository = new InMemoryVehicleRepository();

    @Test
    void findAllReturnsEightVehiclesWithUniqueIds() {
        List<Vehicle> vehicles = repository.findAll();

        assertThat(vehicles).hasSize(8);
        assertThat(vehicles).extracting(Vehicle::id).doesNotHaveDuplicates();
    }

    @Test
    void findAllCoversEveryRiskClass() {
        assertThat(repository.findAll())
                .extracting(Vehicle::riskClass)
                .contains(1, 2, 3, 4, 5);
    }

    @Test
    void findByIdReturnsVehicleWhenIdExists() {
        assertThat(repository.findById(1))
                .hasValueSatisfying(vehicle -> assertThat(vehicle.model()).isEqualTo("Kompakt K1"));
    }

    @Test
    void findByIdReturnsEmptyWhenIdIsUnknown() {
        assertThat(repository.findById(999)).isEmpty();
    }
}
