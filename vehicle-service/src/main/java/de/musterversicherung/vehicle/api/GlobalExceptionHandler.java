package de.musterversicherung.vehicle.api;

import de.musterversicherung.vehicle.domain.VehicleNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Übersetzt Exceptions in HTTP-Antworten im Format ProblemDetail (RFC 9457).
 * Die Basisklasse kümmert sich schon um Spring-eigene Fehler (z. B. falscher Parametertyp -> 400).
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(VehicleNotFoundException.class)
    public ProblemDetail handleVehicleNotFound(VehicleNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                "Es gibt kein Fahrzeug mit der ID " + exception.getVehicleId() + ".");
        problem.setTitle("Fahrzeug nicht gefunden");
        return problem;
    }

    // Letztes Netz: Details nur ins Log, nie an den Client (keine internen Infos nach außen).
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception exception) {
        log.error("Unexpected error", exception);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ein unerwarteter Fehler ist aufgetreten. Bitte später erneut versuchen.");
        problem.setTitle("Interner Fehler");
        return problem;
    }
}
