package co.edu.uco.application.usecase.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class FunctionalityDomainTest {

    @Test
    void create_storesValues() {
        UUID id = UUID.randomUUID();

        FunctionalityDomain domain = FunctionalityDomain.create(id, " name ");

        assertSoftly(softly -> {
            softly.assertThat(domain.getId()).isEqualTo(id);
            softly.assertThat(domain.getName()).isEqualTo("name");
        });
    }

    @Test
    void setId_usesDefaultWhenNull() {
        FunctionalityDomain domain = FunctionalityDomain.create(null, "name");

        assertThat(domain.getId())
                .isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000000"));
    }

    @Test
    void setName_usesEmptyValue_whenNameIsMissing() {
        FunctionalityDomain domain = FunctionalityDomain.create(UUID.randomUUID(), null);

        assertThat(domain.getName()).isEmpty();
    }
}
