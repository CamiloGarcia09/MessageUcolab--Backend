package co.edu.uco.application.usecase.validator.specification.impl;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DateTimeRangeTest {

    private static final LocalDateTime START = LocalDateTime.of(2025, 1, 1, 0, 0);
    private static final LocalDateTime END = LocalDateTime.of(2025, 12, 31, 23, 59);

    @Test
    void startIsAfterEnd_returnsFalse_whenStartIsBeforeEnd() {
        DateTimeRange range = new DateTimeRange(START, END);

        assertFalse(range.startIsAfterEnd());
    }

    @Test
    void startIsAfterEnd_returnsFalse_whenStartEqualsEnd() {
        DateTimeRange range = new DateTimeRange(START, START);

        assertFalse(range.startIsAfterEnd());
    }

    @Test
    void startIsAfterEnd_returnsTrue_whenStartIsAfterEnd() {
        DateTimeRange range = new DateTimeRange(END, START);

        assertTrue(range.startIsAfterEnd());
    }
}