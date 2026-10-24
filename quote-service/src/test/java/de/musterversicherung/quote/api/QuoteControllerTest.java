package de.musterversicherung.quote.api;

import de.musterversicherung.quote.client.UnknownVehicleException;
import de.musterversicherung.quote.client.VehicleServiceUnavailableException;
import de.musterversicherung.quote.pricing.Coverage;
import de.musterversicherung.quote.pricing.PriceResult;
import de.musterversicherung.quote.pricing.PriceStep;
import de.musterversicherung.quote.service.Quote;
import de.musterversicherung.quote.service.QuoteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QuoteController.class)
class QuoteControllerTest {

    private static final String VALID_BODY = """
            {"vehicleId": 4, "driverAge": 40, "accidentFreeYears": 5, "coverage": "TEILKASKO"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QuoteService quoteService;

    @Test
    void returnsQuote() throws Exception {
        PriceResult price = new PriceResult(new BigDecimal("456.88"), new BigDecimal("38.07"), List.of(
                new PriceStep(1, "Grundpreis für Risikoklasse 3", new BigDecimal("430.00"), new BigDecimal("430.00"))));
        when(quoteService.createQuote(any())).thenReturn(new Quote("Rheinmobil Touring 500", Coverage.TEILKASKO, price));

        postQuote(VALID_BODY)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.vehicleName").value("Rheinmobil Touring 500"))
                .andExpect(jsonPath("$.coverage").value("TEILKASKO"))
                .andExpect(jsonPath("$.yearlyPrice").value(456.88))
                .andExpect(jsonPath("$.monthlyPrice").value(38.07))
                .andExpect(jsonPath("$.breakdown", hasSize(1)))
                .andExpect(jsonPath("$.breakdown[0].step").value(1))
                .andExpect(jsonPath("$.breakdown[0].description").value("Grundpreis für Risikoklasse 3"))
                .andExpect(jsonPath("$.breakdown[0].factorOrAmount").value(430.00))
                .andExpect(jsonPath("$.breakdown[0].subtotal").value(430.00));
    }

    @Test
    void returns400WithAllFieldErrorsSorted() throws Exception {
        postQuote("{}")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Ungültige Eingabe"))
                .andExpect(jsonPath("$.errors", hasSize(4)))
                .andExpect(jsonPath("$.errors[0].field").value("accidentFreeYears"))
                .andExpect(jsonPath("$.errors[1].field").value("coverage"))
                .andExpect(jsonPath("$.errors[2].field").value("driverAge"))
                .andExpect(jsonPath("$.errors[2].message").value("Bitte das Alter angeben."))
                .andExpect(jsonPath("$.errors[3].field").value("vehicleId"));
        verify(quoteService, never()).createQuote(any());
    }

    @Test
    void returns400ForCrossFieldRule() throws Exception {
        postQuote("""
                {"vehicleId": 1, "driverAge": 30, "accidentFreeYears": 13, "coverage": "HAFTPFLICHT"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0].field").value("accidentFreeYears"))
                .andExpect(jsonPath("$.errors[0].message")
                        .value("Die unfallfreien Jahre dürfen nicht größer sein als Alter minus 18."));
    }

    @Test
    void returns400ForUnknownCoverage() throws Exception {
        postQuote("""
                {"vehicleId": 1, "driverAge": 30, "accidentFreeYears": 1, "coverage": "SUPERKASKO"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Ungültige Eingabe"));
    }

    @Test
    void returns400ForBrokenJson() throws Exception {
        postQuote("{ kaputt").andExpect(status().isBadRequest());
    }

    @Test
    void returns400WithFieldErrorForUnknownVehicle() throws Exception {
        when(quoteService.createQuote(any())).thenThrow(new UnknownVehicleException(4));

        postQuote(VALID_BODY)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[0].field").value("vehicleId"))
                .andExpect(jsonPath("$.errors[0].message").value("Das Fahrzeug mit der ID 4 gibt es nicht."));
    }

    @Test
    void returns503WhenVehicleServiceIsUnavailable() throws Exception {
        when(quoteService.createQuote(any()))
                .thenThrow(new VehicleServiceUnavailableException("down", new RuntimeException()));

        postQuote(VALID_BODY)
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.detail").value(
                        "Der Fahrzeugdienst ist gerade nicht erreichbar. Bitte später erneut versuchen."));
    }

    @Test
    void returns500WithoutInternalDetails() throws Exception {
        when(quoteService.createQuote(any())).thenThrow(new IllegalStateException("geheime interne Info"));

        postQuote(VALID_BODY)
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value(
                        "Ein unerwarteter Fehler ist aufgetreten. Bitte später erneut versuchen."));
    }

    private ResultActions postQuote(String body) throws Exception {
        return mockMvc.perform(post("/api/quotes").contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
