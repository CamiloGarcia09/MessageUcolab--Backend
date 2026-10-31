package co.edu.uco.infraestructure.secondaryadapters.cache;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.MessageImportDraftStore;
import co.edu.uco.application.usecase.domain.message.importing.MessageImportPreview;
import co.edu.uco.crosscutting.exceptions.BusinessException;
import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionLocation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.TCH_079;

@Repository
public class MessageImportRedisAdapter implements MessageImportDraftStore {
    private final StringRedisTemplate redis;
    private final ObjectMapper json;
    private final CatalogPort catalog;

    public MessageImportRedisAdapter(StringRedisTemplate redis, ObjectMapper json, CatalogPort catalog) {
        this.redis = redis;
        this.json = json;
        this.catalog = catalog;
    }

    @Override
    public void save(MessageImportPreview preview) {
        var remaining = Duration.between(Instant.now(), preview.expiresAt());
        if (remaining.isNegative() || remaining.isZero()) {
            return;
        }
        try {
            redis.opsForValue().set(key(preview.importId(), preview.applicationId()), json.writeValueAsString(preview), remaining);
        } catch (JsonProcessingException | DataAccessException ex) {
            throw failure(ex);
        }
    }

    @Override
    public Optional<MessageImportPreview> find(String importId, String applicationId) {
        try {
            var value = redis.opsForValue().get(key(importId, applicationId));
            return value == null ? Optional.empty() : Optional.of(json.readValue(value, MessageImportPreview.class));
        } catch (JsonProcessingException | DataAccessException ex) {
            throw failure(ex);
        }
    }

    private String key(String importId, String applicationId) {
        return "message-import:v1:" + applicationId + ":" + importId;
    }

    private BusinessException failure(Exception ex) {
        return BusinessException.buildTechnicalException(catalog.getMessage(TCH_079.getCode()), ex, ExceptionLocation.INFRASTRUCTURE);
    }
}
