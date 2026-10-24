package de.musterversicherung.quote.pricing;

import de.musterversicherung.quote.pricing.TariffProperties.AgeBand;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Dieselben Werte wie in application.yml, aber ohne Spring. So bleiben die Unit-Tests schnell.
 * TariffPropertiesBindingTest prüft, dass application.yml wirklich diese Werte enthält.
 */
final class TestTariffs {

    private TestTariffs() {
    }

    static TariffProperties standard() {
        return new TariffProperties(
                Map.of(1, money("300.00"), 2, money("360.00"), 3, money("430.00"),
                        4, money("510.00"), 5, money("600.00")),
                standardAgeBands(),
                Map.of(Coverage.HAFTPFLICHT, money("1.00"),
                        Coverage.TEILKASKO, money("1.25"),
                        Coverage.VOLLKASKO, money("1.70")),
                money("0.03"),
                money("0.50"));
    }

    static List<AgeBand> standardAgeBands() {
        return List.of(
                new AgeBand(18, 24, money("1.60")),
                new AgeBand(25, 29, money("1.20")),
                new AgeBand(30, 64, money("1.00")),
                new AgeBand(65, 99, money("1.15")));
    }

    static BigDecimal money(String value) {
        return new BigDecimal(value);
    }
}
