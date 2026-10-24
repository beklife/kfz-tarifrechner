package de.musterversicherung.quote.api;

import de.musterversicherung.quote.pricing.Coverage;
import de.musterversicherung.quote.validation.ValidAccidentFreeYears;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Eingabe für POST /api/quotes.
 * Wrapper-Typen (Long, Integer) statt long/int: Ein fehlendes Feld wird null und
 * dann von @NotNull gemeldet. Mit int wäre es still 0.
 */
@ValidAccidentFreeYears
public record QuoteRequest(
        @NotNull(message = "Bitte ein Fahrzeug auswählen.")
        @Positive(message = "Die Fahrzeug-ID muss positiv sein.")
        Long vehicleId,

        @NotNull(message = "Bitte das Alter angeben.")
        @Min(value = 18, message = "Das Alter muss mindestens 18 sein.")
        @Max(value = 99, message = "Das Alter darf höchstens 99 sein.")
        Integer driverAge,

        @NotNull(message = "Bitte die unfallfreien Jahre angeben.")
        @Min(value = 0, message = "Die unfallfreien Jahre dürfen nicht negativ sein.")
        @Max(value = 50, message = "Die unfallfreien Jahre dürfen höchstens 50 sein.")
        Integer accidentFreeYears,

        @NotNull(message = "Bitte eine Versicherungsart auswählen.")
        Coverage coverage) {
}
