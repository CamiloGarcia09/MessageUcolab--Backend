package co.edu.uco.application.usecase;

import co.edu.uco.application.common.catalog.strategy.MessageCatalogStrategy;
import co.edu.uco.application.common.mapper.entity.DataMapper;
import co.edu.uco.application.primaryports.dto.message.MessageDTO;
import co.edu.uco.application.secondaryports.entity.MessageData;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.SimplePage;
import co.edu.uco.application.secondaryports.repository.SimplePageRequest;
import co.edu.uco.application.usecase.domain.MessageDomain;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;
import co.edu.uco.application.usecase.security.MessageEnvironmentResolver;
import co.edu.uco.application.usecase.validator.message.ListMessageValidator;
import co.edu.uco.application.usecase.validator.page.PageRequestRangeValidator;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.BusinessException;
import co.edu.uco.crosscutting.exceptions.CrossWordsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindMessageByEnvironmentUseCaseTest {

    private static final MessageAccessContext ACCESS_CONTEXT = new MessageAccessContext("app-1");

    @Mock
    private MessageCatalogStrategy messageCatalogStrategy;
    @Mock
    private DataMapper<MessageData, MessageDomain, MessageDTO> entityMapper;
    @Mock
    private ListMessageValidator listMessageValidator;
    @Mock
    private PageRequestRangeValidator rangeValidator;
    @Mock
    private MessageEnvironmentResolver environmentResolver;
    @Mock
    private LoggingPortFactory loggerFactory;
    @Mock
    private LoggingPort log;

    private FindMessageByEnvironmentUseCase useCase;

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(FindMessageByEnvironmentUseCase.class)).thenReturn(log);
        useCase = new FindMessageByEnvironmentUseCase(
                messageCatalogStrategy, entityMapper, listMessageValidator, rangeValidator,
                environmentResolver, loggerFactory);
        when(environmentResolver.resolveApplication(ACCESS_CONTEXT)).thenReturn("env");
    }

    @Test
    void execute_returnsMappedPage_whenMessagesExist() {
        MessageData message = new MessageData();
        MessageDTO dto = MessageDTO.create("CODE", "Title", "Content", "type", "category", "func");
        SimplePage<MessageData> page = SimplePage.of(List.of(message), 1, 10, 1, 1);
        SimplePageRequest request = new SimplePageRequest();
        when(messageCatalogStrategy.getMessagesWithApplication("env", request)).thenReturn(page);
        when(entityMapper.mapperDTO(message)).thenReturn(dto);

        SimplePage<MessageDTO> result = useCase.execute(ACCESS_CONTEXT, request);

        assertThat(result.getData()).containsExactly(dto);
        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getTotalItems()).isEqualTo(1);
    }

    @Test
    void execute_returnsEmptyPage_whenNoMessages() {
        SimplePage<MessageData> page = SimplePage.of(List.of(), 1, 10, 0, 0);
        SimplePageRequest request = new SimplePageRequest();
        when(messageCatalogStrategy.getMessagesWithApplication("env", request)).thenReturn(page);

        SimplePage<MessageDTO> result = useCase.execute(ACCESS_CONTEXT, request);

        assertThat(result.getData()).isEmpty();
    }

    @Test
    void execute_skipsRangeValidation_whenApplicationHasNoMessages() {
        SimplePageRequest request = new SimplePageRequest();
        when(messageCatalogStrategy.getMessagesWithApplication("env", request))
                .thenReturn(SimplePage.of(List.of(), 1, 10, 0, 0));

        assertThat(useCase.execute(ACCESS_CONTEXT, request).getData()).isEmpty();
        verifyNoInteractions(rangeValidator);
    }

    @Test
    void execute_rethrowsCrossWordsException() {
        SimplePageRequest request = new SimplePageRequest();
        doThrow(CrossWordsException.build("range error")).when(rangeValidator).validate(1, 0);
        SimplePage<MessageData> page = SimplePage.of(List.of(new MessageData()), 1, 10, 1, 0);
        when(messageCatalogStrategy.getMessagesWithApplication("env", request)).thenReturn(page);

        assertThatThrownBy(() -> useCase.execute(ACCESS_CONTEXT, request))
                .isInstanceOf(CrossWordsException.class)
                .satisfies(ex -> assertThat(((CrossWordsException) ex).getTechnicalMessage()).isEqualTo("range error"));
    }

    @Test
    void execute_throwsBusinessException_whenUnexpectedErrorOccurs() {
        SimplePageRequest request = new SimplePageRequest();
        when(messageCatalogStrategy.getMessagesWithApplication("env", request))
                .thenThrow(new IllegalStateException("boom"));
        when(messageCatalogStrategy.getSystemMessageContent(MessageCatalogCodeEnum.FUN_011.getCode()))
                .thenReturn("Unexpected error listing messages");

        assertThatThrownBy(() -> useCase.execute(ACCESS_CONTEXT, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getUserMessage())
                        .isEqualTo("Unexpected error listing messages"));
        verify(log).error("Unexpected error listing messages");
    }
}
