package co.edu.uco.application.usecase.validator.specification.impl;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DateRangeOrderSpecificationTest {

    private static final LocalDateTime START = LocalDateTime.of(2025, 1, 1, 0, 0);
    private static final LocalDateTime END = LocalDateTime.of(2025, 12, 31, 23, 59);

    private final DateRangeOrderSpecification specification = new DateRangeOrderSpecification();

    @Test
    void isSatisfiedBy_returnsFalse_whenCandidateIsNull() {
        assertFalse(specification.isSatisfiedBy(null));
    }

    @Test
    void isSatisfiedBy_returnsTrue_whenStartIsBeforeEnd() {
        assertTrue(specification.isSatisfiedBy(new DateTimeRange(START, END)));
    }

    @Test
    void isSatisfiedBy_returnsTrue_whenStartEqualsEnd() {
        assertTrue(specification.isSatisfiedBy(new DateTimeRange(START, START)));
    }

    @Test
    void isSatisfiedBy_returnsFalse_whenStartIsAfterEnd() {
        assertFalse(specification.isSatisfiedBy(new DateTimeRange(END, START)));
    }
}