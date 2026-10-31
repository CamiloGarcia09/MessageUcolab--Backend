package co.edu.uco.application.primaryports.dto.functionality;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CreateFunctionalityDTOTest {

    @Test
    void defaultConstructor_initializesAllFieldsToEmpty() {
        CreateFunctionalityDTO dto = new CreateFunctionalityDTO();

        assertAll(
                () -> assertEquals("", dto.getName()),
                () -> assertEquals("", dto.getApplicationId()),
                () -> assertEquals("", dto.getStateId()));
    }

    @Test
    void setters_trimValues() {
        CreateFunctionalityDTO dto = new CreateFunctionalityDTO();
        dto.setName("  Search  ");
        dto.setApplicationId("  app-1  ");
        dto.setStateId("  state  ");

        assertAll(
                () -> assertEquals("Search", dto.getName()),
                () -> assertEquals("app-1", dto.getApplicationId()),
                () -> assertEquals("state", dto.getStateId()));
    }

    @Test
    void builder_createsDtoWithValues() {
        CreateFunctionalityDTO dto = CreateFunctionalityDTO.builder()
                .name("Search")
                .applicationId("app-1")
                .stateId("state")
                .build();

        assertAll(
                () -> assertEquals("Search", dto.getName()),
                () -> assertEquals("app-1", dto.getApplicationId()),
                () -> assertEquals("state", dto.getStateId()));
    }
}
