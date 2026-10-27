package co.edu.uco.application.usecase;

import co.edu.uco.application.primaryports.dto.catalog.CatalogItemDTO;
import co.edu.uco.application.primaryports.dto.catalog.FunctionalityCatalogItemDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.entity.ApplicationData;
import co.edu.uco.application.secondaryports.entity.EnvironmentData;
import co.edu.uco.application.secondaryports.entity.EnvironmentTypeData;
import co.edu.uco.application.secondaryports.entity.FunctionalityData;
import co.edu.uco.application.secondaryports.entity.MessageCategoryData;
import co.edu.uco.application.secondaryports.entity.MessageEnvironmentStateData;
import co.edu.uco.application.secondaryports.entity.MessageTypeData;
import co.edu.uco.application.secondaryports.entity.StatusMessageData;
import co.edu.uco.application.secondaryports.repository.ApplicationCatalogRepository;
import co.edu.uco.application.secondaryports.repository.ApplicationRepository;
import co.edu.uco.application.secondaryports.repository.EnvironmentCatalogRepository;
import co.edu.uco.application.secondaryports.repository.FunctionalityCatalogRepository;
import co.edu.uco.application.secondaryports.repository.MessageCategoryCatalogRepository;
import co.edu.uco.application.secondaryports.repository.MessageEnvironmentStateCatalogRepository;
import co.edu.uco.application.secondaryports.repository.MessageStateCatalogRepository;
import co.edu.uco.application.secondaryports.repository.MessageTypeCatalogRepository;
import co.edu.uco.application.usecase.validator.session.SessionApplicationAccessCompositeValidator;
import co.edu.uco.crosscutting.exceptions.ForbiddenException;
import co.edu.uco.crosscutting.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindCatalogUseCaseTest {
    private static final String APP_ID = "c2d9f822-36d3-4580-a838-4f79f3c7cff8";
    private static final String OTHER_APP_ID = "72436a46-54d7-4cb5-a42c-af14f92d9416";
    private static final List<CatalogItemDTO> SESSION = List.of(CatalogItemDTO.create(APP_ID, "assigned"));

    @Mock private ApplicationCatalogRepository applicationCatalogRepository;
    @Mock private EnvironmentCatalogRepository environmentCatalogRepository;
    @Mock private FunctionalityCatalogRepository functionalityCatalogRepository;
    @Mock private MessageTypeCatalogRepository messageTypeCatalogRepository;
    @Mock private MessageCategoryCatalogRepository messageCategoryCatalogRepository;
    @Mock private MessageStateCatalogRepository messageStateCatalogRepository;
    @Mock private MessageEnvironmentStateCatalogRepository messageEnvironmentStateCatalogRepository;
    @Mock private ApplicationRepository applicationRepository;
    @Mock private CatalogPort catalogPort;

    private FindCatalogUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new FindCatalogUseCase(applicationCatalogRepository, environmentCatalogRepository,
                functionalityCatalogRepository, messageTypeCatalogRepository, messageCategoryCatalogRepository,
                messageStateCatalogRepository, messageEnvironmentStateCatalogRepository,
                new SessionApplicationAccessCompositeValidator(catalogPort, applicationRepository));
    }

    @Test
    void findApplications_returnsOnlyApplicationsAssignedInSessionAndPresentInCatalog() {
        ApplicationData assigned = ApplicationData.build(UUID.fromString(APP_ID), "Assigned app");
        ApplicationData other = ApplicationData.build(UUID.fromString(OTHER_APP_ID), "Other app");
        when(applicationCatalogRepository.findAll()).thenReturn(List.of(assigned, other));

        List<CatalogItemDTO> result = useCase.findApplications(SESSION);

        assertThat(result).extracting(CatalogItemDTO::getId, CatalogItemDTO::getName)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(APP_ID, "Assigned app"));
    }

    @Test
    void findApplications_returnsEmptyWithoutQueryWhenSessionIsNullOrEmpty() {
        assertThat(useCase.findApplications(null)).isEmpty();
        assertThat(useCase.findApplications(List.of())).isEmpty();
        verifyNoInteractions(applicationCatalogRepository);
    }

    @Test
    void findApplications_ignoresMalformedAssignedIds() {
        when(applicationCatalogRepository.findAll()).thenReturn(List.of(
                ApplicationData.build(UUID.fromString(APP_ID), "Registered")));

        assertThat(useCase.findApplications(List.of(CatalogItemDTO.create("not-uuid", "invalid"))))
                .isEmpty();
    }

    @Test
    void findEnvironmentsByApplication_returnsCatalogForAssignedRegisteredApplication() {
        when(applicationRepository.findById(APP_ID)).thenReturn(Optional.of(
                ApplicationData.build(UUID.fromString(APP_ID), "Assigned app")));
        when(environmentCatalogRepository.findAllByApplicationId(APP_ID)).thenReturn(List.of());

        assertThat(useCase.findEnvironmentsByApplication(APP_ID, SESSION)).isEmpty();
        verify(environmentCatalogRepository).findAllByApplicationId(APP_ID);
    }

    @Test
    void findEnvironmentsByApplication_rejectsUnassignedApplicationBeforeCatalogQuery() {
        when(catalogPort.getMessage("FUN_153")).thenReturn("Application not assigned");

        assertThatThrownBy(() -> useCase.findEnvironmentsByApplication(OTHER_APP_ID, SESSION))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(environmentCatalogRepository, applicationRepository);
    }

    @Test
    void findFunctionalitiesByApplication_rejectsAssignedButUnregisteredApplication() {
        when(applicationRepository.findById(APP_ID)).thenReturn(Optional.empty());
        when(catalogPort.getMessage("FUN_157")).thenReturn("Application not found");

        assertThatThrownBy(() -> useCase.findFunctionalitiesByApplication(APP_ID, SESSION))
                .isInstanceOf(NotFoundException.class);
        verifyNoInteractions(functionalityCatalogRepository);
    }

    @Test
    void findEnvironmentsByApplication_mapsIdentifierAndNameForAssignedApplication() {
        UUID environmentId = UUID.randomUUID();
        when(applicationRepository.findById(APP_ID)).thenReturn(Optional.of(
                ApplicationData.build(UUID.fromString(APP_ID), "Assigned app")));
        when(environmentCatalogRepository.findAllByApplicationId(APP_ID)).thenReturn(List.of(
                new EnvironmentData(environmentId, ApplicationData.build(),
                        new EnvironmentTypeData(UUID.randomUUID(), "Production"))));

        assertThat(useCase.findEnvironmentsByApplication(APP_ID, SESSION))
                .extracting(CatalogItemDTO::getId, CatalogItemDTO::getName)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(environmentId.toString(), "Production"));
    }

    @Test
    void findFunctionalitiesByApplication_mapsIdentifierAndNameForAssignedRegisteredApplication() {
        UUID functionalityId = UUID.randomUUID();
        UUID stateId = UUID.randomUUID();
        when(applicationRepository.findById(APP_ID)).thenReturn(Optional.of(
                ApplicationData.build(UUID.fromString(APP_ID), "Assigned app")));
        FunctionalityData active = new FunctionalityData(functionalityId, "Search", ApplicationData.build());
        active.setStateId(stateId);
        active.setState("Active");
        FunctionalityData inactive = new FunctionalityData(UUID.randomUUID(), "Legacy", ApplicationData.build());
        inactive.setStateId(UUID.randomUUID());
        inactive.setState("Inactive");
        when(functionalityCatalogRepository.findAllByApplicationId(APP_ID)).thenReturn(List.of(active, inactive));

        List<CatalogItemDTO> result = useCase.findFunctionalitiesByApplication(APP_ID, SESSION);

        assertThat(result).containsExactly(
                new FunctionalityCatalogItemDTO(functionalityId.toString(), "Search", stateId.toString(), "Active", true),
                new FunctionalityCatalogItemDTO(inactive.getId().toString(), "Legacy",
                        inactive.getStateId().toString(), "Inactive", false));
        assertThat(result).allSatisfy(item -> assertThat(item).isInstanceOf(FunctionalityCatalogItemDTO.class));
    }

    @Test
    void findMessageTypes_mapsIdentifierAndName() {
        UUID id = UUID.randomUUID();
        when(messageTypeCatalogRepository.findAll()).thenReturn(List.of(new MessageTypeData(id, "TEXT")));

        assertThat(useCase.findMessageTypes()).extracting(CatalogItemDTO::getId, CatalogItemDTO::getName)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(id.toString(), "TEXT"));
    }

    @Test
    void findMessageCategories_mapsIdentifierAndName() {
        UUID id = UUID.randomUUID();
        when(messageCategoryCatalogRepository.findAll()).thenReturn(List.of(new MessageCategoryData(id, "GENERAL")));

        assertThat(useCase.findMessageCategories()).extracting(CatalogItemDTO::getId, CatalogItemDTO::getName)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(id.toString(), "GENERAL"));
    }

    @Test
    void findMessageStates_mapsIdentifierAndName() {
        UUID id = UUID.randomUUID();
        when(messageStateCatalogRepository.findAll()).thenReturn(List.of(new StatusMessageData(id, "ACTIVE")));

        assertThat(useCase.findMessageStates()).extracting(CatalogItemDTO::getId, CatalogItemDTO::getName)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(id.toString(), "ACTIVE"));
    }

    @Test
    void findMessageEnvironmentStates_mapsIdentifierAndName() {
        UUID id = UUID.randomUUID();
        when(messageEnvironmentStateCatalogRepository.findAll())
                .thenReturn(List.of(new MessageEnvironmentStateData(id, "PENDING")));

        assertThat(useCase.findMessageEnvironmentStates())
                .extracting(CatalogItemDTO::getId, CatalogItemDTO::getName)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(id.toString(), "PENDING"));
    }
}
