package co.edu.uco.application.usecase.validator.specification.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidDateSpecificationTest {

    private final ValidDateSpecification specification = new ValidDateSpecification();

    @Test
    void isSatisfiedBy_returnsFalse_whenCandidateIsNull() {
        assertFalse(specification.isSatisfiedBy(null));
    }

    @Test
    void isSatisfiedBy_returnsFalse_whenCandidateIsEmpty() {
        assertFalse(specification.isSatisfiedBy(""));
    }

    @Test
    void isSatisfiedBy_returnsFalse_whenCandidateHasInvalidText() {
        assertFalse(specification.isSatisfiedBy("not-a-date"));
    }

    @Test
    void isSatisfiedBy_returnsFalse_whenCandidateHasInvalidFormat() {
        assertFalse(specification.isSatisfiedBy("2025/13/45"));
    }

    @Test
    void isSatisfiedBy_returnsTrue_whenCandidateIsValidDate() {
        assertTrue(specification.isSatisfiedBy("2025-01-01T00:00:00"));
    }
}