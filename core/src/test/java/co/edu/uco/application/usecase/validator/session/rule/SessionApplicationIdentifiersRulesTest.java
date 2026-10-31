package co.edu.uco.application.usecase.validator.session.rule;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.validator.session.SessionApplicationValidationContext;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Reglas de requerido y formato UUID del contexto de sesion.
 */
@ExtendWith(MockitoExtension.class)
class SessionApplicationIdentifiersRulesTest {

    private static final String APPLICATION_ID = "c2d9f822-36d3-4580-a838-4f79f3c7cff8";
    private static final String ENVIRONMENT_ID = "7b3e5d91-a2c8-46f0-9d14-5e7a1b6c3f82";

    @Mock
    private CatalogPort catalogPort;

    @Test
    void identifiersRequired_doesNotThrow_whenApplicationAndEnvironmentArePresent() {
        assertThatCode(() -> new SessionApplicationIdentifiersRequiredRule(catalogPort)
                .validate(context(APPLICATION_ID, ENVIRONMENT_ID))).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "environmentId=[{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void identifiersRequired_throwsUsingFun155_whenEnvironmentIsMissing(String environmentId) {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_155.getCode())).thenReturn("Identifiers required");
        SessionApplicationValidationContext context = context(APPLICATION_ID, environmentId);

        assertThatThrownBy(() -> new SessionApplicationIdentifiersRequiredRule(catalogPort).validate(context))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(exception -> assertThat(((BusinessRuleException) exception).getUserMessage())
                        .isEqualTo("Identifiers required"));
    }

    @ParameterizedTest(name = "applicationId=[{0}]")
    @NullAndEmptySource
    void identifiersRequired_throwsUsingFun155_whenApplicationIsMissing(String applicationId) {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_155.getCode())).thenReturn("Identifiers required");

        assertThatThrownBy(() -> new SessionApplicationIdentifiersRequiredRule(catalogPort)
                .validate(context(applicationId, ENVIRONMENT_ID)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void identifiersRequired_throws_whenContextIsNull() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_155.getCode())).thenReturn("Identifiers required");

        assertThatThrownBy(() -> new SessionApplicationIdentifiersRequiredRule(catalogPort).validate(null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void applicationIdRequired_doesNotThrow_whenPresent() {
        assertThatCode(() -> new SessionApplicationIdRequiredRule(catalogPort).validate(context(APPLICATION_ID, null)))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "applicationId=[{0}]")
    @NullAndEmptySource
    void applicationIdRequired_throwsUsingFun172_whenMissing(String applicationId) {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_172.getCode())).thenReturn("Application id required");

        assertThatThrownBy(() -> new SessionApplicationIdRequiredRule(catalogPort).validate(context(applicationId, null)))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(exception -> assertThat(((BusinessRuleException) exception).getUserMessage())
                        .isEqualTo("Application id required"));
    }

    @Test
    void uuidRules_doNotThrow_whenIdentifiersAreValidUuids() {
        SessionApplicationValidationContext context = context(APPLICATION_ID, ENVIRONMENT_ID);

        assertThatCode(() -> new SessionApplicationIdUuidRule(catalogPort).validate(context)).doesNotThrowAnyException();
        assertThatCode(() -> new SessionEnvironmentIdUuidRule(catalogPort).validate(context)).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "applicationId=[{0}]")
    @ValueSource(strings = {"not-a-uuid", "00000000-0000-0000-0000-000000000000"})
    void applicationIdUuid_throwsUsingFun038_whenNotAUsableUuid(String applicationId) {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_038.getCode())).thenReturn("Invalid id");

        assertThatThrownBy(() -> new SessionApplicationIdUuidRule(catalogPort)
                .validate(context(applicationId, ENVIRONMENT_ID)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @ParameterizedTest(name = "environmentId=[{0}]")
    @ValueSource(strings = {"not-a-uuid", "00000000-0000-0000-0000-000000000000"})
    void environmentIdUuid_throwsUsingFun038_whenNotAUsableUuid(String environmentId) {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_038.getCode())).thenReturn("Invalid id");

        assertThatThrownBy(() -> new SessionEnvironmentIdUuidRule(catalogPort)
                .validate(context(APPLICATION_ID, environmentId)))
                .isInstanceOf(BusinessRuleException.class);
    }

    private static SessionApplicationValidationContext context(String applicationId, String environmentId) {
        return new SessionApplicationValidationContext(List.of(), applicationId, environmentId);
    }
}
