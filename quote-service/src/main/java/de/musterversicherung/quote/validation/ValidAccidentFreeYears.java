package de.musterversicherung.quote.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Feldübergreifende Prüfung: Man kann erst ab 18 Auto fahren.
 * Also gilt: unfallfreie Jahre <= Alter - 18.
 * Steht an der Klasse (nicht am Feld), weil zwei Felder zusammen geprüft werden.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AccidentFreeYearsValidator.class)
public @interface ValidAccidentFreeYears {

    String message() default "Die unfallfreien Jahre dürfen nicht größer sein als Alter minus 18.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
