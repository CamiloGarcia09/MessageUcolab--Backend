package co.edu.uco.application.primaryports.dto.message;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MessageCodeDTOTest {

    @Test
    void create_keepsProvidedCode() {
        MessageCodeDTO dto = MessageCodeDTO.create("CODE-001");

        assertThat(dto.getCode()).isEqualTo("CODE-001");
    }

    @Test
    void create_normalizesNullToEmpty() {
        MessageCodeDTO dto = MessageCodeDTO.create(null);

        assertThat(dto.getCode()).isEmpty();
    }

    @Test
    void setCode_updatesValue() {
        MessageCodeDTO dto = MessageCodeDTO.create("A");
        dto.setCode("NEW-CODE");

        assertThat(dto.getCode()).isEqualTo("NEW-CODE");
    }
}