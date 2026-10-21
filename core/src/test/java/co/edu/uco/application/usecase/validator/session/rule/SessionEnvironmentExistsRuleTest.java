package co.edu.uco.application.usecase.validator.session.rule;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.entity.ApplicationData;
import co.edu.uco.application.secondaryports.entity.EnvironmentData;
import co.edu.uco.application.secondaryports.entity.EnvironmentTypeData;
import co.edu.uco.application.secondaryports.repository.EnvironmentRepository;
import co.edu.uco.application.usecase.validator.session.SessionApplicationValidationContext;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionEnvironmentExistsRuleTest {

    private static final String ENVIRONMENT_ID = "7b3e5d91-a2c8-46f0-9d14-5e7a1b6c3f82";
    private static final String NOT_FOUND_MESSAGE = "Environment not found";

    @Mock
    private CatalogPort catalogPort;
    @Mock
    private EnvironmentRepository environmentRepository;

    private SessionEnvironmentExistsRule rule;

    @BeforeEach
    void setUp() {
        rule = new SessionEnvironmentExistsRule(catalogPort, environmentRepository);
    }

    @Test
    void validate_doesNotThrow_whenEnvironmentExists() {
        when(environmentRepository.findById(ENVIRONMENT_ID)).thenReturn(Optional.of(new EnvironmentData(
                UUID.fromString(ENVIRONMENT_ID), ApplicationData.build(UUID.randomUUID(), "App"),
                new EnvironmentTypeData(UUID.randomUUID(), "Develop"))));

        assertThatCode(() -> rule.validate(context(ENVIRONMENT_ID))).doesNotThrowAnyException();
    }

    @Test
    void validate_throwsNotFoundUsingFun158_whenEnvironmentDoesNotExist() {
        when(environmentRepository.findById(ENVIRONMENT_ID)).thenReturn(Optional.empty());
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_158.getCode())).thenReturn(NOT_FOUND_MESSAGE);
        SessionApplicationValidationContext context = context(ENVIRONMENT_ID);

        assertThatThrownBy(() -> rule.validate(context))
                .isInstanceOf(NotFoundException.class)
                .satisfies(exception -> assertThat(((NotFoundException) exception).getUserMessage())
                        .isEqualTo(NOT_FOUND_MESSAGE));
    }

    @Test
    void validate_throwsNotFoundWithoutRepositoryLookup_whenEnvironmentIdIsNotAUsableUuid() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_158.getCode())).thenReturn(NOT_FOUND_MESSAGE);

        assertThatThrownBy(() -> rule.validate(context("not-a-uuid"))).isInstanceOf(NotFoundException.class);

        verifyNoInteractions(environmentRepository);
    }

    private static SessionApplicationValidationContext context(String environmentId) {
        return new SessionApplicationValidationContext(List.of(), "c2d9f822-36d3-4580-a838-4f79f3c7cff8", environmentId);
    }
}
