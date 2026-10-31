package co.edu.uco.application.usecase.validator.specification.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextMaxLengthSpecificationTest {

    private final TextMaxLengthSpecification specification = new TextMaxLengthSpecification(5);

    @Test
    void isSatisfiedBy_returnsTrue_whenCandidateIsNull() {
        assertTrue(specification.isSatisfiedBy(null));
    }

    @Test
    void isSatisfiedBy_returnsTrue_whenCandidateIsEmpty() {
        assertTrue(specification.isSatisfiedBy(""));
    }

    @Test
    void isSatisfiedBy_returnsTrue_whenLengthIsExactlyMaximum() {
        assertTrue(specification.isSatisfiedBy("abcde"));
    }

    @Test
    void isSatisfiedBy_returnsTrue_whenLengthIsBelowMaximum() {
        assertTrue(specification.isSatisfiedBy("abc"));
    }

    @Test
    void isSatisfiedBy_returnsFalse_whenLengthIsAboveMaximum() {
        assertFalse(specification.isSatisfiedBy("abcdef"));
    }

    @Test
    void isSatisfiedBy_trimsBeforeEvaluating() {
        assertTrue(specification.isSatisfiedBy("  abcde  "));
    }
}