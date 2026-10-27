package co.edu.uco.application.primaryports.dto.message;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResolvedMessageDTOTest {
    @Test
    void constructor_trimsConsumerFields() {
        var dto = new ResolvedMessageDTO(" CODE ", " Title ", " Content ");

        assertThat(dto).extracting("code", "title", "content").containsExactly("CODE", "Title", "Content");
    }

    @Test
    void constructor_handlesNullFieldsAsEmptyStrings() {
        var dto = new ResolvedMessageDTO(null, null, null);

        assertThat(dto).extracting("code", "title", "content").containsExactly("", "", "");
    }

    @Test
    void toString_displaysOnlyTheNormalizedConsumerProperties() {
        var dto = new ResolvedMessageDTO(" CODE ", " Title ", " Body ");

        assertThat(dto).hasToString("ResolvedMessageDTO(code=CODE, title=Title, content=Body, type=, category=, functionality=, createdAt=null, updatedAt=null)");
    }
}

