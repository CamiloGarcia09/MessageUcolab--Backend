package co.edu.uco.application.usecase.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class MessageDomainTest {

    private static final UUID SAFE_DEFAULT_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Test
    void setters_storeValues() {
        MessageDomain domain = new MessageDomain();
        UUID id = UUID.randomUUID();
        domain.setId(id);
        domain.setCode("CODE");
        domain.setTitle("Title");
        domain.setContent("Content");
        domain.setApplication("app");

        assertThat(domain.getId()).isEqualTo(id);
        assertThat(domain.getCode()).isEqualTo("CODE");
        assertThat(domain.getTitle()).isEqualTo("Title");
        assertThat(domain.getContent()).isEqualTo("Content");
        assertThat(domain.getApplication()).isEqualTo("app");
    }

    @Test
    void setters_trimTextFields() {
        MessageDomain domain = new MessageDomain();
        domain.setCode("  CODE  ");
        domain.setTitle("  Title  ");
        domain.setContent("  Content  ");
        domain.setApplication("  app  ");

        assertThat(domain.getCode()).isEqualTo("CODE");
        assertThat(domain.getTitle()).isEqualTo("Title");
        assertThat(domain.getContent()).isEqualTo("Content");
        assertThat(domain.getApplication()).isEqualTo("app");
    }

    @Test
    void setId_usesDefaultWhenNull() {
        MessageDomain domain = new MessageDomain();
        domain.setId(null);

        assertThat(domain.getId())
                .isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000000"));
    }

    @Test
    void setType_storesType() {
        MessageDomain domain = new MessageDomain();
        MessageTypeDomain type = MessageTypeDomain.create(UUID.randomUUID(), "info");
        domain.setType(type);

        assertThat(domain.getType()).isSameAs(type);
    }

    @Test
    void setCategory_storesCategory() {
        MessageDomain domain = new MessageDomain();
        MessageCategoryDomain category = MessageCategoryDomain.create(UUID.randomUUID(), "general");
        domain.setCategory(category);

        assertThat(domain.getCategory()).isSameAs(category);
    }

    @Test
    void setStatus_storesStatus() {
        MessageDomain domain = new MessageDomain();
        MessageStatusDomain status = MessageStatusDomain.create(UUID.randomUUID(), "active");
        domain.setStatus(status);

        assertThat(domain.getStatus()).isSameAs(status);
    }

    @Test
    void setFunctionality_storesFunctionality() {
        MessageDomain domain = new MessageDomain();
        FunctionalityDomain functionality = FunctionalityDomain.create(UUID.randomUUID(), "payment");
        domain.setFunctionality(functionality);

        assertThat(domain.getFunctionality()).isSameAs(functionality);
    }

    @Test
    void setType_assignsSafeDefaultDomainObject_whenNull() {
        MessageDomain domain = new MessageDomain();

        domain.setType(null);

        assertAll(
                () -> assertThat(domain.getType()).isNotNull(),
                () -> assertThat(domain.getType().getId()).isEqualTo(SAFE_DEFAULT_ID),
                () -> assertThat(domain.getType().getName()).isEmpty());
    }

    @Test
    void setCategory_assignsSafeDefaultDomainObject_whenNull() {
        MessageDomain domain = new MessageDomain();

        domain.setCategory(null);

        assertAll(
                () -> assertThat(domain.getCategory()).isNotNull(),
                () -> assertThat(domain.getCategory().getId()).isEqualTo(SAFE_DEFAULT_ID),
                () -> assertThat(domain.getCategory().getName()).isEmpty());
    }

    @Test
    void setStatus_assignsSafeDefaultDomainObject_whenNull() {
        MessageDomain domain = new MessageDomain();

        domain.setStatus(null);

        assertAll(
                () -> assertThat(domain.getStatus()).isNotNull(),
                () -> assertThat(domain.getStatus().getId()).isEqualTo(SAFE_DEFAULT_ID),
                () -> assertThat(domain.getStatus().getName()).isEmpty());
    }

    @Test
    void setFunctionality_assignsSafeDefaultDomainObject_whenNull() {
        MessageDomain domain = new MessageDomain();

        domain.setFunctionality(null);

        assertAll(
                () -> assertThat(domain.getFunctionality()).isNotNull(),
                () -> assertThat(domain.getFunctionality().getId()).isEqualTo(SAFE_DEFAULT_ID),
                () -> assertThat(domain.getFunctionality().getName()).isEmpty());
    }

}
