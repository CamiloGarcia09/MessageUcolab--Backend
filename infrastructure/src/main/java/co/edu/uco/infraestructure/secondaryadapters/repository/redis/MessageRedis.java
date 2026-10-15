package co.edu.uco.infraestructure.secondaryadapters.repository.redis;

import co.edu.uco.crosscutting.helpers.UtilUUID;
import jakarta.persistence.Id;
import lombok.Getter;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.util.UUID;
import java.time.Instant;

import static co.edu.uco.crosscutting.helpers.UtilText.EMPTY;
import static co.edu.uco.crosscutting.helpers.UtilText.trim;
import static co.edu.uco.crosscutting.helpers.UtilUUID.getDefaultUUID;

import static co.edu.uco.infraestructure.config.InfrastructureConstant.REDIS_HASH;

@Getter
@RedisHash(value = REDIS_HASH, timeToLive = 120)
public final class MessageRedis {
    @Id
    private UUID id;
    @Indexed
    private String code;
    private String title;
    private String content;
    private String type;
    private String category;
    private String status;
    private String application;
    private String functionality;
    private Instant createdAt;
    @Indexed
    private String environmentId;
    public MessageRedis() {
        setId(UtilUUID.getNewUUID());
        setCode(EMPTY);
        setTitle(EMPTY);
        setContent(EMPTY);
        setType(EMPTY);
        setCategory(EMPTY);
        setStatus(EMPTY);
        setApplication(EMPTY);
        setFunctionality(EMPTY);
        setEnvironmentId(EMPTY);
    }
    public MessageRedis(UUID id, String code, String title, String content, String type,
            String category, String status, String application, String functionality, String environmentId) {
        setId(id);
        setCode(code);
        setTitle(title);
        setContent(content);
        setType(type);
        setCategory(category);
        setStatus(status);
        setApplication(application);
        setFunctionality(functionality);
        setEnvironmentId(environmentId);
    }
    public void setId(UUID id) {
        this.id = getDefaultUUID(id);
    }
    public void setCode(String code) {
        this.code = trim(code);
    }
    public void setTitle(String title) {
        this.title = trim(title);
    }
    public void setContent(String content) {
        this.content = trim(content);
    }
    public void setType(String type) {
        this.type = trim(type);
    }
    public void setCategory(String category) {
        this.category = trim(category);
    }
    public void setStatus(String status) {
        this.status = trim(status);
    }
    public void setApplication(String application) {
        this.application = trim(application);
    }
    public void setFunctionality(String functionality) {
        this.functionality = trim(functionality);
    }
    public void setEnvironmentId(String environmentId) {
        this.environmentId = trim(environmentId);
    }
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
