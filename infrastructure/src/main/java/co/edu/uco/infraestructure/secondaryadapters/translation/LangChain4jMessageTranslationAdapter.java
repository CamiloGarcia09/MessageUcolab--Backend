package co.edu.uco.infraestructure.secondaryadapters.translation;

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

import static co.edu.uco.crosscutting.helpers.UtilObject.isNullObject;
import static co.edu.uco.crosscutting.helpers.UtilText.isEmptyOrNull;
import static co.edu.uco.crosscutting.helpers.UtilText.trim;

@Component
public class LangChain4jMessageTranslationAdapter implements MessageTranslationPort {

    private final LoggingPort log;
    private final CatalogPort catalogPort;
    private final TranslationAiProperties properties;
    private final ObjectMapper objectMapper;
    private ChatModel chatModel;

    public LangChain4jMessageTranslationAdapter(
            CatalogPort catalogPort, TranslationAiProperties properties,
            ObjectMapper objectMapper, LoggingPortFactory loggerFactory
    ) {
        this.log = loggerFactory.getLogger(LangChain4jMessageTranslationAdapter.class);
        this.catalogPort = catalogPort;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    private static final String PROVIDER_OPEN_AI = "openai";

    @Override
    public MessageTranslationResponseData translate(MessageTranslationRequestData requestData) {
        if (isNullObject(requestData)) {
            throw BusinessException.buildTechnicalException(
                    catalogPort.getMessage(MessageCatalogCodeEnum.FUN_023.getCode()),
                    catalogPort.getMessage(MessageCatalogCodeEnum.TCH_037.getCode())
            );
        }
        if (!properties.isEnabled()) {
            throw BusinessException.buildUserException(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_046.getCode()));
        }
        if (isOpenAiProvider() && isEmptyOrNull(properties.getApiKey())) {
            throw BusinessException.buildUserException(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_047.getCode()));
        }

        var startedAt = System.nanoTime();
        try {
            var response = model().chat(buildChatRequest(requestData));
            var output = response.aiMessage().text();
            var translated = objectMapper.readValue(output, TranslationModelResponse.class);
            if (!translated.isComplete()) {
                throw BusinessException.buildUserException(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_048.getCode()));
            }
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
                    translated.translatedTitle(),
                    translated.translatedContent(),
                    translated.translatedType(),
                    translated.translatedCategory(),
                    translated.translatedFunctionality(),
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

    private ChatRequest buildChatRequest(MessageTranslationRequestData requestData) throws JsonProcessingException {
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
                        UserMessage.from(buildPrompt(requestData)))
                .responseFormat(responseFormat)
                .build();
    }

    private String buildPrompt(MessageTranslationRequestData requestData) throws JsonProcessingException {
        return catalogPort.getMessage(MessageCatalogCodeEnum.FUN_054.getCode()).formatted(
                requestData.getCode(),
                requestData.getSourceLanguage(),
                requestData.getTargetLanguage(),
                requestData.getApplication(),
                requestData.getFunctionality(),
                requestData.getType(),
                requestData.getCategory(),
                trim(requestData.getTitle()),
                trim(requestData.getContent())
        ) + catalogPort.getMessage(MessageCatalogCodeEnum.FUN_202.getCode()).formatted(
                requestData.getTargetLanguage(),
                objectMapper.writeValueAsString(Map.of(
                        "title", trim(requestData.getTitle()),
                        "content", trim(requestData.getContent()),
                        "type", trim(requestData.getType()),
                        "category", trim(requestData.getCategory()),
                        "functionality", trim(requestData.getFunctionality())
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
