package co.edu.uco.application.secondaryports.entity;

import co.edu.uco.application.usecase.domain.aggregate.entities.OrganizationEntity;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static co.edu.uco.crosscutting.helpers.UtilUUID.DEFAULT_UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class ApplicationDataTest {

    @Test
    void constructorWithOrganization_preservesApplicationAndOrganizationData() {
        UUID applicationId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        OrganizationEntity organization = organization(organizationId, "UCO");

        ApplicationData application = new ApplicationData(applicationId, "  Messages  ", organization);

        assertSoftly(softly -> {
            softly.assertThat(application.getId()).isEqualTo(applicationId);
            softly.assertThat(application.getName()).isEqualTo("Messages");
            softly.assertThat(application.getOrganization()).isSameAs(organization);
            softly.assertThat(application.getOrganization().getId()).isEqualTo(organizationId);
        });
    }

    @Test
    void legacyConstructor_remainsCompatibleAndUsesDefaultOrganization() {
        UUID applicationId = UUID.randomUUID();

        ApplicationData application = new ApplicationData(applicationId, "Legacy");

        assertDefaultOrganization(application, applicationId, "Legacy");
    }

    @Test
    void legacyBuild_remainsCompatibleAndUsesDefaultOrganization() {
        UUID applicationId = UUID.randomUUID();

        ApplicationData application = ApplicationData.build(applicationId, "Legacy build");

        assertDefaultOrganization(application, applicationId, "Legacy build");
    }

    @Test
    void defaultBuild_generatesApplicationIdAndDefaultOrganization() {
        ApplicationData application = ApplicationData.build();

        assertSoftly(softly -> {
            softly.assertThat(application.getId()).isNotNull().isNotEqualTo(DEFAULT_UUID);
            softly.assertThat(application.getName()).isEmpty();
            softly.assertThat(application.getOrganization().getId()).isEqualTo(DEFAULT_UUID);
            softly.assertThat(application.getOrganization().getName()).isEmpty();
        });
    }

    @Test
    void buildWithOrganization_preservesProvidedOrganization() {
        UUID applicationId = UUID.randomUUID();
        OrganizationEntity organization = organization(UUID.randomUUID(), "UCO");

        ApplicationData application = ApplicationData.build(applicationId, "App", organization);

        assertThat(application.getOrganization()).isSameAs(organization);
    }

    private void assertDefaultOrganization(ApplicationData application, UUID expectedId, String expectedName) {
        assertSoftly(softly -> {
            softly.assertThat(application.getId()).isEqualTo(expectedId);
            softly.assertThat(application.getName()).isEqualTo(expectedName);
            softly.assertThat(application.getOrganization()).isNotNull();
            softly.assertThat(application.getOrganization().getId()).isEqualTo(DEFAULT_UUID);
            softly.assertThat(application.getOrganization().getName()).isEmpty();
        });
    }

    private OrganizationEntity organization(UUID id, String name) {
        OrganizationEntity organization = new OrganizationEntity();
        organization.setId(id);
        organization.setName(name);
        return organization;
    }
}
