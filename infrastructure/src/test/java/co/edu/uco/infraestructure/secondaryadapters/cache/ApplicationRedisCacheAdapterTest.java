package co.edu.uco.infraestructure.secondaryadapters.cache;

import co.edu.uco.infraestructure.secondaryadapters.repository.redis.RedisRepositoryAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationRedisCacheAdapterTest {
    @Mock RedisRepositoryAdapter messages;
    @Mock StringRedisTemplate redis;
    @Mock Cursor<String> cursor;

    @Test
    void evict_deletesMessageHashesAndOnlyApplicationSpecificCacheNamespaces() {
        var app = UUID.randomUUID();
        var messageIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        when(redis.scan(any(ScanOptions.class))).thenReturn(cursor);
        when(cursor.hasNext()).thenReturn(false);

        new ApplicationRedisCacheAdapter(messages, redis).evict(app, messageIds);

        verify(messages).deleteAllById(messageIds);
        var options = ArgumentCaptor.forClass(ScanOptions.class);
        verify(redis, times(3)).scan(options.capture());
        assertThat(options.getAllValues()).extracting(ScanOptions::getPattern).containsExactly(
                "translation-v3::*::" + app + "::*",
                "translation-v3::*::consumer-v1:" + app + ":*",
                "message-import:v1:" + app + ":*");
        verify(cursor, times(3)).close();
        verify(redis, never()).delete(any(String.class));
    }

    @Test
    void evict_batchesScannedKeysForTargetApplicationOnly() {
        var app = UUID.randomUUID();
        when(redis.scan(any(ScanOptions.class))).thenReturn(cursor);
        var keyList = java.util.stream.IntStream.range(0, 201)
                .mapToObj(index -> "translation-v3:en:" + app + ":MSG_" + index).toList();
        var iterator = keyList.iterator();
        var batches = new java.util.ArrayList<List<String>>();
        doAnswer(invocation -> {
            batches.add(List.copyOf(invocation.getArgument(0)));
            return 1L;
        }).when(redis).delete(any(List.class));
        when(cursor.hasNext()).thenAnswer(invocation -> iterator.hasNext());
        when(cursor.next()).thenAnswer(invocation -> iterator.next());

        new ApplicationRedisCacheAdapter(messages, redis).evict(app, List.of());

        assertThat(batches).hasSize(2);
        assertThat(batches.get(0)).hasSize(200);
        assertThat(batches.get(1)).containsExactly("translation-v3:en:" + app + ":MSG_200");
        verify(redis, times(2)).delete(any(List.class));
        verify(messages).deleteAllById(List.of());
    }
}
