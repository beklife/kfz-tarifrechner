package de.musterversicherung.quote.service;

import de.musterversicherung.quote.pricing.Coverage;
import de.musterversicherung.quote.pricing.PriceResult;

/**
 * Ergebnis einer Tarifberechnung (Fachobjekt, nicht API).
 */
public record Quote(String vehicleName, Coverage coverage, PriceResult price) {
}
