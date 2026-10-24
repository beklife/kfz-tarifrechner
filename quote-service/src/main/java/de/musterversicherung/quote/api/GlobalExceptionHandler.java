package de.musterversicherung.quote.api;

import de.musterversicherung.quote.client.UnknownVehicleException;
import de.musterversicherung.quote.client.VehicleServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Comparator;
import java.util.List;

/**
 * Übersetzt Exceptions in ProblemDetail-Antworten (RFC 9457) mit deutschen Texten.
 * Eingabefehler haben immer dieselbe Form: Status 400 und eine Liste "errors" mit Feld + Meldung.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    static final String INVALID_INPUT_TITLE = "Ungültige Eingabe";
    static final String UNAVAILABLE_DETAIL =
            "Der Fahrzeugdienst ist gerade nicht erreichbar. Bitte später erneut versuchen.";

    // @Valid ist fehlgeschlagen -> 400 mit allen Feldfehlern
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldViolation> violations = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldViolation(error.getField(), error.getDefaultMessage()))
                .sorted(Comparator.comparing(FieldViolation::field))
                .toList();
        return handleExceptionInternal(exception, invalidInput(violations), headers, status, request);
    }

    // Kaputtes JSON oder falscher Wert, z. B. coverage = "SUPERKASKO" -> 400
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Die Anfrage konnte nicht gelesen werden. Bitte die Eingaben prüfen.");
        problem.setTitle(INVALID_INPUT_TITLE);
        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    // Fahrzeug-ID gibt es nicht -> 400 (nicht 404), gleiche Form wie ein Validierungsfehler.
    // Grund: Die URL /api/quotes existiert. Falsch ist ein Wert im Body.
    @ExceptionHandler(UnknownVehicleException.class)
    public ProblemDetail handleUnknownVehicle(UnknownVehicleException exception) {
        return invalidInput(List.of(new FieldViolation("vehicleId",
                "Das Fahrzeug mit der ID " + exception.getVehicleId() + " gibt es nicht.")));
    }

    @ExceptionHandler(VehicleServiceUnavailableException.class)
    public ProblemDetail handleVehicleServiceUnavailable(VehicleServiceUnavailableException exception) {
        log.warn("vehicle-service unavailable: {}", exception.getMessage(), exception.getCause());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, UNAVAILABLE_DETAIL);
        problem.setTitle("Fahrzeugdienst nicht erreichbar");
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception exception) {
        log.error("Unexpected error", exception);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ein unerwarteter Fehler ist aufgetreten. Bitte später erneut versuchen.");
        problem.setTitle("Interner Fehler");
        return problem;
    }

    private static ProblemDetail invalidInput(List<FieldViolation> violations) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Bitte die markierten Eingaben prüfen.");
        problem.setTitle(INVALID_INPUT_TITLE);
        problem.setProperty("errors", violations);
        return problem;
    }
}
