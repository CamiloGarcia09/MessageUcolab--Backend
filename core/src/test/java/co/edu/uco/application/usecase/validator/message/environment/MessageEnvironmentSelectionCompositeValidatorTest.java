package co.edu.uco.application.usecase.validator.message.environment;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.entity.ApplicationData;
import co.edu.uco.application.secondaryports.entity.EnvironmentData;
import co.edu.uco.application.secondaryports.entity.EnvironmentTypeData;
import co.edu.uco.application.secondaryports.repository.EnvironmentRepository;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import co.edu.uco.crosscutting.exceptions.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageEnvironmentSelectionCompositeValidatorTest {

    private static final String APPLICATION_ID = "c2d9f822-36d3-4580-a838-4f79f3c7cff8";
    private static final String OTHER_APPLICATION_ID = "72436a46-54d7-4cb5-a42c-af14f92d9416";
    private static final String DEVELOP_ID = "7b3e5d91-a2c8-46f0-9d14-5e7a1b6c3f82";
    private static final String PRODUCTION_ID = "de1b8223-1e9c-4807-beec-b2c1cff44e73";

    @Mock
    private CatalogPort catalogPort;
    @Mock
    private EnvironmentRepository environmentRepository;

    private MessageEnvironmentSelectionCompositeValidator validator;

    @BeforeEach
    void setUp() {
        validator = new MessageEnvironmentSelectionCompositeValidator(catalogPort, environmentRepository);
    }

    @Test
    void validate_acceptsDistinctEnvironmentsOfTheApplication() {
        when(environmentRepository.findById(DEVELOP_ID)).thenReturn(Optional.of(environment(DEVELOP_ID, APPLICATION_ID)));
        when(environmentRepository.findById(PRODUCTION_ID))
                .thenReturn(Optional.of(environment(PRODUCTION_ID, APPLICATION_ID)));

        assertThatCode(() -> validator.validate(List.of(DEVELOP_ID, PRODUCTION_ID), APPLICATION_ID))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_shortCircuitsBeforeRepository_whenSelectionIsEmpty() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_212.getCode())).thenReturn("Select an environment");

        assertThatThrownBy(() -> validator.validate(List.of(), APPLICATION_ID))
                .isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(environmentRepository);
    }

    @Test
    void validate_shortCircuitsBeforeRepository_whenIdIsNotUuid() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_038.getCode())).thenReturn("Invalid id");

        assertThatThrownBy(() -> validator.validate(List.of("not-a-uuid"), APPLICATION_ID))
                .isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(environmentRepository);
    }

    @Test
    void validate_shortCircuitsBeforeRepository_whenEnvironmentIsRepeated() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_213.getCode())).thenReturn("Repeated");

        assertThatThrownBy(() -> validator.validate(List.of(DEVELOP_ID, DEVELOP_ID), APPLICATION_ID))
                .isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(environmentRepository);
    }

    @Test
    void validate_rejectsEnvironmentOfAnotherApplication() {
        when(environmentRepository.findById(DEVELOP_ID))
                .thenReturn(Optional.of(environment(DEVELOP_ID, OTHER_APPLICATION_ID)));
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_036.getCode())).thenReturn("Foreign environment");

        assertThatThrownBy(() -> validator.validate(List.of(DEVELOP_ID), APPLICATION_ID))
                .isInstanceOf(ForbiddenException.class);
    }

    private static EnvironmentData environment(String environmentId, String applicationId) {
        return new EnvironmentData(UUID.fromString(environmentId),
                ApplicationData.build(UUID.fromString(applicationId), "App"),
                new EnvironmentTypeData(UUID.randomUUID(), "Develop"));
    }
}
