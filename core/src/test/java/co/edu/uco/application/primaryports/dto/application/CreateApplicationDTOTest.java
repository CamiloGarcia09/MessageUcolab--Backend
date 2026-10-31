package co.edu.uco.application.primaryports.dto.application;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CreateApplicationDTOTest {

    @Test
    void defaultConstructor_initializesAllFieldsToEmpty() {
        CreateApplicationDTO dto = new CreateApplicationDTO();

        assertAll(
                () -> assertEquals("", dto.getName()),
                () -> assertEquals("", dto.getOrganizationId()),
                () -> assertEquals("", dto.getLanguageId()),
                () -> assertEquals("", dto.getStateId()));
    }

    @Test
    void setters_trimValues() {
        CreateApplicationDTO dto = new CreateApplicationDTO();
        dto.setName("  App  ");
        dto.setOrganizationId("  org-id  ");
        dto.setLanguageId("  lang  ");
        dto.setStateId("  state  ");

        assertAll(
                () -> assertEquals("App", dto.getName()),
                () -> assertEquals("org-id", dto.getOrganizationId()),
                () -> assertEquals("lang", dto.getLanguageId()),
                () -> assertEquals("state", dto.getStateId()));
    }

    @Test
    void builder_createsDtoWithValues() {
        CreateApplicationDTO dto = CreateApplicationDTO.builder()
                .name("App")
                .organizationId("org-id")
                .languageId("lang")
                .stateId("state")
                .build();

        assertAll(
                () -> assertEquals("App", dto.getName()),
                () -> assertEquals("org-id", dto.getOrganizationId()),
                () -> assertEquals("lang", dto.getLanguageId()),
                () -> assertEquals("state", dto.getStateId()));
    }
}
