package co.edu.uco.application.primaryports.dto.organization;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CreateOrganizationDTOTest {

    @Test
    void defaultConstructor_initializesNameToEmpty() {
        CreateOrganizationDTO dto = new CreateOrganizationDTO();

        assertThat(dto.getName()).isEmpty();
    }

    @Test
    void setName_trimsProvidedValue() {
        CreateOrganizationDTO dto = new CreateOrganizationDTO();

        dto.setName("  Universidad de Córdoba  ");

        assertThat(dto.getName()).isEqualTo("Universidad de Córdoba");
    }

    @Test
    void setName_convertsNullToEmpty() {
        CreateOrganizationDTO dto = new CreateOrganizationDTO();

        dto.setName(null);

        assertThat(dto.getName()).isEmpty();
    }

    @Test
    void builder_createsDtoWithProvidedName() {
        CreateOrganizationDTO dto = CreateOrganizationDTO.builder()
                .name("UCO")
                .build();

        assertThat(dto.getName()).isEqualTo("UCO");
    }
}
