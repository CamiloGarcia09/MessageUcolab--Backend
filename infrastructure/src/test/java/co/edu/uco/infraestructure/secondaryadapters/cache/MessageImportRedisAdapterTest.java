package co.edu.uco.infraestructure.secondaryadapters.cache;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.domain.message.importing.MessageImportPreview;
import co.edu.uco.crosscutting.exceptions.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import java.time.Instant;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageImportRedisAdapterTest {
    @Mock private StringRedisTemplate redis;
    @Mock private ValueOperations<String, String> values;
    @Mock private ObjectMapper json;
    @Mock private CatalogPort catalog;

    @Test
    void save_persistsPreviewWithApplicationScopedKeyAndRemainingTtl() throws Exception {
        var preview = preview(Instant.now().plusSeconds(1800));
        when(redis.opsForValue()).thenReturn(values);
        when(json.writeValueAsString(preview)).thenReturn("serialized-preview");

        new MessageImportRedisAdapter(redis, json, catalog).save(preview);

        verify(values).set(eq("message-import:v1:app:import"), eq("serialized-preview"), any(java.time.Duration.class));
    }

    @Test
    void save_doesNotStoreAlreadyExpiredPreview() {
        var adapter = new MessageImportRedisAdapter(redis, json, catalog);

        adapter.save(preview(Instant.now().minusSeconds(1)));

        verify(redis, never()).opsForValue();
    }

    @Test
    void save_wrapsSerializationFailureAsTechnicalBusinessException() throws Exception {
        var preview = preview(Instant.now().plusSeconds(60));
        when(json.writeValueAsString(preview)).thenThrow(new JsonProcessingException("serialization") { });
        when(catalog.getMessage("TCH_079")).thenReturn("Import storage unavailable");

        assertThatThrownBy(() -> new MessageImportRedisAdapter(redis, json, catalog).save(preview))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getHttpStatus()).isEqualTo(500));
        verify(values, never()).set(any(), any(), any(java.time.Duration.class));
    }

    @Test
    void find_returnsEmptyWhenDraftKeyIsAbsent() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("message-import:v1:app:import")).thenReturn(null);

        assertThat(new MessageImportRedisAdapter(redis, json, catalog).find("import", "app")).isEmpty();
    }

    private static MessageImportPreview preview(Instant expiry) {
        return new MessageImportPreview("import", "app", List.of(), List.of(), expiry);
    }
}
