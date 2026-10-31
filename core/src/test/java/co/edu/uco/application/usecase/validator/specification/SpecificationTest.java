package co.edu.uco.application.usecase.validator.specification;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpecificationTest {

    @Test
    void and_returnsTrue_whenBothAreSatisfied() {
        Specification<Integer> positive = candidate -> candidate > 0;
        Specification<Integer> small = candidate -> candidate < 10;

        assertTrue(positive.and(small).isSatisfiedBy(5));
    }

    @Test
    void and_returnsFalse_whenFirstIsNotSatisfied() {
        Specification<Integer> positive = candidate -> candidate > 0;
        Specification<Integer> small = candidate -> candidate < 10;

        assertFalse(positive.and(small).isSatisfiedBy(-5));
    }

    @Test
    void and_returnsFalse_whenSecondIsNotSatisfied() {
        Specification<Integer> positive = candidate -> candidate > 0;
        Specification<Integer> small = candidate -> candidate < 10;

        assertFalse(positive.and(small).isSatisfiedBy(20));
    }

    @Test
    void or_returnsTrue_whenEitherIsSatisfied() {
        Specification<String> shortText = candidate -> candidate.length() <= 3;
        Specification<String> longText = candidate -> candidate.length() >= 10;

        assertTrue(shortText.or(longText).isSatisfiedBy("ab"));
        assertTrue(shortText.or(longText).isSatisfiedBy("abcdefghij"));
    }

    @Test
    void or_returnsFalse_whenNeitherIsSatisfied() {
        Specification<String> shortText = candidate -> candidate.length() <= 3;
        Specification<String> longText = candidate -> candidate.length() >= 10;

        assertFalse(shortText.or(longText).isSatisfiedBy("abcdef"));
    }

    @Test
    void not_invertsResult() {
        Specification<Integer> positive = candidate -> candidate > 0;

        assertTrue(positive.not().isSatisfiedBy(-5));
        assertFalse(positive.not().isSatisfiedBy(5));
    }
}