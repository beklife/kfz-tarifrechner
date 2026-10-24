package de.musterversicherung.quote.api;

import de.musterversicherung.quote.pricing.Coverage;

import java.math.BigDecimal;
import java.util.List;

/**
 * Antwort von POST /api/quotes.
 */
public record QuoteResponse(
        String vehicleName,
        Coverage coverage,
        BigDecimal yearlyPrice,
        BigDecimal monthlyPrice,
        List<BreakdownStep> breakdown) {

    public record BreakdownStep(int step, String description, BigDecimal factorOrAmount, BigDecimal subtotal) {
    }
}
