package co.edu.uco.application.usecase.validator.specification;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpecificationsTest {

    @Test
    void field_returnsFalse_whenCandidateIsNull() {
        Specification<String> lengthSpec = Specifications.field(String::length, length -> length >= 3);

        assertFalse(lengthSpec.isSatisfiedBy(null));
    }

    @Test
    void field_delegatesToNestedSpecification() {
        Specification<String> lengthSpec = Specifications.field(String::length, length -> length >= 3);

        assertTrue(lengthSpec.isSatisfiedBy("abcd"));
        assertFalse(lengthSpec.isSatisfiedBy("ab"));
    }
}