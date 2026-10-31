package co.edu.uco.application.usecase.validator.specification.impl;

import co.edu.uco.application.secondaryports.entity.FunctionalityData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.UUID;

import static co.edu.uco.crosscutting.helpers.UtilUUID.DEFAULT_UUID;
import static org.assertj.core.api.Assertions.assertThat;

class ActiveFunctionalitySpecificationTest {

    private final ActiveFunctionalitySpecification specification = new ActiveFunctionalitySpecification();

    @Test
    void isSatisfiedBy_returnsTrue_whenStateIsActiveRegardlessOfCase() {
        FunctionalityData functionality = functionality(UUID.randomUUID(), "aCtIvE");

        assertThat(specification.isSatisfiedBy(functionality)).isTrue();
    }

    @ParameterizedTest(name = "state {1} is not active")
    @CsvSource({"Inactive", "Pending", "''"})
    void isSatisfiedBy_returnsFalse_whenStateIsNotActive(String state) {
        FunctionalityData functionality = functionality(UUID.randomUUID(), state);

        assertThat(specification.isSatisfiedBy(functionality)).isFalse();
    }

    @Test
    void isSatisfiedBy_returnsFalse_whenStateReferenceIsDefaultOrCandidateIsNull() {
        FunctionalityData withoutReference = functionality(DEFAULT_UUID, "Active");

        assertThat(specification.isSatisfiedBy(withoutReference)).isFalse();
        assertThat(specification.isSatisfiedBy(null)).isFalse();
    }

    private static FunctionalityData functionality(UUID stateId, String state) {
        FunctionalityData functionality = FunctionalityData.build();
        functionality.setStateId(stateId);
        functionality.setState(state);
        return functionality;
    }
}
