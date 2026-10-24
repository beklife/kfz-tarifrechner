package de.musterversicherung.quote.pricing;

import de.musterversicherung.quote.pricing.TariffProperties.AgeBand;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static de.musterversicherung.quote.pricing.TestTariffs.money;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prüft das "Fail fast": Falsche Konfiguration soll beim Start auffallen, nicht erst bei der ersten Anfrage.
 */
class TariffPropertiesTest {

    private final TariffProperties valid = TestTariffs.standard();

    @Test
    void sortsAgeBands() {
        List<AgeBand> unsorted = TestTariffs.standardAgeBands().reversed();

        TariffProperties properties = withAgeBands(unsorted);

        assertThat(properties.ageFactors()).extracting(AgeBand::minAge).containsExactly(18, 25, 30, 65);
    }

    @Test
    void rejectsGapInAgeBands() {
        List<AgeBand> withGap = List.of(
                new AgeBand(18, 24, money("1.60")),
                new AgeBand(26, 99, money("1.00")));

        assertThatThrownBy(() -> withAgeBands(withGap))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("gap");
    }

    @Test
    void rejectsAgeBandsThatDoNotReach99() {
        List<AgeBand> tooShort = List.of(new AgeBand(18, 80, money("1.00")));

        assertThatThrownBy(() -> withAgeBands(tooShort)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingCoverageFactor() {
        assertThatThrownBy(() -> new TariffProperties(
                valid.basePrices(), valid.ageFactors(),
                Map.of(Coverage.HAFTPFLICHT, money("1.00")),
                valid.discountPerYear(), valid.maxDiscount()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("coverage");
    }

    @Test
    void rejectsMissingRiskClass() {
        assertThatThrownBy(() -> new TariffProperties(
                Map.of(1, money("300.00")), valid.ageFactors(), valid.coverageFactors(),
                valid.discountPerYear(), valid.maxDiscount()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("risk classes");
    }

    @Test
    void rejectsMaxDiscountAboveOneHundredPercent() {
        assertThatThrownBy(() -> new TariffProperties(
                valid.basePrices(), valid.ageFactors(), valid.coverageFactors(),
                valid.discountPerYear(), money("1.10")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private TariffProperties withAgeBands(List<AgeBand> ageBands) {
        return new TariffProperties(valid.basePrices(), ageBands, valid.coverageFactors(),
                valid.discountPerYear(), valid.maxDiscount());
    }
}
