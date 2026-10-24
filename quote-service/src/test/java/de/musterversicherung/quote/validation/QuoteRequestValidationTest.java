package de.musterversicherung.quote.validation;

import de.musterversicherung.quote.api.QuoteRequest;
import de.musterversicherung.quote.pricing.Coverage;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testet alle Regeln von QuoteRequest mit dem echten Bean-Validation-Validator, aber ohne Spring.
 */
class QuoteRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void acceptsValidRequest() {
        assertThat(validate(new QuoteRequest(1L, 40, 10, Coverage.VOLLKASKO))).isEmpty();
    }

    @ParameterizedTest(name = "age {0}, accident-free {1} -> valid")
    @CsvSource({"18, 0", "30, 12", "68, 50", "99, 50"})
    void acceptsAccidentFreeYearsUpToAgeMinus18(int age, int years) {
        assertThat(validate(new QuoteRequest(1L, age, years, Coverage.HAFTPFLICHT))).isEmpty();
    }

    @ParameterizedTest(name = "age {0}, accident-free {1} -> invalid")
    @CsvSource({"18, 1", "30, 13", "40, 23"})
    void rejectsMoreAccidentFreeYearsThanPossible(int age, int years) {
        Set<ConstraintViolation<QuoteRequest>> violations =
                validate(new QuoteRequest(1L, age, years, Coverage.HAFTPFLICHT));

        assertThat(violations).singleElement().satisfies(violation -> {
            // Der Fehler hängt am Feld, nicht am ganzen Objekt -> Frontend kann ihn zuordnen.
            assertThat(violation.getPropertyPath()).hasToString("accidentFreeYears");
            assertThat(violation.getMessage()).contains("Alter minus 18");
        });
    }

    @ParameterizedTest(name = "age {0} -> invalid")
    @CsvSource({"17", "100"})
    void rejectsAgeOutOfRange(int age) {
        assertThat(fieldsWithErrors(new QuoteRequest(1L, age, 0, Coverage.HAFTPFLICHT)))
                .containsExactly("driverAge");
    }

    @Test
    void rejectsMoreThanFiftyAccidentFreeYears() {
        assertThat(fieldsWithErrors(new QuoteRequest(1L, 99, 51, Coverage.HAFTPFLICHT)))
                .containsExactly("accidentFreeYears");
    }

    @Test
    void reportsEveryMissingFieldOnce() {
        // Der Cross-Field-Validator meldet bei null nichts, sonst stünde accidentFreeYears doppelt da.
        assertThat(fieldsWithErrors(new QuoteRequest(null, null, null, null)))
                .containsExactlyInAnyOrder("vehicleId", "driverAge", "accidentFreeYears", "coverage");
    }

    @Test
    void rejectsNonPositiveVehicleId() {
        assertThat(fieldsWithErrors(new QuoteRequest(0L, 40, 0, Coverage.HAFTPFLICHT)))
                .containsExactly("vehicleId");
    }

    private Set<ConstraintViolation<QuoteRequest>> validate(QuoteRequest request) {
        return validator.validate(request);
    }

    private java.util.List<String> fieldsWithErrors(QuoteRequest request) {
        return validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .toList();
    }
}
