package co.edu.uco.application.usecase.validator.session.rule;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.entity.ApplicationData;
import co.edu.uco.application.secondaryports.entity.EnvironmentData;
import co.edu.uco.application.secondaryports.entity.EnvironmentTypeData;
import co.edu.uco.application.secondaryports.repository.EnvironmentRepository;
import co.edu.uco.application.usecase.validator.session.SessionApplicationValidationContext;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionEnvironmentBelongsApplicationRuleTest {

    private static final UUID APPLICATION_ID = UUID.fromString("c2d9f822-36d3-4580-a838-4f79f3c7cff8");
    private static final UUID OTHER_APPLICATION_ID = UUID.fromString("72436a46-54d7-4cb5-a42c-af14f92d9416");
    private static final String ENVIRONMENT_ID = "7b3e5d91-a2c8-46f0-9d14-5e7a1b6c3f82";
    private static final String CONFLICT_MESSAGE = "Environment does not belong to application";

    @Mock
    private CatalogPort catalogPort;
    @Mock
    private EnvironmentRepository environmentRepository;

    private SessionEnvironmentBelongsApplicationRule rule;

    @BeforeEach
    void setUp() {
        rule = new SessionEnvironmentBelongsApplicationRule(catalogPort, environmentRepository);
    }

    @Test
    void validate_doesNotThrow_whenEnvironmentBelongsToApplication() {
        when(environmentRepository.findById(ENVIRONMENT_ID)).thenReturn(Optional.of(environmentOf(APPLICATION_ID)));

        assertThatCode(() -> rule.validate(context(APPLICATION_ID.toString(), ENVIRONMENT_ID)))
                .doesNotThrowAnyException();

        verify(environmentRepository).findById(ENVIRONMENT_ID);
    }

    @Test
    void validate_throwsConflictUsingFun160_whenEnvironmentBelongsToAnotherApplication() {
        when(environmentRepository.findById(ENVIRONMENT_ID)).thenReturn(Optional.of(environmentOf(OTHER_APPLICATION_ID)));
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_160.getCode())).thenReturn(CONFLICT_MESSAGE);
        SessionApplicationValidationContext context = context(APPLICATION_ID.toString(), ENVIRONMENT_ID);

        assertThatThrownBy(() -> rule.validate(context))
                .isInstanceOf(ConflictException.class)
                .satisfies(exception -> assertThat((ConflictException) exception)
                        .extracting(ConflictException::getHttpStatus, ConflictException::getUserMessage)
                        .containsExactly(409, CONFLICT_MESSAGE));
    }

    @Test
    void validate_throwsConflict_whenEnvironmentDoesNotExist() {
        when(environmentRepository.findById(ENVIRONMENT_ID)).thenReturn(Optional.empty());
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_160.getCode())).thenReturn(CONFLICT_MESSAGE);

        assertThatThrownBy(() -> rule.validate(context(APPLICATION_ID.toString(), ENVIRONMENT_ID)))
                .isInstanceOf(ConflictException.class);
    }

    @ParameterizedTest(name = "environmentId=[{0}]")
    @ValueSource(strings = {"", "not-a-uuid", "00000000-0000-0000-0000-000000000000"})
    void validate_throwsConflictWithoutRepositoryLookup_whenEnvironmentIdIsNotAUsableUuid(String environmentId) {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_160.getCode())).thenReturn(CONFLICT_MESSAGE);
        SessionApplicationValidationContext context = context(APPLICATION_ID.toString(), environmentId);

        assertThatThrownBy(() -> rule.validate(context)).isInstanceOf(ConflictException.class);

        verifyNoInteractions(environmentRepository);
    }

    @Test
    void validate_throwsConflictWithoutRepositoryLookup_whenApplicationIdIsNotAUsableUuid() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_160.getCode())).thenReturn(CONFLICT_MESSAGE);

        assertThatThrownBy(() -> rule.validate(context("not-a-uuid", ENVIRONMENT_ID)))
                .isInstanceOf(ConflictException.class);

        verifyNoInteractions(environmentRepository);
    }

    @Test
    void validate_throwsConflictWithoutRepositoryLookup_whenContextIsNull() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_160.getCode())).thenReturn(CONFLICT_MESSAGE);

        assertThatThrownBy(() -> rule.validate(null)).isInstanceOf(ConflictException.class);

        verifyNoInteractions(environmentRepository);
    }

    private static EnvironmentData environmentOf(UUID applicationId) {
        return new EnvironmentData(UUID.fromString(ENVIRONMENT_ID),
                ApplicationData.build(applicationId, "App"),
                new EnvironmentTypeData(UUID.randomUUID(), "Develop"));
    }

    private static SessionApplicationValidationContext context(String applicationId, String environmentId) {
        return new SessionApplicationValidationContext(List.of(), applicationId, environmentId);
    }
}
