package de.musterversicherung.vehicle.service;

import de.musterversicherung.vehicle.domain.Vehicle;
import de.musterversicherung.vehicle.domain.VehicleNotFoundException;
import de.musterversicherung.vehicle.repository.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private VehicleService vehicleService;

    @Test
    void findByIdReturnsVehicleFromRepository() {
        Vehicle vehicle = new Vehicle(1, "Autowerk Nord", "Kompakt K1", 1);
        when(vehicleRepository.findById(1)).thenReturn(Optional.of(vehicle));

        assertThat(vehicleService.findById(1)).isEqualTo(vehicle);
    }

    @Test
    void findByIdThrowsWhenVehicleIsUnknown() {
        when(vehicleRepository.findById(42)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vehicleService.findById(42))
                .isInstanceOf(VehicleNotFoundException.class)
                .hasMessageContaining("42");
    }
}
