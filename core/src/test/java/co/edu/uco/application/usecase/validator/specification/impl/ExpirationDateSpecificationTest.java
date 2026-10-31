package co.edu.uco.application.usecase.validator.specification.impl;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpirationDateSpecificationTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2025, 1, 1, 0, 0);

    private final ExpirationDateSpecification specification = new ExpirationDateSpecification(NOW);

    @Test
    void isSatisfiedBy_returnsFalse_whenCandidateIsNull() {
        assertFalse(specification.isSatisfiedBy(null));
    }

    @Test
    void isSatisfiedBy_returnsFalse_whenCandidateIsBeforeNow() {
        assertFalse(specification.isSatisfiedBy(NOW.minusDays(1)));
    }

    @Test
    void isSatisfiedBy_returnsFalse_whenCandidateIsEqualToNow() {
        assertFalse(specification.isSatisfiedBy(NOW));
    }

    @Test
    void isSatisfiedBy_returnsTrue_whenCandidateIsAfterNow() {
        assertTrue(specification.isSatisfiedBy(NOW.plusDays(1)));
    }
}