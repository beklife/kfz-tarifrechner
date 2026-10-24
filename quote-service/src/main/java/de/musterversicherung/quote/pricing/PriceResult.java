package de.musterversicherung.quote.pricing;

import java.math.BigDecimal;
import java.util.List;

public record PriceResult(BigDecimal yearlyPrice, BigDecimal monthlyPrice, List<PriceStep> breakdown) {

    public PriceResult {
        breakdown = List.copyOf(breakdown);
    }
}
