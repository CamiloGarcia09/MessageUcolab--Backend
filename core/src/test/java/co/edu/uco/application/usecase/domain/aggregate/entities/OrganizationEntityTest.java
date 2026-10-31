package co.edu.uco.application.usecase.domain.aggregate.entities;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrganizationEntityTest {

    @Test
    void setId_acceptsValidIdentifier() {
        OrganizationEntity organization = new OrganizationEntity();
        UUID id = UUID.fromString("b38d6507-b7cd-4a2f-bbed-0e6c7f434fbd");

        organization.setId(id);

        assertThat(organization.getId()).isEqualTo(id);
    }

    @Test
    void setId_usesDefaultIdentifier_whenIdentifierIsMissing() {
        OrganizationEntity organization = new OrganizationEntity();

        organization.setId(null);

        assertThat(organization.getId())
                .isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000000"));
    }

    @Test
    void setName_trimsValue() {
        OrganizationEntity organization = new OrganizationEntity();

        organization.setName("  UCO  ");

        assertThat(organization.getName()).isEqualTo("UCO");
    }

    @Test
    void setName_usesEmptyValue_whenNameIsMissing() {
        OrganizationEntity organization = new OrganizationEntity();

        organization.setName(null);

        assertThat(organization.getName()).isEmpty();
    }
}
