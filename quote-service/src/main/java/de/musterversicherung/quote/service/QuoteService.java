package de.musterversicherung.quote.service;

import de.musterversicherung.quote.api.QuoteRequest;
import de.musterversicherung.quote.client.VehicleClient;
import de.musterversicherung.quote.client.VehicleInfo;
import de.musterversicherung.quote.pricing.PriceCalculator;
import de.musterversicherung.quote.pricing.PriceResult;
import org.springframework.stereotype.Service;

/**
 * Verbindet die Teile: Fahrzeug holen (anderer Service) -> Preis berechnen (eigene Logik).
 */
@Service
public class QuoteService {

    private final VehicleClient vehicleClient;
    private final PriceCalculator priceCalculator;

    public QuoteService(VehicleClient vehicleClient, PriceCalculator priceCalculator) {
        this.vehicleClient = vehicleClient;
        this.priceCalculator = priceCalculator;
    }

    public Quote createQuote(QuoteRequest request) {
        VehicleInfo vehicle = vehicleClient.getVehicle(request.vehicleId());
        PriceResult price = priceCalculator.calculate(
                vehicle.riskClass(), request.driverAge(), request.accidentFreeYears(), request.coverage());
        return new Quote(vehicle.displayName(), request.coverage(), price);
    }
}
