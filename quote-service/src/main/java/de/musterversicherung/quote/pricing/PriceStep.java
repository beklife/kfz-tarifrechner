package de.musterversicherung.quote.pricing;

import java.math.BigDecimal;

/**
 * Ein Schritt der Preisberechnung, z. B. "Altersfaktor (18–24 Jahre)", 1.60, 688.00.
 *
 * @param factorOrAmount beim Grundpreis ein Betrag in Euro, sonst ein Faktor
 * @param subtotal       Zwischensumme nach diesem Schritt, nur für die Anzeige auf Cent gerundet
 */
public record PriceStep(int step, String description, BigDecimal factorOrAmount, BigDecimal subtotal) {
}
