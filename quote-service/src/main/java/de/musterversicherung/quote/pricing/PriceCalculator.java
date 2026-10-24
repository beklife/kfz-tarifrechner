package de.musterversicherung.quote.pricing;

import de.musterversicherung.quote.pricing.TariffProperties.AgeBand;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Die erfundene Demo-Formel:
 * Jahrespreis = Grundpreis × Altersfaktor × Deckungsfaktor × (1 − Rabatt), erst am Ende auf Cent gerundet.
 * Monatspreis = Jahrespreis / 12, auf Cent gerundet.
 */
@Component
public class PriceCalculator {

    private static final int MONEY_SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final BigDecimal MONTHS_PER_YEAR = BigDecimal.valueOf(12);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final TariffProperties tariff;

    public PriceCalculator(TariffProperties tariff) {
        this.tariff = tariff;
    }

    public PriceResult calculate(int riskClass, int driverAge, int accidentFreeYears, Coverage coverage) {
        List<PriceStep> breakdown = new ArrayList<>();

        BigDecimal basePrice = basePriceFor(riskClass);
        BigDecimal amount = basePrice;
        breakdown.add(step(1, "Grundpreis für Risikoklasse " + riskClass, basePrice, amount));

        AgeBand ageBand = ageBandFor(driverAge);
        amount = amount.multiply(ageBand.factor());
        breakdown.add(step(2, "Altersfaktor (" + ageBand.minAge() + "–" + ageBand.maxAge() + " Jahre)",
                ageBand.factor(), amount));

        BigDecimal coverageFactor = tariff.coverageFactors().get(coverage);
        amount = amount.multiply(coverageFactor);
        breakdown.add(step(3, "Deckungsfaktor (" + coverage.displayName() + ")", coverageFactor, amount));

        BigDecimal discount = discountFor(accidentFreeYears);
        BigDecimal discountFactor = BigDecimal.ONE.subtract(discount);
        amount = amount.multiply(discountFactor);
        breakdown.add(step(4, discountDescription(accidentFreeYears, discount), discountFactor, amount));

        // Die einzige Rundung der Rechnung. Vorher wird immer mit dem exakten Wert gerechnet.
        BigDecimal yearlyPrice = amount.setScale(MONEY_SCALE, ROUNDING);
        // divide() braucht Scale und RoundingMode, sonst Exception bei unendlichen Dezimalzahlen (z. B. 100/12).
        BigDecimal monthlyPrice = yearlyPrice.divide(MONTHS_PER_YEAR, MONEY_SCALE, ROUNDING);

        return new PriceResult(yearlyPrice, monthlyPrice, breakdown);
    }

    private BigDecimal basePriceFor(int riskClass) {
        BigDecimal basePrice = tariff.basePrices().get(riskClass);
        if (basePrice == null) {
            throw new IllegalArgumentException("Unknown risk class: " + riskClass);
        }
        return basePrice;
    }

    private AgeBand ageBandFor(int driverAge) {
        return tariff.ageFactors().stream()
                .filter(band -> band.contains(driverAge))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No age factor for age " + driverAge));
    }

    /** 3 % pro Jahr, aber höchstens maxDiscount (50 %). */
    private BigDecimal discountFor(int accidentFreeYears) {
        if (accidentFreeYears < 0) {
            throw new IllegalArgumentException("accidentFreeYears must not be negative: " + accidentFreeYears);
        }
        BigDecimal uncapped = tariff.discountPerYear().multiply(BigDecimal.valueOf(accidentFreeYears));
        return uncapped.min(tariff.maxDiscount());
    }

    private String discountDescription(int accidentFreeYears, BigDecimal discount) {
        BigDecimal uncapped = tariff.discountPerYear().multiply(BigDecimal.valueOf(accidentFreeYears));
        String text = "Schadenfreiheitsrabatt: " + accidentFreeYears + " Jahre × "
                + percent(tariff.discountPerYear()) + " % = " + percent(uncapped) + " %";
        if (uncapped.compareTo(discount) > 0) {
            text += ", begrenzt auf " + percent(discount) + " %";
        }
        return text;
    }

    /** 0.15 -> "15" */
    private static String percent(BigDecimal fraction) {
        return fraction.multiply(HUNDRED).stripTrailingZeros().toPlainString();
    }

    // Zwischensumme nur für die Anzeige runden. Die Variable "amount" oben bleibt exakt.
    private static PriceStep step(int number, String description, BigDecimal factorOrAmount, BigDecimal amount) {
        return new PriceStep(number, description, factorOrAmount, amount.setScale(MONEY_SCALE, ROUNDING));
    }
}
