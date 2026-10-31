package co.edu.uco.application.usecase.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MessageCodeDomainTest {

    @Test
    void parameterizedConstructor_trimsCode() {
        MessageCodeDomain domain = new MessageCodeDomain("  CODE001  ");

        assertThat(domain.getCode()).isEqualTo("CODE001");
    }

    @Test
    void emptyConstructor_leavesCodeNull() {
        MessageCodeDomain domain = new MessageCodeDomain();

        assertThat(domain.getCode()).isNull();
    }

    @Test
    void create_returnsInstanceWithTrimmedCode() {
        MessageCodeDomain domain = MessageCodeDomain.create("  CODE002  ");

        assertThat(domain.getCode()).isEqualTo("CODE002");
    }

    @Test
    void setCode_trimsValue() {
        MessageCodeDomain domain = new MessageCodeDomain();

        domain.setCode("  CODE003  ");

        assertThat(domain.getCode()).isEqualTo("CODE003");
    }

    @Test
    void setCode_acceptsValidCode() {
        MessageCodeDomain domain = new MessageCodeDomain();

        domain.setCode("CODE004");

        assertThat(domain.getCode()).isEqualTo("CODE004");
    }
}
