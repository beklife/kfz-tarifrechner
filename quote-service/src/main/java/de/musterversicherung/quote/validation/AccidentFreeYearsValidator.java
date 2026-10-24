package de.musterversicherung.quote.validation;

import de.musterversicherung.quote.api.QuoteRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class AccidentFreeYearsValidator implements ConstraintValidator<ValidAccidentFreeYears, QuoteRequest> {

    static final int MIN_DRIVING_AGE = 18;

    @Override
    public boolean isValid(QuoteRequest request, ConstraintValidatorContext context) {
        if (request == null || request.driverAge() == null || request.accidentFreeYears() == null) {
            // Fehlende Werte meldet schon @NotNull am Feld. Nicht doppelt melden.
            return true;
        }
        if (request.driverAge() < MIN_DRIVING_AGE) {
            // Zu junges Alter meldet schon @Min am Feld driverAge. Sonst gäbe es zwei Fehler für ein Problem.
            return true;
        }
        if (request.accidentFreeYears() <= request.driverAge() - MIN_DRIVING_AGE) {
            return true;
        }
        // Fehler am Feld "accidentFreeYears" melden statt am ganzen Objekt.
        // So kann das Frontend die Meldung direkt unter dem richtigen Eingabefeld zeigen.
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("accidentFreeYears")
                .addConstraintViolation();
        return false;
    }
}
