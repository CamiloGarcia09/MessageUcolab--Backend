package co.edu.uco.application.usecase;

import co.edu.uco.application.common.catalog.strategy.MessageCatalogStrategy;
import co.edu.uco.application.common.mapper.entity.DataMapper;
import co.edu.uco.application.primaryports.dto.message.MessageDTO;
import co.edu.uco.application.secondaryports.entity.MessageData;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.usecase.domain.MessageDomain;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;
import co.edu.uco.application.usecase.security.MessageEnvironmentResolver;
import co.edu.uco.application.usecase.validator.message.FindMessageCodeValidator;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindMessageByCodeAndEnvironmentUseCaseTest {

    private static final MessageAccessContext ACCESS_CONTEXT = new MessageAccessContext("app-1");

    @Mock
    private MessageCatalogStrategy messageCatalogStrategy;
    @Mock
    private FindMessageCodeValidator findMessageCodeValidator;
    @Mock
    private DataMapper<MessageData, MessageDomain, MessageDTO> entityMapper;
    @Mock
    private MessageEnvironmentResolver environmentResolver;
    @Mock
    private LoggingPortFactory loggerFactory;
    @Mock
    private LoggingPort log;

    private FindMessageByCodeAndEnvironmentUseCase useCase;

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(FindMessageByCodeAndEnvironmentUseCase.class)).thenReturn(log);
        useCase = new FindMessageByCodeAndEnvironmentUseCase(
                messageCatalogStrategy, findMessageCodeValidator, entityMapper, environmentResolver, loggerFactory);
        when(environmentResolver.resolveApplication(ACCESS_CONTEXT)).thenReturn("env");
    }

    @Test
    void execute_returnsMappedDto_whenMessageExists() {
        MessageData message = new MessageData();
        MessageDTO dto = MessageDTO.create("CODE", "Title", "Content", "type", "category", "func");
        when(messageCatalogStrategy.getMessageByCodeAndApplication("CODE", "env"))
                .thenReturn(Optional.of(message));
        when(entityMapper.mapperDTO(message)).thenReturn(dto);

        MessageDTO result = useCase.execute("CODE", ACCESS_CONTEXT);

        assertThat(result).isSameAs(dto);
    }

    @Test
    void execute_throwsBusinessException_whenMessageNotFound() {
        when(messageCatalogStrategy.getMessageByCodeAndApplication("CODE", "env"))
                .thenReturn(Optional.empty());
        when(messageCatalogStrategy.getSystemMessageContent(MessageCatalogCodeEnum.FUN_012.getCode()))
                .thenReturn("Message %s not found in environment %s");

        assertThatThrownBy(() -> useCase.execute("CODE", ACCESS_CONTEXT))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getUserMessage())
                        .isEqualTo("Message CODE not found in environment env"));
    }

    @Test
    void execute_throwsBusinessException_whenUnexpectedExceptionOccurs() {
        when(messageCatalogStrategy.getMessageByCodeAndApplication("CODE", "env"))
                .thenThrow(new IllegalStateException("boom"));
        when(messageCatalogStrategy.getSystemMessageContent(MessageCatalogCodeEnum.FUN_012.getCode()))
                .thenReturn("Message %s not found in environment %s");

        assertThatThrownBy(() -> useCase.execute("CODE", ACCESS_CONTEXT))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getUserMessage())
                        .isEqualTo("Message CODE not found in environment env"));

        ArgumentCaptor<Exception> exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(log).error(eq("Message CODE not found in environment env"), exceptionCaptor.capture());
        assertThat(exceptionCaptor.getValue()).isInstanceOf(IllegalStateException.class);
    }
}
