package co.edu.uco.application.usecase.validator.specification.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidUuidSpecificationTest {

    private static final String VALID_UUID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String DEFAULT_UUID = "00000000-0000-0000-0000-000000000000";

    private final ValidUuidSpecification specification = new ValidUuidSpecification();

    @Test
    void isSatisfiedBy_returnsFalse_whenCandidateIsNull() {
        assertFalse(specification.isSatisfiedBy(null));
    }

    @Test
    void isSatisfiedBy_returnsFalse_whenCandidateIsEmpty() {
        assertFalse(specification.isSatisfiedBy(""));
    }

    @Test
    void isSatisfiedBy_returnsFalse_whenCandidateIsNotAUuid() {
        assertFalse(specification.isSatisfiedBy("not-a-uuid"));
    }

    @Test
    void isSatisfiedBy_returnsFalse_whenCandidateIsDefaultUuid() {
        assertFalse(specification.isSatisfiedBy(DEFAULT_UUID));
    }

    @Test
    void isSatisfiedBy_returnsTrue_whenCandidateIsValidUuid() {
        assertTrue(specification.isSatisfiedBy(VALID_UUID));
    }
}