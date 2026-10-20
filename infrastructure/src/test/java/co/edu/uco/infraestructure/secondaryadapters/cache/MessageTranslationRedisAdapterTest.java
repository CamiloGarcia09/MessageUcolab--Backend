package co.edu.uco.infraestructure.secondaryadapters.cache;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.entity.MessageTranslationResponseData;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageTranslationRedisAdapterTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private CatalogPort catalogPort;
    @Mock
    private LoggingPortFactory loggerFactory;
    @Mock
    private LoggingPort log;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MessageTranslationRedisAdapter adapter;

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(MessageTranslationRedisAdapter.class)).thenReturn(log);
        adapter = new MessageTranslationRedisAdapter(redisTemplate, catalogPort, objectMapper, loggerFactory);
    }

    @Test
    void findTranslation_returnsEmpty_whenMessageCodeIsNull() {
        assertThat(adapter.findTranslation(null, "env-1", "en", "es")).isEmpty();
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void findTranslation_returnsEmpty_whenEnvironmentIdIsNull() {
        assertThat(adapter.findTranslation("MSG-001", null, "en", "es")).isEmpty();
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void findTranslation_returnsEmpty_whenSourceLanguageIsEmpty() {
        assertThat(adapter.findTranslation("MSG-001", "env-1", "", "es")).isEmpty();
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void findTranslation_returnsEmpty_whenTargetLanguageIsEmpty() {
        assertThat(adapter.findTranslation("MSG-001", "env-1", "en", null)).isEmpty();
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void findTranslation_returnsEmpty_whenJsonNotFound() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        assertThat(adapter.findTranslation("MSG-001", "env-1", "en", "es")).isEmpty();
    }

    @Test
    void findTranslation_usesVersionedKey_andRejectsIncompleteLegacyJson() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("translation-v3::MSG-001::env-1::en::es"))
                .thenReturn("{\"translatedTitle\":\"old\",\"translatedContent\":\"old content\"}");

        Optional<MessageTranslationResponseData> result =
                adapter.findTranslation("MSG-001", "env-1", "en", "es");

        assertThat(result).isEmpty();
        verify(valueOperations).get("translation-v3::MSG-001::env-1::en::es");
    }

    @Test
    void findTranslation_returnsTranslation_whenJsonFound() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(
                "{\"translatedTitle\":\"Titulo\",\"translatedContent\":\"Contenido\","
                        + "\"translatedType\":\"tipo\",\"translatedCategory\":\"categoria\","
                        + "\"translatedFunctionality\":\"funcionalidad\","
                        + "\"provider\":\"ollama\",\"model\":\"llama3\"}");

        Optional<MessageTranslationResponseData> result =
                adapter.findTranslation("MSG-001", "env-1", "en", "es");

        assertThat(result).isPresent();
        assertAll(
                () -> assertThat(result.get().getTranslatedTitle()).isEqualTo("Titulo"),
                 () -> assertThat(result.get().getTranslatedContent()).isEqualTo("Contenido"),
                 () -> assertThat(result.get().getTranslatedType()).isEqualTo("tipo"),
                 () -> assertThat(result.get().getTranslatedCategory()).isEqualTo("categoria"),
                 () -> assertThat(result.get().getTranslatedFunctionality()).isEqualTo("funcionalidad"),
                () -> assertThat(result.get().getProvider()).isEqualTo("ollama"),
                () -> assertThat(result.get().getModel()).isEqualTo("llama3"),
                () -> assertThat(result.get().getElapsedMillis()).isZero());
    }

    @Test
    void findTranslation_returnsEmpty_whenDataAccessException() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_014.getCode())).thenReturn("cache error");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenThrow(new DataAccessException("redis down") {
        });

        assertThat(adapter.findTranslation("MSG-001", "env-1", "en", "es")).isEmpty();
        verify(log).error(eq("cache error"), any(DataAccessException.class));
    }

    @Test
    void findTranslation_returnsEmpty_whenJsonIsInvalid() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_015.getCode())).thenReturn("parse error");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("not-a-json");

        assertThat(adapter.findTranslation("MSG-001", "env-1", "en", "es")).isEmpty();
        verify(log).error(eq("parse error"), any(Exception.class));
    }

    @Test
    void saveTranslation_doesNothing_whenMessageCodeIsNull() {
        adapter.saveTranslation(null, "env-1", "en", "es", MessageTranslationResponseData.create(
                "T", "C", "TT", "TC", "TF", "p", "m", 10L));

        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void saveTranslation_doesNothing_whenTranslationIsNull() {
        adapter.saveTranslation("MSG-001", "env-1", "en", "es", null);

        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void saveTranslation_savesJson() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        adapter.saveTranslation("MSG-001", "env-1", "en", "es", MessageTranslationResponseData.create(
                "Titulo", "Contenido", "tipo", "categoria", "funcionalidad", "ollama", "llama3", 10L));

        org.mockito.ArgumentCaptor<String> keyCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.ArgumentCaptor<String> jsonCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(keyCaptor.capture(), jsonCaptor.capture(), any(Duration.class));
        assertAll(
                () -> assertThat(keyCaptor.getValue()).isEqualTo("translation-v3::MSG-001::env-1::en::es"),
                () -> assertThat(jsonCaptor.getValue()).contains("\"translatedType\":\"tipo\"",
                        "\"translatedCategory\":\"categoria\"", "\"translatedFunctionality\":\"funcionalidad\""),
                () -> assertThat(jsonCaptor.getValue()).doesNotContain("application", "\"type\"", "\"category\"",
                        "\"functionality\""));
    }

    @Test
    void translation_roundTripsThroughRedisJson_withFiveFieldsAndProviderMetadata() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        MessageTranslationResponseData response = MessageTranslationResponseData.create(
                "Título", "Contenido", "Tipo traducido", "Categoría traducida",
                "Funcionalidad traducida", "provider-x", "model-y", 345L);

        adapter.saveTranslation("MSG-001", "env-1", "es", "en", response);

        org.mockito.ArgumentCaptor<String> jsonCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq("translation-v3::MSG-001::env-1::es::en"),
                jsonCaptor.capture(), any(Duration.class));
        when(valueOperations.get("translation-v3::MSG-001::env-1::es::en"))
                .thenReturn(jsonCaptor.getValue());

        Optional<MessageTranslationResponseData> restored =
                adapter.findTranslation("MSG-001", "env-1", "es", "en");

        assertThat(restored).isPresent();
        assertAll(
                () -> assertThat(restored.orElseThrow().getTranslatedTitle()).isEqualTo("Título"),
                () -> assertThat(restored.orElseThrow().getTranslatedContent()).isEqualTo("Contenido"),
                () -> assertThat(restored.orElseThrow().getTranslatedType()).isEqualTo("Tipo traducido"),
                () -> assertThat(restored.orElseThrow().getTranslatedCategory()).isEqualTo("Categoría traducida"),
                () -> assertThat(restored.orElseThrow().getTranslatedFunctionality())
                        .isEqualTo("Funcionalidad traducida"),
                () -> assertThat(restored.orElseThrow().getProvider()).isEqualTo("provider-x"),
                () -> assertThat(restored.orElseThrow().getModel()).isEqualTo("model-y"),
                () -> assertThat(restored.orElseThrow().getElapsedMillis()).isZero());
    }

    @Test
    void saveTranslation_logsError_whenDataAccessException() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_014.getCode())).thenReturn("cache error");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new DataAccessException("redis down") {
        }).when(valueOperations).set(anyString(), anyString(), any(Duration.class));

        adapter.saveTranslation("MSG-001", "env-1", "en", "es", MessageTranslationResponseData.create(
                "Titulo", "Contenido", "tipo", "categoria", "funcionalidad", "ollama", "llama3", 10L));

        verify(log).error(eq("cache error"), any(DataAccessException.class));
    }

    @Test
    void saveTranslation_logsError_whenUnexpectedException() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_015.getCode())).thenReturn("write error");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new RuntimeException("boom")).when(valueOperations)
                .set(anyString(), anyString(), any(Duration.class));

        adapter.saveTranslation("MSG-001", "env-1", "en", "es", MessageTranslationResponseData.create(
                "Titulo", "Contenido", "tipo", "categoria", "funcionalidad", "ollama", "llama3", 10L));

        verify(log).error(eq("write error"), any(RuntimeException.class));
    }
}
