package co.edu.uco.infraestructure.secondaryadapters.catalog;

import co.edu.uco.application.common.catalog.BootstrapMessageCatalogPort;
import co.edu.uco.application.common.catalog.CatalogPortStaticRef;
import co.edu.uco.application.common.catalog.MessageCatalog;
import co.edu.uco.application.crosscutting.exceptions.MessageKeyCanNotBeEmptyException;
import co.edu.uco.application.crosscutting.exceptions.MessageKeyCanNotBeNullException;
import co.edu.uco.application.crosscutting.exceptions.MessageNotFoundException;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.crosscutting.catalog.BootstrapMessageCatalogEnum;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.CrossWordsException;
import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionType;
import jakarta.annotation.PostConstruct;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

import static co.edu.uco.crosscutting.helpers.UtilObject.isNullObject;
import static co.edu.uco.crosscutting.helpers.UtilText.EMPTY;
import static co.edu.uco.crosscutting.helpers.UtilText.isEmpty;
import static co.edu.uco.crosscutting.helpers.UtilText.isNull;
import static co.edu.uco.crosscutting.helpers.UtilText.isEmptyOrNull;
import static co.edu.uco.crosscutting.helpers.UtilText.trim;

@Component
public class RedisCatalogMessageAdapter implements CatalogPort {

    private final LoggingPort log;
    private final RedisTemplate<String, String> redisTemplate;
    private final BootstrapMessageCatalogPort defaultCatalog;

    public RedisCatalogMessageAdapter(RedisTemplate<String, String> redisTemplate, LoggingPortFactory loggerFactory) {
        this.log = loggerFactory.getLogger(RedisCatalogMessageAdapter.class);
        this.redisTemplate = redisTemplate;
        this.defaultCatalog = new BootstrapMessageCatalogPort();
    }

    @PostConstruct
    public void init() {
        CatalogPortStaticRef.set(this);
    }

    @Override
    public MessageCatalog getMessageModel(String key) {
        if (isNull(key)) {
            MessageKeyCanNotBeNullException.report();
        }
        if (isEmpty(trim(key))) {
            MessageKeyCanNotBeEmptyException.report();
        }
        if (BootstrapMessageCatalogEnum.exists(key)) {
            return defaultCatalog.getMessageModel(key);
        }
        try {
            Map<Object, Object> entry = redisTemplate.opsForHash().entries(key);
            if (isNullObject(entry) || entry.isEmpty()) {
                MessageNotFoundException.report(key);
            }
            return new MessageCatalog(
                    (String) entry.get("code"),
                    (String) entry.get("title"),
                    (String) entry.get("content"),
                    (String) entry.get("type"),
                    (String) entry.get("category"));
        } catch (CrossWordsException ex) {
            throw ex;
        } catch (Exception ex) {
            var techMsg = CatalogPortStaticRef.getMessage(MessageCatalogCodeEnum.TCH_051.getCode()).formatted(key);
            log.error(techMsg, ex);
            throw CrossWordsException.buildInfrastructure(techMsg,
                    CatalogPortStaticRef.getMessage(MessageCatalogCodeEnum.FUN_023.getCode()), ex, ExceptionType.TECHNICAL);
        }
    }

    @Override
    public String getMessage(String key) {
        if (isEmptyOrNull(trim(key))) {
            return EMPTY;
        }
        if (BootstrapMessageCatalogEnum.exists(key)) {
            return defaultCatalog.getMessage(key);
        }
        try {
            var value = (String) redisTemplate.opsForHash().get(key, "content");
            return isNullObject(value) ? key : value;
        } catch (Exception ex) {
            log.error(CatalogPortStaticRef.getMessage(MessageCatalogCodeEnum.TCH_052.getCode()).formatted(key), ex);
            return key;
        }
    }

    @Override
    public String getMessage(String key, String defaultMessage) {
        if (isEmptyOrNull(trim(key))) {
            return defaultMessage;
        }
        if (BootstrapMessageCatalogEnum.exists(key)) {
            return defaultCatalog.getMessage(key);
        }
        try {
            String message = (String) redisTemplate.opsForHash().get(key, "content");
            return isNullObject(message) ? defaultMessage : message;
        } catch (Exception ex) {
            log.error(CatalogPortStaticRef.getMessage(MessageCatalogCodeEnum.TCH_052.getCode()).formatted(key), ex);
            return defaultMessage;
        }
    }

    @Override
    public String getTitle(String key) {
        if (isEmptyOrNull(trim(key))) {
            return EMPTY;
        }
        if (BootstrapMessageCatalogEnum.exists(key)) {
            return defaultCatalog.getTitle(key);
        }
        try {
            var title = (String) redisTemplate.opsForHash().get(key, "title");
            return isNullObject(title) ? EMPTY : title;
        } catch (Exception ex) {
            log.error(CatalogPortStaticRef.getMessage(MessageCatalogCodeEnum.TCH_053.getCode()).formatted(key), ex);
            return EMPTY;
        }
    }

    @Override
    public void setMessage(String key, MessageCatalog message) {
        if (isEmptyOrNull(trim(key)) || isNullObject(message)) {
            return;
        }
        try {
            redisTemplate.opsForHash().putAll(key, Map.of(
                    "code",     isNullObject(message.code())     ? EMPTY : message.code(),
                    "title",    isNullObject(message.title())    ? EMPTY : message.title(),
                    "content",  isNullObject(message.content())  ? EMPTY : message.content(),
                    "type",     isNullObject(message.type())     ? EMPTY : message.type(),
                    "category", isNullObject(message.category()) ? EMPTY : message.category()));
        } catch (Exception ex) {
            log.error(CatalogPortStaticRef.getMessage(MessageCatalogCodeEnum.TCH_054.getCode()).formatted(key), ex);
        }
    }
}