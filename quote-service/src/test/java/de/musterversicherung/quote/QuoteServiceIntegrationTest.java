package de.musterversicherung.quote;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ganzer quote-service gegen einen echten (kleinen, falschen) vehicle-service über echtes HTTP.
 * Der Fake-Server kommt aus dem JDK (com.sun.net.httpserver) – keine Extra-Bibliothek nötig.
 * Anders als MockRestServiceServer testet das auch die echten Timeout-Einstellungen.
 */
@SpringBootTest
@AutoConfigureMockMvc
class QuoteServiceIntegrationTest {

    private static final Duration TEST_READ_TIMEOUT = Duration.ofMillis(300);

    private static HttpServer fakeVehicleService;

    @Autowired
    private MockMvc mockMvc;

    @BeforeAll
    static void startFakeVehicleService() throws IOException {
        fakeVehicleService = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        // Jede Anfrage in eigenem Thread, damit die langsame Antwort die anderen nicht blockiert.
        fakeVehicleService.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        fakeVehicleService.createContext("/api/vehicles/4", exchange -> respond(exchange, 200, """
                {"id":4,"manufacturer":"Rheinmobil","model":"Touring 500","riskClass":3}
                """));
        fakeVehicleService.createContext("/api/vehicles/99", exchange -> respond(exchange, 404, """
                {"status":404,"title":"Fahrzeug nicht gefunden"}
                """));
        fakeVehicleService.createContext("/api/vehicles/7", exchange -> {
            sleep(TEST_READ_TIMEOUT.multipliedBy(3));
            respond(exchange, 200, "{}");
        });
        fakeVehicleService.start();
    }

    @AfterAll
    static void stopFakeVehicleService() {
        fakeVehicleService.stop(0);
    }

    @DynamicPropertySource
    static void pointToFakeVehicleService(DynamicPropertyRegistry registry) {
        registry.add("vehicle-service.base-url",
                () -> "http://localhost:" + fakeVehicleService.getAddress().getPort());
        registry.add("vehicle-service.read-timeout", TEST_READ_TIMEOUT::toString);
    }

    @Test
    void calculatesQuoteWithDataFromVehicleService() throws Exception {
        // Risikoklasse 3 kommt vom vehicle-service: 430 × 1.00 × 1.25 × 0.85 = 456.875 -> 456.88
        postQuote(4)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleName").value("Rheinmobil Touring 500"))
                .andExpect(jsonPath("$.yearlyPrice").value(456.88))
                .andExpect(jsonPath("$.monthlyPrice").value(38.07))
                .andExpect(jsonPath("$.breakdown", hasSize(4)));
    }

    @Test
    void returns400WhenVehicleServiceSays404() throws Exception {
        postQuote(99)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("vehicleId"));
    }

    @Test
    void returns503QuicklyWhenVehicleServiceIsTooSlow() throws Exception {
        long start = System.nanoTime();

        postQuote(7).andExpect(status().isServiceUnavailable());

        Duration elapsed = Duration.ofNanos(System.nanoTime() - start);
        // Wir haben nicht auf die (langsame) Antwort gewartet, sondern nach dem Timeout abgebrochen.
        assertThat(elapsed).isLessThan(TEST_READ_TIMEOUT.multipliedBy(3));
    }

    @Test
    void staysReadyEvenIfVehicleServiceIsSlow() throws Exception {
        // Bewusst: Readiness hängt NICHT vom vehicle-service ab (sonst Kettenreaktion bei Ausfall).
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    private ResultActions postQuote(long vehicleId) throws Exception {
        return mockMvc.perform(post("/api/quotes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"vehicleId": %d, "driverAge": 40, "accidentFreeYears": 5, "coverage": "TEILKASKO"}
                        """.formatted(vehicleId)));
    }

    private static void respond(HttpExchange exchange, int status, String json)
            throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
