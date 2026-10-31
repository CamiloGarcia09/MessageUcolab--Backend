package co.edu.uco.application.usecase.validator.specification.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextMinLengthSpecificationTest {

    private final TextMinLengthSpecification specification = new TextMinLengthSpecification(5);

    @Test
    void isSatisfiedBy_returnsFalse_whenCandidateIsNull() {
        assertFalse(specification.isSatisfiedBy(null));
    }

    @Test
    void isSatisfiedBy_returnsFalse_whenCandidateIsEmpty() {
        assertFalse(specification.isSatisfiedBy(""));
    }

    @Test
    void isSatisfiedBy_returnsFalse_whenLengthIsBelowMinimum() {
        assertFalse(specification.isSatisfiedBy("abc"));
    }

    @Test
    void isSatisfiedBy_returnsTrue_whenLengthIsExactlyMinimum() {
        assertTrue(specification.isSatisfiedBy("abcde"));
    }

    @Test
    void isSatisfiedBy_returnsTrue_whenLengthIsAboveMinimum() {
        assertTrue(specification.isSatisfiedBy("abcdef"));
    }

    @Test
    void isSatisfiedBy_trimsBeforeEvaluating() {
        assertTrue(specification.isSatisfiedBy("  abcde  "));
    }
}