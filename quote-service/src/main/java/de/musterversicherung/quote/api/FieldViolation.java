package de.musterversicherung.quote.api;

/**
 * Ein Fehler an einem Eingabefeld. Steht in ProblemDetail unter "errors".
 */
public record FieldViolation(String field, String message) {
}
