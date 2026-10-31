package co.edu.uco.application.usecase.validator.session.rule;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.entity.ApplicationData;
import co.edu.uco.application.secondaryports.repository.ApplicationRepository;
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
class SessionApplicationExistsRuleTest {

    private static final String APPLICATION_ID = "c2d9f822-36d3-4580-a838-4f79f3c7cff8";
    private static final String NOT_FOUND_MESSAGE = "Application not found";

    @Mock
    private CatalogPort catalogPort;
    @Mock
    private ApplicationRepository applicationRepository;

    private SessionApplicationExistsRule rule;

    @BeforeEach
    void setUp() {
        rule = new SessionApplicationExistsRule(catalogPort, applicationRepository);
    }

    @Test
    void validate_doesNotThrow_whenApplicationIsRegisteredWithSameUuid() {
        when(applicationRepository.findById(APPLICATION_ID))
                .thenReturn(Optional.of(ApplicationData.build(UUID.fromString(APPLICATION_ID), "MessageUcoLab")));

        assertThatCode(() -> rule.validate(context(APPLICATION_ID))).doesNotThrowAnyException();
    }

    @Test
    void validate_throwsNotFoundUsingFun157_whenApplicationIsNotRegistered() {
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.empty());
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_157.getCode())).thenReturn(NOT_FOUND_MESSAGE);
        SessionApplicationValidationContext context = context(APPLICATION_ID);

        assertThatThrownBy(() -> rule.validate(context))
                .isInstanceOf(NotFoundException.class)
                .satisfies(exception -> assertThat((NotFoundException) exception)
                        .extracting(NotFoundException::getHttpStatus, NotFoundException::getUserMessage)
                        .containsExactly(404, NOT_FOUND_MESSAGE));
    }

    @Test
    void validate_throwsNotFoundWithoutRepositoryLookup_whenApplicationIdIsNotAUsableUuid() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_157.getCode())).thenReturn(NOT_FOUND_MESSAGE);

        assertThatThrownBy(() -> rule.validate(context("not-a-uuid"))).isInstanceOf(NotFoundException.class);

        verifyNoInteractions(applicationRepository);
    }

    private static SessionApplicationValidationContext context(String applicationId) {
        return new SessionApplicationValidationContext(List.of(), applicationId, null);
    }
}
