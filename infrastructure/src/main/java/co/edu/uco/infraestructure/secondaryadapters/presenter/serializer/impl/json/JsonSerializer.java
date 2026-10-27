package co.edu.uco.infraestructure.secondaryadapters.presenter.serializer.impl.json;

import co.edu.uco.application.common.catalog.CatalogPortStaticRef;
import co.edu.uco.application.primaryports.dto.message.MessageDTO;
import co.edu.uco.application.primaryports.dto.message.ResolvedMessageDTO;
import co.edu.uco.application.primaryports.dto.message.ConsumerTranslatedMessageDTO;
import co.edu.uco.application.primaryports.dto.message.TranslatedMessageDTO;
import co.edu.uco.infraestructure.secondaryadapters.presenter.serializer.AbstractSerializer;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.CrossWordsException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.Instant;

import static co.edu.uco.infraestructure.config.InfrastructureConstant.JSON_SERIALIZER_CONTENT_TYPE;


public final class JsonSerializer extends AbstractSerializer {
    public JsonSerializer() {
        super(JSON_SERIALIZER_CONTENT_TYPE);
    }
    @Override
    public <T> String serialize(T data) throws CrossWordsException {
        try{
            var mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.addMixIn(MessageDTO.class, MessageDTOMixIn.class);
            mapper.addMixIn(ResolvedMessageDTO.class, ResolvedMessageDTOMixIn.class);
            mapper.addMixIn(ConsumerTranslatedMessageDTO.class, ConsumerTranslatedMessageDTOMixIn.class);
            mapper.addMixIn(TranslatedMessageDTO.class, TranslatedMessageDTOMixIn.class);
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            return mapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw CrossWordsException.build(CatalogPortStaticRef.getMessage(MessageCatalogCodeEnum.TCH_018.getCode()), e);
        }
    }
    @Override
    public boolean isDefault() {
        return true;
    }

    private abstract static class MessageDTOMixIn {
        @JsonProperty("created_at")
        public abstract Instant getCreatedAt();
    }

    @JsonPropertyOrder({"code", "title", "content", "type", "category", "functionality", "created_at", "updated_at"})
    private abstract static class ResolvedMessageDTOMixIn {
        @JsonProperty("created_at")
        public abstract Instant getCreatedAt();

        @JsonProperty("updated_at")
        public abstract Instant getUpdatedAt();
    }

    @JsonPropertyOrder({"code", "sourceLanguage", "targetLanguage", "title", "content", "type", "category",
            "functionality", "created_at", "updated_at"})
    private abstract static class ConsumerTranslatedMessageDTOMixIn extends ResolvedMessageDTOMixIn {
    }

    @JsonPropertyOrder({
            "code", "sourceLanguage", "targetLanguage",
            "originalTitle", "originalContent", "originalType", "originalCategory", "originalFunctionality",
            "translatedTitle", "translatedContent", "translatedType", "translatedCategory", "translatedFunctionality",
            "translationProvider", "translationModel", "translationElapsedMs", "dynamicTranslation"
    })
    private abstract static class TranslatedMessageDTOMixIn {
    }
}
