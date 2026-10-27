package co.edu.uco.application.usecase;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.entity.MessageData;
import co.edu.uco.application.secondaryports.repository.ResolveMessageRepository;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;
import co.edu.uco.application.usecase.security.MessageEnvironmentResolver;
import co.edu.uco.application.usecase.validator.message.FindMessageCodeValidator;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.NotFoundException;
import co.edu.uco.crosscutting.exceptions.CrossWordsException;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResolveMessageUseCaseTest {
    private static final String REQUESTED_ENVIRONMENT = "env-1";
    private static final MessageAccessContext CONTEXT = new MessageAccessContext("app-1");

    @Mock private FindMessageCodeValidator validator;
    @Mock private MessageEnvironmentResolver environmentResolver;
    @Mock private ResolveMessageRepository repository;
    @Mock private CatalogPort catalog;

    private ResolveMessageUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ResolveMessageUseCase(validator, environmentResolver, repository, catalog);
    }

    @Test
    void execute_returnsOnlyTrimmedConsumerFields_whenMessageExists() {
        when(environmentResolver.resolve(CONTEXT, REQUESTED_ENVIRONMENT)).thenReturn("env-1");
        MessageData message = MessageData.build();
        message.setCode(" CODE ");
        message.setTitle(" Title ");
        message.setContent(" Content ");
        when(repository.findAvailableByCodeAndEnvironment("CODE", "env-1")).thenReturn(Optional.of(message));

        var result = useCase.execute("CODE", REQUESTED_ENVIRONMENT, CONTEXT);

        assertThat(result).extracting("code", "title", "content")
                .containsExactly("CODE", "Title", "Content");
        verify(environmentResolver).resolve(CONTEXT, REQUESTED_ENVIRONMENT);
        verify(repository).findAvailableByCodeAndEnvironment("CODE", "env-1");
    }

    @Test
    void execute_doesNotAccessEnvironmentOrRepository_whenCodeValidationFails() {
        doThrow(BusinessRuleException.buildUserException("invalid code")).when(validator).validate(" ");

        assertThatThrownBy(() -> useCase.execute(" ", REQUESTED_ENVIRONMENT, CONTEXT)).isInstanceOf(BusinessRuleException.class);

        verifyNoInteractions(environmentResolver, repository, catalog);
    }

    @Test
    void execute_throwsNotFound_whenRepositoryHasNoAvailableMessage() {
        when(environmentResolver.resolve(CONTEXT, REQUESTED_ENVIRONMENT)).thenReturn("env-1");
        when(repository.findAvailableByCodeAndEnvironment("MISSING", "env-1")).thenReturn(Optional.empty());
        when(catalog.getMessage(MessageCatalogCodeEnum.FUN_012.getCode()))
                .thenReturn("Message %s missing in %s");

        assertThatThrownBy(() -> useCase.execute("MISSING", REQUESTED_ENVIRONMENT, CONTEXT)).isInstanceOf(NotFoundException.class);

        verify(repository).findAvailableByCodeAndEnvironment("MISSING", "env-1");
    }

    @Test
    void execute_propagatesTechnicalFailure_insteadOfReportingNotFound() {
        when(environmentResolver.resolve(CONTEXT, REQUESTED_ENVIRONMENT)).thenReturn("env-1");
        var failure = CrossWordsException.buildInfrastructure("database unavailable", new IllegalStateException("db"));
        when(repository.findAvailableByCodeAndEnvironment("CODE", "env-1")).thenThrow(failure);

        assertThatThrownBy(() -> useCase.execute("CODE", REQUESTED_ENVIRONMENT, CONTEXT)).isSameAs(failure);
    }
}
