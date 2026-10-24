package de.musterversicherung.vehicle.api;

import de.musterversicherung.vehicle.domain.Vehicle;
import de.musterversicherung.vehicle.domain.VehicleNotFoundException;
import de.musterversicherung.vehicle.service.VehicleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testet nur die Web-Schicht: HTTP rein, JSON raus. Der Service ist gemockt.
 */
@WebMvcTest(VehicleController.class)
class VehicleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VehicleService vehicleService;

    @Test
    void getAllReturnsVehiclesAsJson() throws Exception {
        when(vehicleService.findAll()).thenReturn(List.of(
                new Vehicle(1, "Autowerk Nord", "Kompakt K1", 1),
                new Vehicle(6, "Alpenwagen", "Bergsport GT", 5)));

        mockMvc.perform(get("/api/vehicles"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].manufacturer").value("Autowerk Nord"))
                .andExpect(jsonPath("$[0].model").value("Kompakt K1"))
                .andExpect(jsonPath("$[1].riskClass").value(5));
    }

    @Test
    void getByIdReturnsVehicle() throws Exception {
        when(vehicleService.findById(1)).thenReturn(new Vehicle(1, "Autowerk Nord", "Kompakt K1", 1));

        mockMvc.perform(get("/api/vehicles/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.model").value("Kompakt K1"))
                .andExpect(jsonPath("$.riskClass").value(1));
    }

    @Test
    void getByIdReturns404ProblemDetailWhenUnknown() throws Exception {
        when(vehicleService.findById(99)).thenThrow(new VehicleNotFoundException(99));

        mockMvc.perform(get("/api/vehicles/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Fahrzeug nicht gefunden"))
                .andExpect(jsonPath("$.detail").value("Es gibt kein Fahrzeug mit der ID 99."));
    }

    @Test
    void getByIdReturns400WhenIdIsNotANumber() throws Exception {
        mockMvc.perform(get("/api/vehicles/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));
    }
}
