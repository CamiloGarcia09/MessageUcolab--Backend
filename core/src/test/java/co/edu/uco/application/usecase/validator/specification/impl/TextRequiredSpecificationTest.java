package co.edu.uco.application.usecase.validator.specification.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextRequiredSpecificationTest {

    private final TextRequiredSpecification specification = new TextRequiredSpecification();

    @Test
    void isSatisfiedBy_returnsFalse_whenCandidateIsNull() {
        assertFalse(specification.isSatisfiedBy(null));
    }

    @Test
    void isSatisfiedBy_returnsFalse_whenCandidateIsEmpty() {
        assertFalse(specification.isSatisfiedBy(""));
    }

    @Test
    void isSatisfiedBy_returnsFalse_whenCandidateIsBlank() {
        assertFalse(specification.isSatisfiedBy("   "));
    }

    @Test
    void isSatisfiedBy_returnsTrue_whenCandidateHasContent() {
        assertTrue(specification.isSatisfiedBy("value"));
    }

    @Test
    void isSatisfiedBy_returnsTrue_whenCandidateHasContentSpaced() {
        assertTrue(specification.isSatisfiedBy("  value  "));
    }
}