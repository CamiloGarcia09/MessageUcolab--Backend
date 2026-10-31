package co.edu.uco.application.primaryports.dto.message;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CreateMessageDTOTest {

    @Test
    void defaultConstructor_initializesAllFieldsToEmpty() {
        CreateMessageDTO dto = new CreateMessageDTO();

        assertAll(
                () -> assertEquals("", dto.getCode()),
                () -> assertEquals("", dto.getTitle()),
                () -> assertEquals("", dto.getContent()),
                () -> assertEquals("", dto.getTypeId()),
                () -> assertEquals("", dto.getCategoryId()),
                () -> assertEquals("", dto.getStatusId()),
                () -> assertEquals("", dto.getFunctionalityId()));
    }

    @Test
    void setters_trimValues() {
        CreateMessageDTO dto = new CreateMessageDTO();
        dto.setCode("  MSG-001  ");
        dto.setTitle("  A valid title  ");
        dto.setContent("  A valid content  ");
        dto.setTypeId("  type  ");
        dto.setCategoryId("  cat  ");
        dto.setStatusId("  status  ");
        dto.setFunctionalityId("  func-1  ");

        assertAll(
                () -> assertEquals("MSG-001", dto.getCode()),
                () -> assertEquals("A valid title", dto.getTitle()),
                () -> assertEquals("A valid content", dto.getContent()),
                () -> assertEquals("type", dto.getTypeId()),
                () -> assertEquals("cat", dto.getCategoryId()),
                () -> assertEquals("status", dto.getStatusId()),
                () -> assertEquals("func-1", dto.getFunctionalityId()));
    }

    @Test
    void builder_createsDtoWithValues() {
        CreateMessageDTO dto = CreateMessageDTO.builder()
                .code("MSG-001")
                .title("A valid title")
                .content("A valid content")
                .typeId("type")
                .categoryId("cat")
                .statusId("status")
                .functionalityId("func-1")
                .build();

        assertAll(
                () -> assertEquals("MSG-001", dto.getCode()),
                () -> assertEquals("A valid title", dto.getTitle()),
                () -> assertEquals("A valid content", dto.getContent()),
                () -> assertEquals("type", dto.getTypeId()),
                () -> assertEquals("cat", dto.getCategoryId()),
                () -> assertEquals("status", dto.getStatusId()),
                () -> assertEquals("func-1", dto.getFunctionalityId()));
    }

    @Test
    void setters_normalizeNullToEmpty() {
        CreateMessageDTO dto = CreateMessageDTO.builder().code("MSG-001").build();

        dto.setCode(null);

        assertEquals("", dto.getCode());
    }
}
