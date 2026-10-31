package co.edu.uco.application.usecase;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.entity.MessageData;
import co.edu.uco.application.common.catalog.strategy.MessageCatalogStrategy;
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
    private static final String REQUESTED_ENVIRONMENT = "environment-uuid";
    private static final String APPLICATION_ID = "application-uuid";
    private static final String RESOLVED_ENVIRONMENT = "resolved-environment-uuid";
    private static final MessageAccessContext CONTEXT = new MessageAccessContext(APPLICATION_ID);

    @Mock private FindMessageCodeValidator validator;
    @Mock private MessageEnvironmentResolver environmentResolver;
    @Mock private MessageCatalogStrategy strategy;
    @Mock private CatalogPort catalog;

    private ResolveMessageUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ResolveMessageUseCase(validator, environmentResolver, strategy, catalog);
    }

    @Test
    void execute_returnsOnlyTrimmedConsumerFields_whenMessageExists() {
        when(environmentResolver.resolve(CONTEXT, REQUESTED_ENVIRONMENT)).thenReturn(RESOLVED_ENVIRONMENT);
        MessageData message = MessageData.build();
        message.setCode(" CODE ");
        message.setTitle(" Title ");
        message.setContent(" Content ");
        when(strategy.getAvailableMessageByCodeAndEnvironment("CODE", APPLICATION_ID, RESOLVED_ENVIRONMENT)).thenReturn(Optional.of(message));

        var result = useCase.execute("CODE", REQUESTED_ENVIRONMENT, CONTEXT);

        assertThat(result).extracting("code", "title", "content")
                .containsExactly("CODE", "Title", "Content");
        verify(environmentResolver).resolve(CONTEXT, REQUESTED_ENVIRONMENT);
        verify(strategy).getAvailableMessageByCodeAndEnvironment("CODE", APPLICATION_ID, RESOLVED_ENVIRONMENT);
    }

    @Test
    void execute_doesNotAccessEnvironmentOrRepository_whenCodeValidationFails() {
        doThrow(BusinessRuleException.buildUserException("invalid code")).when(validator).validate(" ");

        assertThatThrownBy(() -> useCase.execute(" ", REQUESTED_ENVIRONMENT, CONTEXT)).isInstanceOf(BusinessRuleException.class);

        verifyNoInteractions(environmentResolver, strategy, catalog);
    }

    @Test
    void execute_throwsNotFound_whenRepositoryHasNoAvailableMessage() {
        when(environmentResolver.resolve(CONTEXT, REQUESTED_ENVIRONMENT)).thenReturn(RESOLVED_ENVIRONMENT);
        when(strategy.getAvailableMessageByCodeAndEnvironment("MISSING", APPLICATION_ID, RESOLVED_ENVIRONMENT)).thenReturn(Optional.empty());
        when(catalog.getMessage(MessageCatalogCodeEnum.FUN_218.getCode()))
                .thenReturn("Message %s is not available for application %s in environment %s");

        assertThatThrownBy(() -> useCase.execute("MISSING", REQUESTED_ENVIRONMENT, CONTEXT))
                .isInstanceOf(NotFoundException.class)
                .satisfies(exception -> assertThat(((NotFoundException) exception).getUserMessage())
                        .isEqualTo("Message MISSING is not available for application application-uuid in environment resolved-environment-uuid"));

        verify(strategy).getAvailableMessageByCodeAndEnvironment("MISSING", APPLICATION_ID, RESOLVED_ENVIRONMENT);
    }

    @Test
    void execute_propagatesTechnicalFailure_insteadOfReportingNotFound() {
        when(environmentResolver.resolve(CONTEXT, REQUESTED_ENVIRONMENT)).thenReturn(RESOLVED_ENVIRONMENT);
        var failure = CrossWordsException.buildInfrastructure("database unavailable", new IllegalStateException("db"));
        when(strategy.getAvailableMessageByCodeAndEnvironment("CODE", APPLICATION_ID, RESOLVED_ENVIRONMENT)).thenThrow(failure);

        assertThatThrownBy(() -> useCase.execute("CODE", REQUESTED_ENVIRONMENT, CONTEXT)).isSameAs(failure);
    }

    @Test
    void execute_doesNotQueryRepository_whenEnvironmentDoesNotBelongToApplication() {
        var denial = BusinessRuleException.buildUserException("environment does not belong to application");
        when(environmentResolver.resolve(CONTEXT, REQUESTED_ENVIRONMENT)).thenThrow(denial);

        assertThatThrownBy(() -> useCase.execute("CODE", REQUESTED_ENVIRONMENT, CONTEXT)).isSameAs(denial);

        verifyNoInteractions(strategy, catalog);
    }
}



