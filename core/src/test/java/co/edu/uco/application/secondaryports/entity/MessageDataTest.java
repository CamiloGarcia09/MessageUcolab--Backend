package co.edu.uco.application.secondaryports.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MessageDataTest {

    @Test
    void build_returnsDefaultInstance() {
        MessageData data = MessageData.build();

        assertThat(data.getId()).isNotNull();
        assertThat(data.getCode()).isEmpty();
        assertThat(data.getTitle()).isEmpty();
        assertThat(data.getContent()).isEmpty();
        assertThat(data.getApplication()).isEmpty();
        assertThat(data.getType()).isNotNull();
        assertThat(data.getCategory()).isNotNull();
        assertThat(data.getStatus()).isNotNull();
        assertThat(data.getFunctionality()).isNotNull();
    }

    @Test
    void parameterizedConstructor_storesValues() {
        UUID id = UUID.randomUUID();
        MessageTypeData type = MessageTypeData.build();
        MessageCategoryData category = MessageCategoryData.build();
        FunctionalityData functionality = FunctionalityData.build();
        MessageData data = new MessageData(id, " code ", " title ", " content ", type, category, " app ", functionality);

        assertThat(data.getId()).isEqualTo(id);
        assertThat(data.getCode()).isEqualTo("code");
        assertThat(data.getTitle()).isEqualTo("title");
        assertThat(data.getContent()).isEqualTo("content");
        assertThat(data.getApplication()).isEqualTo("app");
        assertThat(data.getType()).isSameAs(type);
        assertThat(data.getCategory()).isSameAs(category);
        assertThat(data.getStatus()).isNotNull();
        assertThat(data.getFunctionality()).isSameAs(functionality);
    }

    @Test
    void setters_applyDefaultsWhenNull() {
        MessageData data = new MessageData();

        data.setId(null);
        data.setType(null);
        data.setCategory(null);
        data.setStatus(null);
        data.setFunctionality(null);

        assertThat(data.getId())
                .isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000000"));
        assertThat(data.getType()).isNotNull();
        assertThat(data.getCategory()).isNotNull();
        assertThat(data.getStatus()).isNotNull();
        assertThat(data.getFunctionality()).isNotNull();
    }

    @Test
    void setters_trimTextFields() {
        MessageData data = new MessageData();

        data.setCode("  a  ");
        data.setTitle("  b  ");
        data.setContent("  c  ");
        data.setApplication("  d  ");

        assertThat(data.getCode()).isEqualTo("a");
        assertThat(data.getTitle()).isEqualTo("b");
        assertThat(data.getContent()).isEqualTo("c");
        assertThat(data.getApplication()).isEqualTo("d");
    }
}