package de.musterversicherung.quote.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * MockRestServiceServer ersetzt die HTTP-Schicht des RestClient. Es gibt kein echtes Netzwerk:
 * Wir legen fest, welche Anfrage kommen muss und was "der Server" antwortet.
 */
class VehicleClientTest {

    private static final String BASE_URL = "http://vehicle-service.test";

    private MockRestServiceServer server;
    private VehicleClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new VehicleClient(builder.build());
    }

    @AfterEach
    void verifyAllExpectedRequestsWereMade() {
        server.verify();
    }

    @Test
    void returnsVehicleOn200() {
        server.expect(requestTo(BASE_URL + "/api/vehicles/3"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"id":3,"manufacturer":"Rheinmobil","model":"Stadtflitzer","riskClass":1}
                        """, MediaType.APPLICATION_JSON));

        VehicleInfo vehicle = client.getVehicle(3);

        assertThat(vehicle).isEqualTo(new VehicleInfo(3, "Rheinmobil", "Stadtflitzer", 1));
        assertThat(vehicle.displayName()).isEqualTo("Rheinmobil Stadtflitzer");
    }

    @Test
    void ignoresUnknownJsonFields() {
        // Robust gegen neue Felder im vehicle-service ("tolerant reader").
        server.expect(requestTo(BASE_URL + "/api/vehicles/3"))
                .andRespond(withSuccess("""
                        {"id":3,"manufacturer":"Rheinmobil","model":"Stadtflitzer","riskClass":1,"color":"rot"}
                        """, MediaType.APPLICATION_JSON));

        assertThat(client.getVehicle(3).riskClass()).isEqualTo(1);
    }

    @Test
    void throwsUnknownVehicleOn404() {
        server.expect(requestTo(BASE_URL + "/api/vehicles/99")).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.getVehicle(99))
                .isInstanceOf(UnknownVehicleException.class)
                .extracting("vehicleId").isEqualTo(99L);
    }

    @Test
    void throwsUnavailableOn500() {
        server.expect(requestTo(BASE_URL + "/api/vehicles/1")).andRespond(withServerError());

        assertThatThrownBy(() -> client.getVehicle(1)).isInstanceOf(VehicleServiceUnavailableException.class);
    }

    @Test
    void throwsUnavailableOnTimeout() {
        // So sieht ein Read-Timeout für den RestClient aus.
        server.expect(requestTo(BASE_URL + "/api/vehicles/1"))
                .andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertThatThrownBy(() -> client.getVehicle(1))
                .isInstanceOf(VehicleServiceUnavailableException.class)
                .hasRootCauseInstanceOf(SocketTimeoutException.class);
    }

    @Test
    void throwsUnavailableWhenConnectionIsRefused() {
        server.expect(requestTo(BASE_URL + "/api/vehicles/1"))
                .andRespond(withException(new ConnectException("Connection refused")));

        assertThatThrownBy(() -> client.getVehicle(1)).isInstanceOf(VehicleServiceUnavailableException.class);
    }

    @Test
    void throwsUnavailableOnBrokenJson() {
        server.expect(requestTo(BASE_URL + "/api/vehicles/1"))
                .andRespond(withSuccess("{ kaputt", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.getVehicle(1)).isInstanceOf(VehicleServiceUnavailableException.class);
    }
}
