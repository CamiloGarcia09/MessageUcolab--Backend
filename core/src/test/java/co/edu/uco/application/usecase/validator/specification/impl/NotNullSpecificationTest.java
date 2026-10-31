package co.edu.uco.application.usecase.validator.specification.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotNullSpecificationTest {

    private final NotNullSpecification<Object> specification = new NotNullSpecification<>();

    @Test
    void isSatisfiedBy_returnsFalse_whenCandidateIsNull() {
        assertFalse(specification.isSatisfiedBy(null));
    }

    @Test
    void isSatisfiedBy_returnsTrue_whenCandidateIsPresent() {
        assertTrue(specification.isSatisfiedBy("value"));
    }
}