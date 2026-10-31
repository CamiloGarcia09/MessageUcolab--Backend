package co.edu.uco.application.common.catalog.strategy;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.entity.MessageTranslationRequestData;
import co.edu.uco.application.secondaryports.entity.MessageTranslationResponseData;
import co.edu.uco.application.secondaryports.translation.MessageTranslationCachePort;
import co.edu.uco.application.secondaryports.translation.MessageTranslationPort;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.BusinessException;
import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionLocation;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import static co.edu.uco.crosscutting.helpers.UtilText.EMPTY;

public final class ConsumerMessageTranslationStrategy {
    private final MessageTranslationCachePort cache;
    private final MessageTranslationPort translator;
    private final CatalogPort catalog;

    public ConsumerMessageTranslationStrategy(MessageTranslationCachePort cache,
                                               MessageTranslationPort translator, CatalogPort catalog) {
        this.cache = cache;
        this.translator = translator;
        this.catalog = catalog;
    }

    public MessageTranslationResponseData translate(ConsumerTranslationSnapshot snapshot, String applicationId,
                                                     String environmentId, String targetLanguage) {
        var message = snapshot.message();
        if (snapshot.sourceLanguage().equals(targetLanguage)) {
            return MessageTranslationResponseData.create(message.getTitle(), message.getContent(),
                    message.getType(), message.getCategory(), message.getFunctionality(), EMPTY, EMPTY, 0L);
        }
        var scope = cacheScope(snapshot, applicationId, environmentId);
        var cached = cache.findTranslation(message.getCode(), scope, snapshot.sourceLanguage(), targetLanguage);
        if (cached.isPresent()) {
            return cached.get();
        }
        var request = MessageTranslationRequestData.create(message.getCode(), snapshot.sourceLanguage(),
                targetLanguage, message.getTitle(), message.getContent(), message.getType(),
                message.getCategory(), applicationId, message.getFunctionality());
        var translation = translator.translate(request);
        cache.saveTranslation(message.getCode(), scope, snapshot.sourceLanguage(), targetLanguage, translation);
        return translation;
    }

    private String cacheScope(ConsumerTranslationSnapshot snapshot, String applicationId, String environmentId) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            for (var value : snapshot.values()) {
                var bytes = value.getBytes(StandardCharsets.UTF_8);
                digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
                digest.update(bytes);
            }
            return "consumer-v1:" + applicationId + ":" + environmentId + ":"
                    + HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw BusinessException.buildTechnicalException(
                    catalog.getMessage(MessageCatalogCodeEnum.FUN_048.getCode()), exception,
                    ExceptionLocation.APPLICATION);
        }
    }
}
