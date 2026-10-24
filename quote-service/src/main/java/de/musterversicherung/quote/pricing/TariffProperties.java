package de.musterversicherung.quote.pricing;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Alle Zahlen der Tarif-Formel aus application.yml (Präfix "tariff").
 * Der kompakte Konstruktor prüft die Werte beim Start: Fehlerhafte Konfiguration -> App startet nicht.
 */
@ConfigurationProperties(prefix = "tariff")
public record TariffProperties(
        Map<Integer, BigDecimal> basePrices,
        List<AgeBand> ageFactors,
        Map<Coverage, BigDecimal> coverageFactors,
        BigDecimal discountPerYear,
        BigDecimal maxDiscount) {

    static final int MIN_DRIVER_AGE = 18;
    static final int MAX_DRIVER_AGE = 99;
    private static final List<Integer> RISK_CLASSES = List.of(1, 2, 3, 4, 5);

    /** Eine Altersgruppe, z. B. 18-24 Jahre mit Faktor 1.60. Grenzen inklusive. */
    public record AgeBand(int minAge, int maxAge, BigDecimal factor) {

        boolean contains(int age) {
            return age >= minAge && age <= maxAge;
        }
    }

    public TariffProperties {
        Objects.requireNonNull(basePrices, "tariff.base-prices is missing");
        Objects.requireNonNull(ageFactors, "tariff.age-factors is missing");
        Objects.requireNonNull(coverageFactors, "tariff.coverage-factors is missing");
        Objects.requireNonNull(discountPerYear, "tariff.discount-per-year is missing");
        Objects.requireNonNull(maxDiscount, "tariff.max-discount is missing");

        if (!basePrices.keySet().containsAll(RISK_CLASSES)) {
            throw new IllegalArgumentException("tariff.base-prices needs risk classes " + RISK_CLASSES);
        }
        if (!coverageFactors.keySet().containsAll(EnumSet.allOf(Coverage.class))) {
            throw new IllegalArgumentException("tariff.coverage-factors needs every coverage type");
        }
        if (maxDiscount.signum() < 0 || maxDiscount.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("tariff.max-discount must be between 0 and 1");
        }

        ageFactors = ageFactors.stream()
                .sorted(Comparator.comparingInt(AgeBand::minAge))
                .toList();
        requireNoGaps(ageFactors);

        // Unveränderliche Kopien: Niemand kann die Konfiguration zur Laufzeit ändern.
        basePrices = Map.copyOf(basePrices);
        coverageFactors = Map.copyOf(coverageFactors);
    }

    private static void requireNoGaps(List<AgeBand> sortedBands) {
        int expectedMinAge = MIN_DRIVER_AGE;
        for (AgeBand band : sortedBands) {
            if (band.minAge() != expectedMinAge || band.maxAge() < band.minAge()) {
                throw new IllegalArgumentException("tariff.age-factors has a gap or overlap at age " + expectedMinAge);
            }
            expectedMinAge = band.maxAge() + 1;
        }
        if (expectedMinAge != MAX_DRIVER_AGE + 1) {
            throw new IllegalArgumentException(
                    "tariff.age-factors must cover ages " + MIN_DRIVER_AGE + " to " + MAX_DRIVER_AGE);
        }
    }
}
