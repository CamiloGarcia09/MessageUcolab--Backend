package co.edu.uco.infraestructure.secondaryadapters.cache;

import co.edu.uco.application.secondaryports.cache.ApplicationCachePort;
import co.edu.uco.infraestructure.secondaryadapters.repository.redis.RedisRepositoryAdapter;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public final class ApplicationRedisCacheAdapter implements ApplicationCachePort {
    private static final int BATCH_SIZE = 200;
    private final RedisRepositoryAdapter messages;
    private final StringRedisTemplate redis;

    public ApplicationRedisCacheAdapter(RedisRepositoryAdapter messages, StringRedisTemplate redis) {
        this.messages = messages;
        this.redis = redis;
    }

    @Override
    public void evict(UUID applicationId, List<UUID> messageIds) {
        // Use repository deletion so secondary indexes are removed with each message hash.
        messages.deleteAllById(messageIds);
        deleteMatching("translation-v3::*::" + applicationId + "::*");
        deleteMatching("translation-v3::*::consumer-v1:" + applicationId + ":*");
        deleteMatching("message-import:v1:" + applicationId + ":*");
    }

    private void deleteMatching(String pattern) {
        // SCAN avoids blocking the shared Redis instance; never use FLUSHDB or broad application-free patterns.
        try (var cursor = redis.scan(ScanOptions.scanOptions().match(pattern).count(BATCH_SIZE).build())) {
            List<String> batch = new ArrayList<>(BATCH_SIZE);
            while (cursor.hasNext()) {
                batch.add(cursor.next());
                if (batch.size() == BATCH_SIZE) {
                    redis.delete(batch);
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) {
                redis.delete(batch);
            }
        }
    }
}
