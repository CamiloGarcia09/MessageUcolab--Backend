package co.edu.uco.infraestructure.secondaryadapters.translation;

import co.edu.uco.application.common.parameter.TranslationAvailability;
import co.edu.uco.application.secondaryports.entity.MessageTranslationRequestData;
import co.edu.uco.application.secondaryports.entity.MessageTranslationResponseData;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.translation.MessageTranslationPort;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.infraestructure.config.TranslationAiProperties;
import co.edu.uco.crosscutting.exceptions.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Semaphore;

import static co.edu.uco.crosscutting.helpers.UtilObject.isNullObject;
import static co.edu.uco.crosscutting.helpers.UtilText.isEmptyOrNull;
import static co.edu.uco.crosscutting.helpers.UtilText.trim;

@Component
public class LangChain4jMessageTranslationAdapter implements MessageTranslationPort {

    private final LoggingPort log;
    private final CatalogPort catalogPort;
    private final TranslationAiProperties properties;
    private final TranslationAvailability availability;
    private final ObjectMapper objectMapper;
    private final Semaphore permits;
    private ChatModel chatModel;

    public LangChain4jMessageTranslationAdapter(
            CatalogPort catalogPort, TranslationAiProperties properties,
            ObjectMapper objectMapper, LoggingPortFactory loggerFactory,
            TranslationAvailability availability
    ) {
        this.availability = availability;
        this.log = loggerFactory.getLogger(LangChain4jMessageTranslationAdapter.class);
        this.catalogPort = catalogPort;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.permits = new Semaphore(Math.max(1, properties.getMaxConcurrentRequests()));
    }

    private static final String PROVIDER_OPEN_AI = "openai";
    private static final String MODEL_BUSY = "El modelo de traducción alcanzó el máximo de traducciones simultáneas";

