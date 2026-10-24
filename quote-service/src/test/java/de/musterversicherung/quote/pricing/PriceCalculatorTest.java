package de.musterversicherung.quote.pricing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static de.musterversicherung.quote.pricing.TestTariffs.money;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class PriceCalculatorTest {

    private final PriceCalculator calculator = new PriceCalculator(TestTariffs.standard());

    @Test
    void calculatesFullExample() {
        // 430 × 1.00 × 1.25 × (1 − 0.15) = 456.875 -> 456.88; 456.88 / 12 = 38.0733 -> 38.07
        PriceResult result = calculator.calculate(3, 40, 5, Coverage.TEILKASKO);

        // isEqualTo prüft auch die Scale: "456.88" ist nicht gleich "456.880".
        assertThat(result.yearlyPrice()).isEqualTo(money("456.88"));
        assertThat(result.monthlyPrice()).isEqualTo(money("38.07"));
    }

    @Test
    void breakdownShowsEveryStepInOrder() {
        PriceResult result = calculator.calculate(3, 40, 5, Coverage.TEILKASKO);

        assertThat(result.breakdown())
                .extracting(PriceStep::step, PriceStep::description, PriceStep::factorOrAmount, PriceStep::subtotal)
                .containsExactly(
                        tuple(1, "Grundpreis für Risikoklasse 3", money("430.00"), money("430.00")),
                        tuple(2, "Altersfaktor (30–64 Jahre)", money("1.00"), money("430.00")),
                        tuple(3, "Deckungsfaktor (Teilkasko)", money("1.25"), money("537.50")),
                        tuple(4, "Schadenfreiheitsrabatt: 5 Jahre × 3 % = 15 %", money("0.85"), money("456.88")));
    }

    @ParameterizedTest(name = "risk class {0} -> base price {1}")
    @CsvSource({"1, 300.00", "2, 360.00", "3, 430.00", "4, 510.00", "5, 600.00"})
    void usesBasePricePerRiskClass(int riskClass, String expectedYearly) {
        // Alter 40 (Faktor 1.00), Haftpflicht (1.00), 0 Jahre (kein Rabatt) -> nur der Grundpreis bleibt
        assertThat(calculator.calculate(riskClass, 40, 0, Coverage.HAFTPFLICHT).yearlyPrice())
                .isEqualTo(money(expectedYearly));
    }

    @ParameterizedTest(name = "age {0} -> factor {1}")
    @CsvSource({
            "18, 1.60", "24, 1.60",
            "25, 1.20", "29, 1.20",
            "30, 1.00", "64, 1.00",
            "65, 1.15", "99, 1.15"})
    void usesAgeFactorAtEveryBoundary(int age, String expectedFactor) {
        PriceResult result = calculator.calculate(1, age, 0, Coverage.HAFTPFLICHT);

        assertThat(result.breakdown().get(1).factorOrAmount()).isEqualTo(money(expectedFactor));
        assertThat(result.yearlyPrice())
                .isEqualTo(money("300.00").multiply(money(expectedFactor)).setScale(2));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({"HAFTPFLICHT, 300.00", "TEILKASKO, 375.00", "VOLLKASKO, 510.00"})
    void usesCoverageFactor(Coverage coverage, String expectedYearly) {
        assertThat(calculator.calculate(1, 40, 0, coverage).yearlyPrice()).isEqualTo(money(expectedYearly));
    }

    @ParameterizedTest(name = "{0} accident-free years -> {1}")
    @CsvSource({
            "0, 300.00",   // 0 %
            "1, 291.00",   // 3 %
            "10, 210.00",  // 30 %
            "16, 156.00",  // 48 %, knapp unter der Grenze
            "17, 150.00",  // wären 51 %, begrenzt auf 50 %
            "50, 150.00"}) // wären 150 %, begrenzt auf 50 %
    void appliesDiscountWithCapAtFiftyPercent(int years, String expectedYearly) {
        // Der Rechner prüft "Jahre <= Alter - 18" bewusst nicht. Das macht die Validierung (AccidentFreeYearsValidator).
        assertThat(calculator.calculate(1, 40, years, Coverage.HAFTPFLICHT).yearlyPrice())
                .isEqualTo(money(expectedYearly));
    }

    @Test
    void describesCappedDiscount() {
        PriceResult result = calculator.calculate(1, 40, 20, Coverage.HAFTPFLICHT);

        assertThat(result.breakdown().get(3).description())
                .isEqualTo("Schadenfreiheitsrabatt: 20 Jahre × 3 % = 60 %, begrenzt auf 50 %");
        assertThat(result.breakdown().get(3).factorOrAmount()).isEqualByComparingTo("0.50");
    }

    @Test
    void roundsHalfUpOnlyAtTheEnd() {
        // 430 × 1.15 × 1.25 = 618.125 exakt.
        // HALF_UP -> 618.13. (HALF_EVEN, die Java-Standard-"Bankrundung", ergäbe 618.12.)
        PriceResult result = calculator.calculate(3, 70, 0, Coverage.TEILKASKO);

        assertThat(result.yearlyPrice()).isEqualTo(money("618.13"));
        // Monatspreis aus dem gerundeten Jahrespreis: 618.13 / 12 = 51.5108... -> 51.51
        assertThat(result.monthlyPrice()).isEqualTo(money("51.51"));
    }

    @Test
    void doesNotRoundIntermediateResults() {
        // 430 × 1.15 = 494.50; × 1.25 = 618.125 (nicht gerundet!); × 0.97 = 599.58125 -> 599.58.
        // Mit Zwischenrundung: 618.13 × 0.97 = 599.5861 -> 599.59 (1 Cent falsch).
        PriceResult result = calculator.calculate(3, 70, 1, Coverage.TEILKASKO);

        assertThat(result.yearlyPrice()).isEqualTo(money("599.58"));
    }

    @Test
    void roundsMonthlyPriceHalfUp() {
        // 510 × 1.00 × 1.00 × 0.97 = 494.70; 494.70 / 12 = 41.225 -> HALF_UP 41.23 (HALF_EVEN: 41.22)
        PriceResult result = calculator.calculate(4, 40, 1, Coverage.HAFTPFLICHT);

        assertThat(result.yearlyPrice()).isEqualTo(money("494.70"));
        assertThat(result.monthlyPrice()).isEqualTo(money("41.23"));
    }

    @Test
    void lastSubtotalEqualsYearlyPrice() {
        PriceResult result = calculator.calculate(5, 22, 3, Coverage.VOLLKASKO);

        assertThat(result.breakdown().getLast().subtotal()).isEqualTo(result.yearlyPrice());
    }

    @Test
    void rejectsUnknownRiskClass() {
        assertThatThrownBy(() -> calculator.calculate(6, 40, 0, Coverage.HAFTPFLICHT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("risk class");
    }

    @Test
    void rejectsAgeOutsideConfiguredBands() {
        assertThatThrownBy(() -> calculator.calculate(1, 17, 0, Coverage.HAFTPFLICHT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("17");
    }

    @Test
    void rejectsNegativeAccidentFreeYears() {
        assertThatThrownBy(() -> calculator.calculate(1, 40, -1, Coverage.HAFTPFLICHT))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
