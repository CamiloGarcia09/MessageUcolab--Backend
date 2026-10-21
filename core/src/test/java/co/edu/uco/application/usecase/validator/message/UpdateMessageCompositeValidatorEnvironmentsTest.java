package co.edu.uco.application.usecase.validator.message;

import co.edu.uco.application.primaryports.dto.message.UpdateMessageDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.entity.ApplicationData;
import co.edu.uco.application.secondaryports.entity.FunctionalityData;
import co.edu.uco.application.secondaryports.repository.FunctionalityCatalogRepository;
import co.edu.uco.application.secondaryports.repository.RecordExistsCatalogPort;
import co.edu.uco.application.usecase.validator.message.environment.MessageEnvironmentSelectionCompositeValidator;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * La edicion solo valida ambientes cuando el cliente los envia; sin ellos conserva los actuales.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UpdateMessageCompositeValidatorEnvironmentsTest {

    private static final UUID APPLICATION_ID = UUID.fromString("c2d9f822-36d3-4580-a838-4f79f3c7cff8");
    private static final UUID FUNCTIONALITY_ID = UUID.fromString("5a8e2c74-1d39-4f6b-b7c2-0e9a3d5f8164");
    private static final String DEVELOP_ID = "7b3e5d91-a2c8-46f0-9d14-5e7a1b6c3f82";

    @Mock
    private CatalogPort catalogPort;
    @Mock
    private RecordExistsCatalogPort recordExistsCatalogPort;
    @Mock
    private FunctionalityCatalogRepository functionalityCatalogRepository;
    @Mock
    private MessageEnvironmentSelectionCompositeValidator environmentSelectionValidator;

    private UpdateMessageCompositeValidator validator;

    @BeforeEach
    void setUp() {
        when(recordExistsCatalogPort.exists(any(), anyString())).thenReturn(true);
        FunctionalityData functionality = FunctionalityData.build();
        functionality.setId(FUNCTIONALITY_ID);
        functionality.setApplication(ApplicationData.build(APPLICATION_ID, "App"));
        when(functionalityCatalogRepository.findAllByApplicationId(APPLICATION_ID.toString()))
                .thenReturn(List.of(functionality));
        validator = new UpdateMessageCompositeValidator(catalogPort, recordExistsCatalogPort,
                functionalityCatalogRepository, environmentSelectionValidator);
    }

    @Test
    void validate_skipsEnvironmentSelection_whenDtoHasNoEnvironments() {
        assertThatCode(() -> validator.validate(dto(null), APPLICATION_ID.toString())).doesNotThrowAnyException();

        verifyNoInteractions(environmentSelectionValidator);
    }

    @Test
    void validate_delegatesEnvironmentSelection_whenDtoHasEnvironments() {
        validator.validate(dto(List.of(DEVELOP_ID)), APPLICATION_ID.toString());

        verify(environmentSelectionValidator).validate(List.of(DEVELOP_ID), APPLICATION_ID.toString());
    }

    @Test
    void validate_stopsBeforeFunctionalityCheck_whenEnvironmentSelectionFails() {
        doThrow(BusinessRuleException.buildUserException("Select an environment"))
                .when(environmentSelectionValidator).validate(List.of(), APPLICATION_ID.toString());

        assertThatThrownBy(() -> validator.validate(dto(List.of()), APPLICATION_ID.toString()))
                .isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(functionalityCatalogRepository);
    }

    private static UpdateMessageDTO dto(List<String> environmentIds) {
        UpdateMessageDTO dto = UpdateMessageDTO.builder()
                .title("Updated title")
                .content("Updated content for the message")
                .typeId("123e4567-e89b-12d3-a456-426614175010")
                .categoryId("123e4567-e89b-12d3-a456-426614175011")
                .statusId("123e4567-e89b-12d3-a456-426614175012")
                .functionalityId(FUNCTIONALITY_ID.toString())
                .build();
        dto.setEnvironmentIds(environmentIds);
        return dto;
    }
}
