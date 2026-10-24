package de.musterversicherung.quote.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient vehicleRestClient(RestClient.Builder builder, VehicleServiceProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        // Connect-Timeout: so lange warten wir, bis die Verbindung steht.
        requestFactory.setConnectTimeout(properties.connectTimeout());
        // Read-Timeout: so lange warten wir auf die Antwort, wenn die Verbindung schon steht.
        requestFactory.setReadTimeout(properties.readTimeout());

        return builder
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}
