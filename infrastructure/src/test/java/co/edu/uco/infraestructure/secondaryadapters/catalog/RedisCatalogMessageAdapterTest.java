package co.edu.uco.infraestructure.secondaryadapters.catalog;

import co.edu.uco.application.common.catalog.MessageCatalog;
import co.edu.uco.application.crosscutting.exceptions.MessageKeyCanNotBeEmptyException;
import co.edu.uco.application.crosscutting.exceptions.MessageKeyCanNotBeNullException;
import co.edu.uco.application.crosscutting.exceptions.MessageNotFoundException;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.crosscutting.exceptions.CrossWordsException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisCatalogMessageAdapterTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private HashOperations<String, Object, Object> hashOperations;
    @Mock
    private LoggingPortFactory loggerFactory;
    @Mock
    private LoggingPort log;
    @Mock
    private co.edu.uco.application.secondaryports.catalog.CatalogPort staticCatalog;

    private RedisCatalogMessageAdapter adapter;

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(RedisCatalogMessageAdapter.class)).thenReturn(log);
        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        lenient().when(staticCatalog.getMessage(org.mockito.ArgumentMatchers.anyString())).thenReturn("static-msg");
        co.edu.uco.application.common.catalog.CatalogPortStaticRef.set(staticCatalog);
        adapter = new RedisCatalogMessageAdapter(redisTemplate, loggerFactory);
    }

    @AfterEach
    void tearDown() {
        co.edu.uco.application.common.catalog.CatalogPortStaticRef.set(null);
    }

    @Test
    void init_setsStaticCatalogReference() {
        co.edu.uco.application.common.catalog.CatalogPortStaticRef.set(null);
        when(hashOperations.get("k", "content")).thenReturn("X");

        adapter.init();

        assertThat(co.edu.uco.application.common.catalog.CatalogPortStaticRef.getMessage("k")).isEqualTo("X");
    }

    @Test
    void getMessageModel_throws_whenKeyIsEmpty() {
        assertThatThrownBy(() -> adapter.getMessageModel(" "))
                .isInstanceOf(MessageKeyCanNotBeEmptyException.class);
        verify(hashOperations, never()).entries(anyString());
    }

    @Test
    void getMessageModel_throws_whenKeyIsNull() {
        assertThatThrownBy(() -> adapter.getMessageModel(null))
                .isInstanceOf(MessageKeyCanNotBeNullException.class);
        verify(hashOperations, never()).entries(anyString());
    }

    @Test
    void getMessageModel_returnsMessageCatalog_whenEntryExists() {
        Map<Object, Object> entry = new HashMap<>();
        entry.put("code", "CODE");
        entry.put("title", "Title");
        entry.put("content", "Content");
        entry.put("type", "TYPE");
        entry.put("category", "CAT");
        when(hashOperations.entries("msg-key")).thenReturn(entry);

        MessageCatalog catalog = adapter.getMessageModel("msg-key");

        assertThat(catalog.code()).isEqualTo("CODE");
        assertThat(catalog.title()).isEqualTo("Title");
        assertThat(catalog.content()).isEqualTo("Content");
        assertThat(catalog.type()).isEqualTo("TYPE");
        assertThat(catalog.category()).isEqualTo("CAT");
    }

    @Test
    void getMessageModel_returnsBootstrapModel_whenKeyIsBootstrapCode() {
        MessageCatalog catalog = adapter.getMessageModel("TCH_021");

        assertThat(catalog.code()).isEqualTo("TCH_021");
        assertThat(catalog.title()).isEqualTo("Respuesta correcta");
        assertThat(catalog.content()).isEqualTo("La respuesta correcta es: {}");
        assertThat(catalog.type()).isEqualTo("TECHNICAL");
        assertThat(catalog.category()).isEqualTo("INFORMATION");
        verify(hashOperations, never()).entries(anyString());
    }

    @Test
    void getMessageModel_throws_whenEntryIsEmpty() {
        when(hashOperations.entries("msg-key")).thenReturn(new HashMap<>());

        assertThatThrownBy(() -> adapter.getMessageModel("msg-key"))
                .isInstanceOf(MessageNotFoundException.class);
    }

    @Test
    void getMessageModel_throws_whenEntryIsNull() {
        when(hashOperations.entries("msg-key")).thenReturn(null);

        assertThatThrownBy(() -> adapter.getMessageModel("msg-key"))
                .isInstanceOf(MessageNotFoundException.class);
    }

    @Test
    void getMessageModel_throwsAndLogs_whenRedisThrows() {
        when(redisTemplate.opsForHash()).thenThrow(new RuntimeException("redis down"));

        assertThatThrownBy(() -> adapter.getMessageModel("msg-key"))
                .isInstanceOf(CrossWordsException.class);
        verify(log).error(anyString(), any(Exception.class));
    }

    @Test
    void getMessage_returnsEmpty_whenKeyIsEmpty() {
        assertThat(adapter.getMessage(" ")).isEmpty();
    }

    @Test
    void getMessage_returnsValue_whenPresent() {
        when(hashOperations.get("msg-key", "content")).thenReturn("Content");

        assertThat(adapter.getMessage("msg-key")).isEqualTo("Content");
    }

    @Test
    void getMessage_returnsBootstrapContent_whenKeyIsBootstrapCode() {
        assertThat(adapter.getMessage("TCH_007")).isEqualTo("La clave del mensaje es nula.");
        verify(hashOperations, never()).get(anyString(), any());
    }

    @Test
    void getMessage_returnsKey_whenValueIsNull() {
        when(hashOperations.get("msg-key", "content")).thenReturn(null);

        assertThat(adapter.getMessage("msg-key")).isEqualTo("msg-key");
    }

    @Test
    void getMessage_returnsKey_whenRedisThrows() {
        when(redisTemplate.opsForHash()).thenThrow(new RuntimeException("redis down"));

        assertThat(adapter.getMessage("msg-key")).isEqualTo("msg-key");
        verify(log).error(anyString(), any(Exception.class));
    }

    @Test
    void getMessageWithDefault_returnsDefault_whenKeyIsEmpty() {
        assertThat(adapter.getMessage(" ", "default")).isEqualTo("default");
    }

    @Test
    void getMessageWithDefault_returnsValue_whenPresent() {
        when(hashOperations.get("msg-key", "content")).thenReturn("Content");

        assertThat(adapter.getMessage("msg-key", "default")).isEqualTo("Content");
    }

    @Test
    void getMessageWithDefault_returnsBootstrapContent_whenKeyIsBootstrapCode() {
        assertThat(adapter.getMessage("TCH_009", "default"))
                .isEqualTo("No existe un código de mensaje con la clave %s.");
        verify(hashOperations, never()).get(anyString(), any());
    }

    @Test
    void getMessageWithDefault_returnsDefault_whenValueIsNull() {
        when(hashOperations.get("msg-key", "content")).thenReturn(null);

        assertThat(adapter.getMessage("msg-key", "default")).isEqualTo("default");
    }

    @Test
    void getMessageWithDefault_returnsDefault_whenRedisThrows() {
        when(redisTemplate.opsForHash()).thenThrow(new RuntimeException("redis down"));

        assertThat(adapter.getMessage("msg-key", "default")).isEqualTo("default");
        verify(log).error(anyString(), any(Exception.class));
    }

    @Test
    void getTitle_returnsEmpty_whenKeyIsEmpty() {
        assertThat(adapter.getTitle(" ")).isEmpty();
    }

    @Test
    void getTitle_returnsTitle_whenPresent() {
        when(hashOperations.get("msg-key", "title")).thenReturn("Title");

        assertThat(adapter.getTitle("msg-key")).isEqualTo("Title");
    }

    @Test
    void getTitle_returnsBootstrapTitle_whenKeyIsBootstrapCode() {
        assertThat(adapter.getTitle("TCH_021")).isEqualTo("Respuesta correcta");
        verify(hashOperations, never()).get(anyString(), any());
    }

    @Test
    void getTitle_returnsEmpty_whenValueIsNull() {
        when(hashOperations.get("msg-key", "title")).thenReturn(null);

        assertThat(adapter.getTitle("msg-key")).isEmpty();
    }

    @Test
    void setMessage_doesNothing_whenKeyIsEmptyOrMessageIsNull() {
        adapter.setMessage(" ", new MessageCatalog("c", "t", "x", "y", "z"));
        adapter.setMessage("key", null);

        verify(hashOperations, never()).putAll(anyString(), any(Map.class));
    }

    @Test
    void setMessage_storesMessageFields() {
        MessageCatalog message = new MessageCatalog("CODE", "Title", "Content", "TYPE", "CAT");

        adapter.setMessage("msg-key", message);

        verify(hashOperations).putAll(eq("msg-key"), any(Map.class));
    }

    @Test
    void setMessage_storesEmptyValues_whenMessageFieldsAreNull() {
        adapter.setMessage("msg-key", new MessageCatalog(null, null, null, null, null));

        verify(hashOperations).putAll(eq("msg-key"), any(Map.class));
    }
}
