package co.edu.uco.infraestructure.secondaryadapters.translation;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.entity.MessageTranslationRequestData;
import co.edu.uco.application.secondaryports.entity.MessageTranslationResponseData;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.crosscutting.exceptions.BusinessException;
import co.edu.uco.infraestructure.config.TranslationAiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.databind.JsonNode;

@ExtendWith(MockitoExtension.class)
class LangChain4jMessageTranslationAdapterTest {

    @Mock
    private CatalogPort catalogPort;
    @Mock
    private LoggingPortFactory loggerFactory;
    @Mock
    private LoggingPort log;
    @Mock
    private ChatResponse chatResponse;
    @Mock
    private AiMessage aiMessage;

    private final TranslationAiProperties properties = new TranslationAiProperties();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private LangChain4jMessageTranslationAdapter adapter;
    private ChatRequest capturedChatRequest;

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(LangChain4jMessageTranslationAdapter.class)).thenReturn(log);
        adapter = new LangChain4jMessageTranslationAdapter(catalogPort, properties, objectMapper, loggerFactory);
    }

    private MessageTranslationRequestData request() {
        return MessageTranslationRequestData.create(
                "MSG-001", "en", "es", "Hello", "World", "TEXT", "GENERAL", "app", "translation"
        );
    }

    private void injectChatModel(ChatModel model) throws Exception {
        Field field = LangChain4jMessageTranslationAdapter.class.getDeclaredField("chatModel");
        field.setAccessible(true);
        field.set(adapter, model);
    }

    private ChatModel chatModelReturning(ChatResponse response) {
        return new ChatModel() {
            @Override
            public ChatResponse chat(ChatRequest request) {
                capturedChatRequest = request;
                return response;
            }
        };
    }

    private ChatModel chatModelThrowing() {
        return new ChatModel() {
            @Override
            public ChatResponse chat(ChatRequest request) {
                throw new RuntimeException("model down");
            }
        };
    }

    private void stubSchemaMessages() {
        when(catalogPort.getMessage("FUN_054")).thenReturn("%s|%s|%s|%s|%s|%s|%s|%s|%s");
        when(catalogPort.getMessage("FUN_201")).thenReturn("catalog system instructions");
        when(catalogPort.getMessage("FUN_202")).thenReturn("\n\nTexts to translate into %s:\n%s");
        when(catalogPort.getMessage("FUN_055")).thenReturn("message");
        when(catalogPort.getMessage("FUN_056")).thenReturn("translatedTitle");
        when(catalogPort.getMessage("FUN_057")).thenReturn("translatedContent");
        when(catalogPort.getMessage("FUN_203")).thenReturn("translatedType");
        when(catalogPort.getMessage("FUN_204")).thenReturn("translatedCategory");
        when(catalogPort.getMessage("FUN_205")).thenReturn("translatedFunctionality");
    }

    @Test
    void translate_throwsBusinessException_whenTranslationDisabled() {
        properties.setEnabled(false);
        when(catalogPort.getMessage("FUN_046")).thenReturn("translation disabled");

        MessageTranslationRequestData req = request();
        assertThatThrownBy(() -> adapter.translate(req))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getUserMessage()).isEqualTo("translation disabled"));
    }

    @Test
    void translate_throwsBusinessException_whenOpenAiProviderWithoutApiKey() {
        properties.setEnabled(true);
        properties.setProvider("openai");
        properties.setApiKey(null);
        when(catalogPort.getMessage("FUN_047")).thenReturn("api key missing");

        MessageTranslationRequestData req = request();
        assertThatThrownBy(() -> adapter.translate(req))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getUserMessage()).isEqualTo("api key missing"));
    }

    @Test
    void translate_returnsTranslatedData_whenOpenAiProvider() throws Exception {
        properties.setEnabled(true);
        properties.setProvider("openai");
        properties.setApiKey("sk-test");
        properties.setModelName("gpt-4o-mini");
        injectChatModel(chatModelReturning(chatResponse));
        stubSchemaMessages();

        when(chatResponse.aiMessage()).thenReturn(aiMessage);
        when(aiMessage.text()).thenReturn("{\"translatedTitle\":\"Hola\",\"translatedContent\":\"Mundo\","
                + "\"translatedType\":\"Texto\",\"translatedCategory\":\"General\","
                + "\"translatedFunctionality\":\"Traduccion\"}");
        when(catalogPort.getMessage("FUN_058")).thenReturn("OpenAI");
        when(catalogPort.getMessage("TCH_036")).thenReturn("translated");

        MessageTranslationResponseData result = adapter.translate(request());
        SystemMessage systemMessage = (SystemMessage) capturedChatRequest.messages().get(0);
        UserMessage promptMessage = (UserMessage) capturedChatRequest.messages().get(1);
        JsonObjectSchema schema = (JsonObjectSchema) capturedChatRequest.responseFormat().jsonSchema().rootElement();
        String prompt = promptMessage.singleText();
        int sourceJsonStart = prompt.indexOf('{', prompt.indexOf("Texts to translate into es:"));
        JsonNode sourceTexts = objectMapper.readTree(prompt.substring(sourceJsonStart));

        assertAll(
                () -> assertThat(result.getTranslatedTitle()).isEqualTo("Hola"),
                 () -> assertThat(result.getTranslatedContent()).isEqualTo("Mundo"),
                 () -> assertThat(result.getTranslatedType()).isEqualTo("Texto"),
                 () -> assertThat(result.getTranslatedCategory()).isEqualTo("General"),
                 () -> assertThat(result.getTranslatedFunctionality()).isEqualTo("Traduccion"),
                () -> assertThat(result.getProvider()).isEqualTo("OpenAI"),
                () -> assertThat(result.getModel()).isEqualTo("gpt-4o-mini"),
                () -> assertThat(result.getElapsedMillis()).isGreaterThanOrEqualTo(0),
                () -> assertThat(schema.properties().keySet()).containsExactlyInAnyOrder(
                        "translatedTitle", "translatedContent", "translatedType", "translatedCategory",
                        "translatedFunctionality"),
                 () -> assertThat(schema.required()).containsExactlyInAnyOrder(
                        "translatedTitle", "translatedContent", "translatedType", "translatedCategory",
                        "translatedFunctionality"),
                  () -> assertThat(systemMessage.text()).isEqualTo("catalog system instructions"),
                 () -> assertThat(prompt).contains("MSG-001", "en", "es", "app",
                         "translation", "TEXT", "GENERAL", "Hello", "World", "Texts to translate into es:"),
                 () -> assertThat(sourceTexts.fieldNames()).toIterable().containsExactlyInAnyOrder(
                         "title", "content", "type", "category", "functionality"),
                 () -> assertThat(sourceTexts.get("title").asText()).isEqualTo("Hello"),
                 () -> assertThat(sourceTexts.get("content").asText()).isEqualTo("World"),
                 () -> assertThat(sourceTexts.get("type").asText()).isEqualTo("TEXT"),
                 () -> assertThat(sourceTexts.get("category").asText()).isEqualTo("GENERAL"),
                 () -> assertThat(sourceTexts.get("functionality").asText()).isEqualTo("translation"));
        verify(log).info(anyString(), anyString(), anyString(), anyString(), anyString(), anyLong());
    }

    @Test
    void translate_returnsTranslatedData_whenOllamaProvider() throws Exception {
        properties.setEnabled(true);
        properties.setProvider("ollama");
        properties.setModelName("llama3");
        injectChatModel(chatModelReturning(chatResponse));
        stubSchemaMessages();

        when(chatResponse.aiMessage()).thenReturn(aiMessage);
        when(aiMessage.text()).thenReturn("{\"translatedTitle\":\"Hola\",\"translatedContent\":\"Mundo\","
                + "\"translatedType\":\"Texto\",\"translatedCategory\":\"General\","
                + "\"translatedFunctionality\":\"Traduccion\"}");
        when(catalogPort.getMessage("FUN_059")).thenReturn("ollama: %s");
        when(catalogPort.getMessage("TCH_036")).thenReturn("translated");

        MessageTranslationResponseData result = adapter.translate(request());

        assertAll(
                () -> assertThat(result.getTranslatedTitle()).isEqualTo("Hola"),
                 () -> assertThat(result.getTranslatedContent()).isEqualTo("Mundo"),
                 () -> assertThat(result.getTranslatedType()).isEqualTo("Texto"),
                 () -> assertThat(result.getTranslatedCategory()).isEqualTo("General"),
                 () -> assertThat(result.getTranslatedFunctionality()).isEqualTo("Traduccion"),
                () -> assertThat(result.getProvider()).isEqualTo("ollama: ollama"),
                () -> assertThat(result.getModel()).isEqualTo("llama3"));
        verify(log).info(anyString(), anyString(), anyString(), anyString(), anyString(), anyLong());
    }

    @Test
    void translate_logsAndThrows_whenModelCallFails() throws Exception {
        properties.setEnabled(true);
        properties.setProvider("openai");
        properties.setApiKey("sk-test");
        properties.setModelName("gpt-4o-mini");
        injectChatModel(chatModelThrowing());
        stubSchemaMessages();

        when(catalogPort.getMessage("FUN_058")).thenReturn("OpenAI");
        when(catalogPort.getMessage("FUN_048")).thenReturn("translation failed");
        when(catalogPort.getMessage("TCH_037")).thenReturn("tch037");

        MessageTranslationRequestData req = request();
        assertThatThrownBy(() -> adapter.translate(req))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getUserMessage()).isEqualTo("translation failed"));
        verify(log).error(anyString(), anyString(), anyString(), anyString(), anyString(), any(RuntimeException.class));
    }

    @Test
    void translate_usesCatalogTemplatesAndSchemaFieldNames() throws Exception {
        properties.setEnabled(true);
        properties.setProvider("ollama");
        properties.setModelName("llama3");
        injectChatModel(chatModelReturning(chatResponse));
        stubSchemaMessages();
        when(catalogPort.getMessage("FUN_202")).thenReturn("CUSTOM[%s] %s");
        when(catalogPort.getMessage("FUN_203")).thenReturn("typeFromCatalog");
        when(catalogPort.getMessage("FUN_204")).thenReturn("categoryFromCatalog");
        when(catalogPort.getMessage("FUN_205")).thenReturn("functionalityFromCatalog");
        when(chatResponse.aiMessage()).thenReturn(aiMessage);
        when(aiMessage.text()).thenReturn("{\"translatedTitle\":\"T\",\"translatedContent\":\"C\","
                + "\"translatedType\":\"Ty\",\"translatedCategory\":\"Ca\","
                + "\"translatedFunctionality\":\"Fu\"}");
        when(catalogPort.getMessage("FUN_059")).thenReturn("ollama: %s");
        when(catalogPort.getMessage("TCH_036")).thenReturn("translated");
        adapter.translate(request());
        UserMessage prompt = (UserMessage) capturedChatRequest.messages().get(1);
        JsonObjectSchema schema = (JsonObjectSchema) capturedChatRequest.responseFormat().jsonSchema().rootElement();

        assertAll(
                () -> assertThat(prompt.singleText()).contains("CUSTOM[es]", "\"title\":\"Hello\""),
                () -> assertThat(schema.properties().keySet()).contains("typeFromCatalog", "categoryFromCatalog",
                        "functionalityFromCatalog"),
                () -> assertThat(schema.required()).contains("typeFromCatalog", "categoryFromCatalog",
                        "functionalityFromCatalog"));
    }

    @Test
    void translate_rejectsIncompleteModelResponse() throws Exception {
        properties.setEnabled(true);
        properties.setProvider("ollama");
        properties.setModelName("llama3");
        injectChatModel(chatModelReturning(chatResponse));
        stubSchemaMessages();
        when(chatResponse.aiMessage()).thenReturn(aiMessage);
        when(aiMessage.text()).thenReturn("{\"translatedTitle\":\"Hola\",\"translatedContent\":\"Mundo\","
                + "\"translatedType\":\"Texto\",\"translatedCategory\":\"General\"}");
        when(catalogPort.getMessage("FUN_059")).thenReturn("ollama: %s");
        when(catalogPort.getMessage("FUN_048")).thenReturn("translation failed");
        when(catalogPort.getMessage("TCH_037")).thenReturn("technical error");

        assertThatThrownBy(() -> adapter.translate(request()))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getUserMessage())
                        .isEqualTo("translation failed"));
    }
}
