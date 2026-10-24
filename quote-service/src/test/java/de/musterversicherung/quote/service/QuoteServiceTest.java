package de.musterversicherung.quote.service;

import de.musterversicherung.quote.api.QuoteRequest;
import de.musterversicherung.quote.client.UnknownVehicleException;
import de.musterversicherung.quote.client.VehicleClient;
import de.musterversicherung.quote.client.VehicleInfo;
import de.musterversicherung.quote.pricing.Coverage;
import de.musterversicherung.quote.pricing.PriceCalculator;
import de.musterversicherung.quote.pricing.PriceResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuoteServiceTest {

    @Mock
    private VehicleClient vehicleClient;

    @Mock
    private PriceCalculator priceCalculator;

    @InjectMocks
    private QuoteService quoteService;

    @Test
    void usesRiskClassOfVehicleForCalculation() {
        PriceResult price = new PriceResult(new BigDecimal("456.88"), new BigDecimal("38.07"), List.of());
        when(vehicleClient.getVehicle(4)).thenReturn(new VehicleInfo(4, "Rheinmobil", "Touring 500", 3));
        when(priceCalculator.calculate(3, 40, 5, Coverage.TEILKASKO)).thenReturn(price);

        Quote quote = quoteService.createQuote(new QuoteRequest(4L, 40, 5, Coverage.TEILKASKO));

        assertThat(quote.vehicleName()).isEqualTo("Rheinmobil Touring 500");
        assertThat(quote.coverage()).isEqualTo(Coverage.TEILKASKO);
        assertThat(quote.price()).isEqualTo(price);
    }

    @Test
    void doesNotCalculateWhenVehicleIsUnknown() {
        when(vehicleClient.getVehicle(99)).thenThrow(new UnknownVehicleException(99));

        assertThatThrownBy(() -> quoteService.createQuote(new QuoteRequest(99L, 40, 5, Coverage.TEILKASKO)))
                .isInstanceOf(UnknownVehicleException.class);
        verifyNoInteractions(priceCalculator);
    }
}
