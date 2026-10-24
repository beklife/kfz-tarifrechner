package de.musterversicherung.vehicle.api;

import de.musterversicherung.vehicle.service.VehicleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @GetMapping
    public List<VehicleDto> findAll() {
        return vehicleService.findAll().stream()
                .map(VehicleDto::from)
                .toList();
    }

    @GetMapping("/{id}")
    public VehicleDto findById(@PathVariable long id) {
        return VehicleDto.from(vehicleService.findById(id));
    }
}
