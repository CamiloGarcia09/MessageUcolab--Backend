package co.edu.uco.infraestructure.secondaryadapters.cache;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.entity.MessageTranslationResponseData;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.translation.MessageTranslationCachePort;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import static co.edu.uco.crosscutting.helpers.UtilObject.isNullObject;
import static co.edu.uco.crosscutting.helpers.UtilText.isEmptyOrNull;
import static co.edu.uco.crosscutting.helpers.UtilText.trim;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.TRANSLATION_CACHE_KEY_SEPARATOR;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.TRANSLATION_CACHE_REDIS_ADAPTER;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.TRANSLATION_CACHE_TTL_SECONDS;

@Component(TRANSLATION_CACHE_REDIS_ADAPTER)
public final class MessageTranslationRedisAdapter implements MessageTranslationCachePort {
    private static final String FIELD_TRANSLATED_TITLE = "translatedTitle";
    private static final String FIELD_TRANSLATED_CONTENT = "translatedContent";
    private static final String FIELD_TRANSLATED_TYPE = "translatedType";
    private static final String FIELD_TRANSLATED_CATEGORY = "translatedCategory";
    private static final String FIELD_TRANSLATED_FUNCTIONALITY = "translatedFunctionality";
    private static final String CACHE_VERSION = "translation-v3";
    private static final String FIELD_PROVIDER = "provider";
    private static final String FIELD_MODEL = "model";

    private final LoggingPort log;
    private final CatalogPort catalogPort;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public MessageTranslationRedisAdapter(
            RedisTemplate<String, String> redisTemplate,
            CatalogPort catalogPort,
            ObjectMapper objectMapper,
            LoggingPortFactory loggerFactory
    ) {
        this.log = loggerFactory.getLogger(MessageTranslationRedisAdapter.class);
        this.catalogPort = catalogPort;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<MessageTranslationResponseData> findTranslation(
            String messageCode,
            String scopeId,
            String sourceLanguage,
            String targetLanguage
    ) {
        if (isNullObject(messageCode) || isNullObject(scopeId)
                || isEmptyOrNull(sourceLanguage) || isEmptyOrNull(targetLanguage)) {
            return Optional.empty();
        }
        try {
            var json = redisTemplate.opsForValue().get(buildKey(messageCode, scopeId, sourceLanguage, targetLanguage));
            if (isEmptyOrNull(json)) {
                return Optional.empty();
            }
            var node = objectMapper.readTree(json);
            if (!isComplete(node)) {
                return Optional.empty();
            }
            return Optional.of(toResponseData(node));
        } catch (DataAccessException ex) {
            log.error(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_014.getCode()), ex);
            return Optional.empty();
        } catch (Exception ex) {
            log.error(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_015.getCode()), ex);
            return Optional.empty();
        }
    }

    @Override
    public void saveTranslation(
            String messageCode,
            String scopeId,
            String sourceLanguage,
            String targetLanguage,
            MessageTranslationResponseData translation
    ) {
        if (isNullObject(messageCode) || isNullObject(scopeId)
                || isEmptyOrNull(sourceLanguage) || isEmptyOrNull(targetLanguage)
                || isNullObject(translation)) {
            return;
        }
        try {
            var json = objectMapper.writeValueAsString(Map.of(
                    FIELD_TRANSLATED_TITLE, translation.getTranslatedTitle(),
                    FIELD_TRANSLATED_CONTENT, translation.getTranslatedContent(),
                    FIELD_TRANSLATED_TYPE, translation.getTranslatedType(),
                    FIELD_TRANSLATED_CATEGORY, translation.getTranslatedCategory(),
                    FIELD_TRANSLATED_FUNCTIONALITY, translation.getTranslatedFunctionality(),
                    FIELD_PROVIDER, translation.getProvider(),
                    FIELD_MODEL, translation.getModel()
            ));
            redisTemplate.opsForValue().set(
                    buildKey(messageCode, scopeId, sourceLanguage, targetLanguage),
                    json,
                    Duration.ofSeconds(TRANSLATION_CACHE_TTL_SECONDS)
            );
        } catch (DataAccessException ex) {
            log.error(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_014.getCode()), ex);
        } catch (Exception ex) {
            log.error(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_015.getCode()), ex);
        }
    }

    private String buildKey(String messageCode, String scopeId, String sourceLanguage, String targetLanguage) {
        return CACHE_VERSION + TRANSLATION_CACHE_KEY_SEPARATOR
                + trim(messageCode) + TRANSLATION_CACHE_KEY_SEPARATOR
                + trim(scopeId) + TRANSLATION_CACHE_KEY_SEPARATOR
                + trim(sourceLanguage) + TRANSLATION_CACHE_KEY_SEPARATOR
                + trim(targetLanguage);
    }

    private MessageTranslationResponseData toResponseData(JsonNode node) {
        return MessageTranslationResponseData.create(
                node.path(FIELD_TRANSLATED_TITLE).asText(),
                node.path(FIELD_TRANSLATED_CONTENT).asText(),
                node.path(FIELD_TRANSLATED_TYPE).asText(),
                node.path(FIELD_TRANSLATED_CATEGORY).asText(),
                node.path(FIELD_TRANSLATED_FUNCTIONALITY).asText(),
                node.path(FIELD_PROVIDER).asText(),
                node.path(FIELD_MODEL).asText(),
                0L
        );
    }

    private boolean isComplete(JsonNode node) {
        return node != null && node.path(FIELD_TRANSLATED_TITLE).isTextual()
                && node.path(FIELD_TRANSLATED_CONTENT).isTextual()
                && node.path(FIELD_TRANSLATED_TYPE).isTextual()
                && node.path(FIELD_TRANSLATED_CATEGORY).isTextual()
                && node.path(FIELD_TRANSLATED_FUNCTIONALITY).isTextual();
    }
}