    @Override
    public MessageTranslationResponseData translate(MessageTranslationRequestData requestData) {
        if (isNullObject(requestData)) {
            throw BusinessException.buildTechnicalException(
                    catalogPort.getMessage(MessageCatalogCodeEnum.FUN_023.getCode()),
                    catalogPort.getMessage(MessageCatalogCodeEnum.TCH_037.getCode())
            );
        }
        if (!availability.isEnabled()) {
            throw BusinessException.buildUserException(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_046.getCode()));
        }
        if (isOpenAiProvider() && isEmptyOrNull(properties.getApiKey())) {
            throw BusinessException.buildUserException(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_047.getCode()));
        }

        var startedAt = System.nanoTime();
        var permitAcquired = false;
        try {
            // Límite de traducciones simultáneas: si el modelo ya está ocupado no se encola la petición
            // (OWASP LLM06); la ruta de consumo entrega entonces el texto original.
            permitAcquired = permits.tryAcquire();
            if (!permitAcquired) {
                throw new IllegalStateException(MODEL_BUSY);
            }
            var texts = new ProtectedTranslationTexts(requestData);
            var response = model().chat(buildChatRequest(requestData, texts));
            var output = response.aiMessage().text();
            var translated = objectMapper.readValue(output, TranslationModelResponse.class);
            if (!translated.isComplete()) {
                throw BusinessException.buildUserException(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_048.getCode()));
            }
            var restoredTitle = texts.restoreTitle(translated.translatedTitle());
            var restoredContent = texts.restoreContent(translated.translatedContent());
            var restoredType = texts.restoreType(translated.translatedType());
            var restoredCategory = texts.restoreCategory(translated.translatedCategory());
            var restoredFunctionality = texts.restoreFunctionality(translated.translatedFunctionality());
            var elapsedMillis = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
            var providerLabel = providerName();
            log.info(
                    catalogPort.getMessage(MessageCatalogCodeEnum.TCH_036.getCode()),
                    providerLabel,
                    properties.getModelName(),
                    requestData.getCode(),
                    requestData.getTargetLanguage(),
                    elapsedMillis
            );
            return MessageTranslationResponseData.create(
                    restoredTitle,
                    restoredContent,
                    restoredType,
                    restoredCategory,
                    restoredFunctionality,
                    providerLabel,
                    properties.getModelName(),
                    elapsedMillis
            );
        } catch (Exception exception) {
            log.error(
                    catalogPort.getMessage(MessageCatalogCodeEnum.TCH_037.getCode()),
                    providerName(),
                    properties.getModelName(),
                    requestData.getCode(),
                    requestData.getTargetLanguage(),
                    exception
            );
            throw BusinessException.buildUserException(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_048.getCode()));
        } finally {
            if (permitAcquired) {
                permits.release();
            }
        }
    }

    private ChatModel model() {
        if (isNullObject(chatModel)) {
            chatModel = isOpenAiProvider() ? openAiModel() : ollamaModel();
        }
        return chatModel;
    }

    private ChatModel openAiModel() {
        var builder = OpenAiChatModel.builder()
                .apiKey(properties.getApiKey())
                .modelName(properties.getModelName())
                .temperature(properties.getTemperature())
                .maxRetries(properties.getMaxRetries())
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()));

        if (!isEmptyOrNull(properties.getBaseUrl())) {
            builder.baseUrl(properties.getBaseUrl());
        }

        return builder.build();
    }

    private ChatModel ollamaModel() {
        return OllamaChatModel.builder()
                .baseUrl(properties.getBaseUrl())
                .modelName(properties.getModelName())
                .temperature(properties.getTemperature())
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .build();
    }

    private boolean isOpenAiProvider() {
        return PROVIDER_OPEN_AI.equalsIgnoreCase(trim(properties.getProvider()));
    }

    private String providerName() {
        return isOpenAiProvider()
                ? catalogPort.getMessage(MessageCatalogCodeEnum.FUN_058.getCode())
                : catalogPort.getMessage(MessageCatalogCodeEnum.FUN_059.getCode()).formatted(properties.getProvider());
    }

    private ChatRequest buildChatRequest(MessageTranslationRequestData requestData, ProtectedTranslationTexts texts)
            throws JsonProcessingException {
        var translatedTypeField = catalogPort.getMessage(MessageCatalogCodeEnum.FUN_203.getCode());
        var translatedCategoryField = catalogPort.getMessage(MessageCatalogCodeEnum.FUN_204.getCode());
        var translatedFunctionalityField = catalogPort.getMessage(MessageCatalogCodeEnum.FUN_205.getCode());
        var responseFormat = ResponseFormat.builder()
                .type(ResponseFormatType.JSON)
                .jsonSchema(JsonSchema.builder()
                        .name(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_055.getCode()))
                        .rootElement(JsonObjectSchema.builder()
                                .addStringProperty(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_056.getCode()))
                                .addStringProperty(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_057.getCode()))
                                .addStringProperty(translatedTypeField)
                                .addStringProperty(translatedCategoryField)
                                .addStringProperty(translatedFunctionalityField)
                                .required(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_056.getCode()),
                                        catalogPort.getMessage(MessageCatalogCodeEnum.FUN_057.getCode()),
                                        translatedTypeField, translatedCategoryField, translatedFunctionalityField)
                                .build())
                        .build())
                .build();

        return ChatRequest.builder()
                .messages(SystemMessage.from(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_201.getCode())),
                        UserMessage.from(buildPrompt(requestData, texts)))
                .responseFormat(responseFormat)
                .build();
    }

    /**
     * El texto de los campos solo viaja dentro del bloque JSON de FUN_202. La plantilla FUN_054 del catálogo
     * actual usa únicamente los cuatro primeros argumentos (contexto); los demás se siguen enviando para que
     * una plantilla anterior que aún esté en Redis no falle por argumentos faltantes.
     */
    private String buildPrompt(MessageTranslationRequestData requestData, ProtectedTranslationTexts texts)
            throws JsonProcessingException {
        return catalogPort.getMessage(MessageCatalogCodeEnum.FUN_054.getCode()).formatted(
                requestData.getCode(),
                requestData.getSourceLanguage(),
                requestData.getTargetLanguage(),
                requestData.getApplication(),
                trim(texts.functionality()),
                trim(texts.type()),
                trim(texts.category()),
                trim(texts.title()),
                trim(texts.content())
        ) + catalogPort.getMessage(MessageCatalogCodeEnum.FUN_202.getCode()).formatted(
                requestData.getTargetLanguage(),
                objectMapper.writeValueAsString(Map.of(
                        "title", trim(texts.title()),
                        "content", trim(texts.content()),
                        "type", trim(texts.type()),
                        "category", trim(texts.category()),
                        "functionality", trim(texts.functionality())
                )));
    }

    private record TranslationModelResponse(String translatedTitle, String translatedContent,
                                            String translatedType, String translatedCategory,
                                            String translatedFunctionality) {
        private boolean isComplete() {
            return translatedTitle != null && translatedContent != null && translatedType != null
                    && translatedCategory != null && translatedFunctionality != null;
        }
    }
}
