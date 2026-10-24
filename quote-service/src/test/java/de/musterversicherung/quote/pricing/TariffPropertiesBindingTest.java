package de.musterversicherung.quote.pricing;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static de.musterversicherung.quote.pricing.TestTariffs.money;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lädt die echte application.yml und prüft: Spring liest sie richtig in den Record ein.
 */
@SpringBootTest
class TariffPropertiesBindingTest {

    @Autowired
    private TariffProperties tariff;

    @Test
    void bindsSameValuesAsTestTariffs() {
        assertThat(tariff).isEqualTo(TestTariffs.standard());
    }

    @Test
    void keepsExactDecimalScale() {
        // "1.60" bleibt 1.60 (Scale 2) und wird nicht über double zu 1.6.
        assertThat(tariff.ageFactors().getFirst().factor()).isEqualTo(money("1.60"));
    }
}
