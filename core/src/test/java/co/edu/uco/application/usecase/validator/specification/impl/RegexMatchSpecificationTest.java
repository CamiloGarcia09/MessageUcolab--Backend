package co.edu.uco.application.usecase.validator.specification.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegexMatchSpecificationTest {

    private final RegexMatchSpecification specification = new RegexMatchSpecification("[0-9]+");

    @Test
    void isSatisfiedBy_returnsFalse_whenCandidateIsNull() {
        assertFalse(specification.isSatisfiedBy(null));
    }

    @Test
    void isSatisfiedBy_returnsFalse_whenCandidateIsEmpty() {
        assertFalse(specification.isSatisfiedBy(""));
    }

    @Test
    void isSatisfiedBy_returnsFalse_whenCandidateDoesNotMatch() {
        assertFalse(specification.isSatisfiedBy("abc"));
    }

    @Test
    void isSatisfiedBy_returnsTrue_whenCandidateMatches() {
        assertTrue(specification.isSatisfiedBy("12345"));
    }
}