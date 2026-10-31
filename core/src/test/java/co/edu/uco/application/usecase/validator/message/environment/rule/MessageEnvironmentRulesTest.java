package co.edu.uco.application.usecase.validator.message.environment.rule;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.entity.ApplicationData;
import co.edu.uco.application.secondaryports.entity.EnvironmentData;
import co.edu.uco.application.secondaryports.entity.EnvironmentTypeData;
import co.edu.uco.application.secondaryports.repository.EnvironmentRepository;
import co.edu.uco.application.usecase.validator.message.environment.MessageEnvironmentSelectionContext;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import co.edu.uco.crosscutting.exceptions.ForbiddenException;
import co.edu.uco.crosscutting.exceptions.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Reglas de los ambientes donde se crea o se enlaza un mensaje: caso valido, limite y error.
 */
@ExtendWith(MockitoExtension.class)
class MessageEnvironmentRulesTest {

    private static final String APPLICATION_ID = "c2d9f822-36d3-4580-a838-4f79f3c7cff8";
    private static final String OTHER_APPLICATION_ID = "72436a46-54d7-4cb5-a42c-af14f92d9416";
    private static final String DEVELOP_ID = "7b3e5d91-a2c8-46f0-9d14-5e7a1b6c3f82";
    private static final String PRODUCTION_ID = "de1b8223-1e9c-4807-beec-b2c1cff44e73";

    @Mock
    private CatalogPort catalogPort;
    @Mock
    private EnvironmentRepository environmentRepository;

    @Test
    void required_doesNotThrow_whenAtLeastOneEnvironmentIsSelected() {
        assertThatCode(() -> new MessageEnvironmentsRequiredRule(catalogPort).validate(context(DEVELOP_ID)))
                .doesNotThrowAnyException();
    }

    @Test
    void required_throwsUsingFun212_whenSelectionIsEmpty() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_212.getCode())).thenReturn("Select an environment");

        assertThatThrownBy(() -> new MessageEnvironmentsRequiredRule(catalogPort).validate(context()))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo("Select an environment"));
    }

    @Test
    void required_throws_whenSelectionOrContextIsNull() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_212.getCode())).thenReturn("Select an environment");
        MessageEnvironmentsRequiredRule rule = new MessageEnvironmentsRequiredRule(catalogPort);

        assertThatThrownBy(() -> rule.validate(new MessageEnvironmentSelectionContext(null, APPLICATION_ID)))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> rule.validate(null)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void uuid_doesNotThrow_whenEveryIdIsValid() {
        assertThatCode(() -> new MessageEnvironmentsUuidRule(catalogPort).validate(context(DEVELOP_ID, PRODUCTION_ID)))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "id=[{0}]")
    @ValueSource(strings = {"", "not-a-uuid", "00000000-0000-0000-0000-000000000000"})
    void uuid_throwsUsingFun038_whenAnyIdIsNotAUsableUuid(String invalid) {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_038.getCode())).thenReturn("Invalid id");

        assertThatThrownBy(() -> new MessageEnvironmentsUuidRule(catalogPort).validate(context(DEVELOP_ID, invalid)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void duplicated_doesNotThrow_whenIdsAreDistinct() {
        assertThatCode(() -> new MessageEnvironmentsDuplicatedRule(catalogPort)
                .validate(context(DEVELOP_ID, PRODUCTION_ID))).doesNotThrowAnyException();
    }

    @Test
    void duplicated_throwsUsingFun213_whenSameEnvironmentIsRepeatedIgnoringCase() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_213.getCode())).thenReturn("Repeated environment");

        assertThatThrownBy(() -> new MessageEnvironmentsDuplicatedRule(catalogPort)
                .validate(context(DEVELOP_ID, DEVELOP_ID.toUpperCase())))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo("Repeated environment"));
    }

    @Test
    void exist_doesNotThrow_whenEveryEnvironmentExists() {
        when(environmentRepository.findById(DEVELOP_ID)).thenReturn(Optional.of(environment(DEVELOP_ID, APPLICATION_ID)));
        when(environmentRepository.findById(PRODUCTION_ID))
                .thenReturn(Optional.of(environment(PRODUCTION_ID, APPLICATION_ID)));

        assertThatCode(() -> new MessageEnvironmentsExistRule(catalogPort, environmentRepository)
                .validate(context(DEVELOP_ID, PRODUCTION_ID))).doesNotThrowAnyException();
    }

    @Test
    void exist_throwsNotFoundUsingFun035_whenAnyEnvironmentIsMissing() {
        when(environmentRepository.findById(DEVELOP_ID)).thenReturn(Optional.of(environment(DEVELOP_ID, APPLICATION_ID)));
        when(environmentRepository.findById(PRODUCTION_ID)).thenReturn(Optional.empty());
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_035.getCode())).thenReturn("Environment not found");

        assertThatThrownBy(() -> new MessageEnvironmentsExistRule(catalogPort, environmentRepository)
                .validate(context(DEVELOP_ID, PRODUCTION_ID)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void belong_doesNotThrow_whenEveryEnvironmentBelongsToApplication() {
        when(environmentRepository.findById(DEVELOP_ID)).thenReturn(Optional.of(environment(DEVELOP_ID, APPLICATION_ID)));

        assertThatCode(() -> new MessageEnvironmentsBelongApplicationRule(catalogPort, environmentRepository)
                .validate(context(DEVELOP_ID))).doesNotThrowAnyException();
    }

    @Test
    void belong_throwsForbiddenUsingFun036_whenAnyEnvironmentBelongsToAnotherApplication() {
        when(environmentRepository.findById(DEVELOP_ID)).thenReturn(Optional.of(environment(DEVELOP_ID, APPLICATION_ID)));
        when(environmentRepository.findById(PRODUCTION_ID))
                .thenReturn(Optional.of(environment(PRODUCTION_ID, OTHER_APPLICATION_ID)));
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_036.getCode())).thenReturn("Foreign environment");

        assertThatThrownBy(() -> new MessageEnvironmentsBelongApplicationRule(catalogPort, environmentRepository)
                .validate(context(DEVELOP_ID, PRODUCTION_ID)))
                .isInstanceOf(ForbiddenException.class)
                .satisfies(ex -> assertThat(((ForbiddenException) ex).getUserMessage()).isEqualTo("Foreign environment"));
    }

    @Test
    void belong_throwsWithoutRepositoryLookup_whenApplicationIdIsNotAUsableUuid() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_036.getCode())).thenReturn("Foreign environment");

        assertThatThrownBy(() -> new MessageEnvironmentsBelongApplicationRule(catalogPort, environmentRepository)
                .validate(new MessageEnvironmentSelectionContext(List.of(DEVELOP_ID), "not-a-uuid")))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(environmentRepository);
    }

    private static MessageEnvironmentSelectionContext context(String... environmentIds) {
        return new MessageEnvironmentSelectionContext(Arrays.asList(environmentIds), APPLICATION_ID);
    }

    private static EnvironmentData environment(String environmentId, String applicationId) {
        return new EnvironmentData(UUID.fromString(environmentId),
                ApplicationData.build(UUID.fromString(applicationId), "App"),
                new EnvironmentTypeData(UUID.randomUUID(), "Develop"));
    }
}
