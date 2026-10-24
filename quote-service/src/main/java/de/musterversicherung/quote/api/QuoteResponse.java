package de.musterversicherung.quote.api;

import de.musterversicherung.quote.pricing.Coverage;
import de.musterversicherung.quote.pricing.PriceStep;
import de.musterversicherung.quote.service.Quote;

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

        static BreakdownStep from(PriceStep priceStep) {
            return new BreakdownStep(priceStep.step(), priceStep.description(),
                    priceStep.factorOrAmount(), priceStep.subtotal());
        }
    }

    public static QuoteResponse from(Quote quote) {
        return new QuoteResponse(
                quote.vehicleName(),
                quote.coverage(),
                quote.price().yearlyPrice(),
                quote.price().monthlyPrice(),
                quote.price().breakdown().stream().map(BreakdownStep::from).toList());
    }
}
