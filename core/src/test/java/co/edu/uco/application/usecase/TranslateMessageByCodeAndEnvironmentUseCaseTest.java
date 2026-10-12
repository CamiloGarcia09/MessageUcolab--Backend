package co.edu.uco.application.usecase;

import co.edu.uco.application.common.catalog.strategy.MessageCatalogStrategy;
import co.edu.uco.application.primaryports.dto.message.TranslatedMessageDTO;
import co.edu.uco.application.secondaryports.entity.FunctionalityData;
import co.edu.uco.application.secondaryports.entity.MessageCategoryData;
import co.edu.uco.application.secondaryports.entity.MessageData;
import co.edu.uco.application.secondaryports.entity.MessageTranslationRequestData;
import co.edu.uco.application.secondaryports.entity.MessageTranslationResponseData;
import co.edu.uco.application.secondaryports.entity.MessageTypeData;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.translation.MessageTranslationCachePort;
import co.edu.uco.application.secondaryports.translation.MessageTranslationPort;
import co.edu.uco.application.usecase.validator.message.FindMessageCodeValidator;
import co.edu.uco.application.usecase.validator.message.TargetLanguageValidator;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;
import co.edu.uco.application.usecase.domain.security.PermissionCode;
import co.edu.uco.application.usecase.security.MessageEnvironmentResolver;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.BusinessException;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TranslateMessageByCodeAndEnvironmentUseCaseTest {

    private static final MessageAccessContext ACCESS_CONTEXT = new MessageAccessContext("env", null);

    @Mock
    private MessageCatalogStrategy messageCatalogStrategy;
    @Mock
    private MessageTranslationPort messageTranslationPort;
    @Mock
    private MessageTranslationCachePort messageTranslationCachePort;
    @Mock
    private FindMessageCodeValidator findMessageCodeValidator;
    @Mock
    private TargetLanguageValidator targetLanguageValidator;
    @Mock
    private MessageEnvironmentResolver environmentResolver;
    @Mock
    private LoggingPortFactory loggerFactory;
    @Mock
    private LoggingPort log;

    private TranslateMessageByCodeAndEnvironmentUseCase useCase;

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(TranslateMessageByCodeAndEnvironmentUseCase.class)).thenReturn(log);
        useCase = new TranslateMessageByCodeAndEnvironmentUseCase(
                messageCatalogStrategy, messageTranslationPort, messageTranslationCachePort,
                findMessageCodeValidator, targetLanguageValidator, environmentResolver, loggerFactory);
        lenient().when(environmentResolver.resolve(ACCESS_CONTEXT, PermissionCode.MESSAGE_TRANSLATE)).thenReturn("env");
    }

    private MessageData messageWithData() {
        MessageData message = new MessageData();
        message.setCode("CODE");
        message.setTitle("Titulo");
        message.setContent("Contenido");
        message.setType(new MessageTypeData(java.util.UUID.randomUUID(), "info"));
        message.setCategory(new MessageCategoryData(java.util.UUID.randomUUID(), "general"));
        message.setApplication("app");
        message.setFunctionality(FunctionalityData.build("func"));
        return message;
    }

    @Test
    void execute_translatesMessageSuccessfully() {
        MessageData message = messageWithData();
        when(messageCatalogStrategy.getMessageByCodeAndEnvironment("CODE", "env"))
                .thenReturn(Optional.of(message));
        MessageTranslationResponseData response = MessageTranslationResponseData.create(
                "Translated Title", "Translated Content", "translated type", "translated category",
                "translated functionality", "ollama", "llama3.2", 120L);
        when(messageTranslationPort.translate(any(MessageTranslationRequestData.class)))
                .thenReturn(response);

        TranslatedMessageDTO result = useCase.execute("CODE", ACCESS_CONTEXT, "", "en");

        assertAll(
                () -> assertThat(result.getCode()).isEqualTo("CODE"),
                () -> assertThat(result.getSourceLanguage()).isEqualTo("auto"),
                () -> assertThat(result.getTargetLanguage()).isEqualTo("en"),
                 () -> assertThat(result.getTranslatedTitle()).isEqualTo("Translated Title"),
                 () -> assertThat(result.getOriginalType()).isEqualTo("info"),
                 () -> assertThat(result.getTranslatedType()).isEqualTo("translated type"),
                 () -> assertThat(result.getOriginalCategory()).isEqualTo("general"),
                 () -> assertThat(result.getTranslatedCategory()).isEqualTo("translated category"),
                 () -> assertThat(result.getOriginalFunctionality()).isEqualTo("func"),
                 () -> assertThat(result.getTranslatedFunctionality()).isEqualTo("translated functionality"),
                () -> assertThat(result.getTranslationProvider()).isEqualTo("ollama"),
                () -> assertThat(result.getTranslationElapsedMs()).isEqualTo(120L),
                () -> assertThat(result.isDynamicTranslation()).isTrue());
        verify(messageTranslationCachePort).saveTranslation(eq("CODE"), eq("env"), eq("auto"), eq("en"), eq(response));
    }

    @Test
    void execute_usesTrimmedSourceLanguage_whenProvided() {
        MessageData message = messageWithData();
        when(messageCatalogStrategy.getMessageByCodeAndEnvironment("CODE", "env"))
                .thenReturn(Optional.of(message));
        MessageTranslationResponseData response = MessageTranslationResponseData.create(
                "t", "c", "tt", "tc", "tf", "ollama", "m", 1L);
        when(messageTranslationPort.translate(any(MessageTranslationRequestData.class)))
                .thenReturn(response);

        TranslatedMessageDTO result = useCase.execute("CODE", ACCESS_CONTEXT, "  es ", "en");

        assertThat(result.getSourceLanguage()).isEqualTo("es");
    }

    @Test
    void execute_servesTranslationFromCache_whenCacheHit() {
        MessageData message = messageWithData();
        when(messageCatalogStrategy.getMessageByCodeAndEnvironment("CODE", "env"))
                .thenReturn(Optional.of(message));
        MessageTranslationResponseData cached = MessageTranslationResponseData.create(
                "Cached Title", "Cached Content", "cached type", "cached category",
                "cached functionality", "ollama", "llama3.2", 5L);
        when(messageTranslationCachePort.findTranslation("CODE", "env", "es", "en"))
                .thenReturn(Optional.of(cached));

        TranslatedMessageDTO result = useCase.execute("CODE", ACCESS_CONTEXT, "es", "en");

        assertAll(
                () -> assertThat(result.getTranslatedTitle()).isEqualTo("Cached Title"),
                 () -> assertThat(result.getTranslatedContent()).isEqualTo("Cached Content"),
                 () -> assertThat(result.getOriginalType()).isEqualTo("info"),
                 () -> assertThat(result.getTranslatedType()).isEqualTo("cached type"),
                 () -> assertThat(result.getOriginalCategory()).isEqualTo("general"),
                 () -> assertThat(result.getTranslatedCategory()).isEqualTo("cached category"),
                 () -> assertThat(result.getOriginalFunctionality()).isEqualTo("func"),
                 () -> assertThat(result.getTranslatedFunctionality()).isEqualTo("cached functionality"),
                () -> assertThat(result.getTranslationElapsedMs()).isEqualTo(5L));
        verify(messageTranslationPort, never()).translate(any());
        verify(messageTranslationCachePort).findTranslation("CODE", "env", "es", "en");
    }

    @Test
    void execute_throwsBusinessException_whenMessageNotFound() {
        when(messageCatalogStrategy.getMessageByCodeAndEnvironment("CODE", "env"))
                .thenReturn(Optional.empty());
        when(messageCatalogStrategy.getSystemMessageContent(MessageCatalogCodeEnum.FUN_012.getCode()))
                .thenReturn("Message %s not found in environment %s");

        assertThatThrownBy(() -> useCase.execute("CODE", ACCESS_CONTEXT, "es", "en"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getUserMessage())
                        .isEqualTo("Message CODE not found in environment env"));
    }

    @Test
    void execute_throwsBusinessException_whenTranslationFails() {
        MessageData message = messageWithData();
        when(messageCatalogStrategy.getMessageByCodeAndEnvironment("CODE", "env"))
                .thenReturn(Optional.of(message));
        when(messageTranslationPort.translate(any(MessageTranslationRequestData.class)))
                .thenThrow(new IllegalStateException("translation down"));
        when(messageCatalogStrategy.getSystemMessageContent(MessageCatalogCodeEnum.FUN_048.getCode()))
                .thenReturn("Error translating message");

        assertThatThrownBy(() -> useCase.execute("CODE", ACCESS_CONTEXT, "es", "en"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getUserMessage())
                        .isEqualTo("Error translating message"));

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(log).error(eq("Error translating message"), captor.capture());
        assertThat(captor.getValue()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void execute_propagatesValidatorException_whenMessageCodeIsInvalid() {
        doThrow(BusinessRuleException.buildUserException("Invalid message code"))
                .when(findMessageCodeValidator).validate("CODE");

        assertThatThrownBy(() -> useCase.execute("CODE", ACCESS_CONTEXT, "es", "en"))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo("Invalid message code"));
    }

    @Test
    void execute_propagatesValidatorException_whenTargetLanguageIsInvalid() {
        doThrow(BusinessRuleException.buildUserException("Invalid target language"))
                .when(targetLanguageValidator).validate("en");

        assertThatThrownBy(() -> useCase.execute("CODE", ACCESS_CONTEXT, "es", "en"))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo("Invalid target language"));

        verify(findMessageCodeValidator).validate("CODE");
    }
}
