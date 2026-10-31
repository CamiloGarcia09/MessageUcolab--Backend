package co.edu.uco.infraestructure.secondaryadapters.presenter.serializer.impl.yaml;

import co.edu.uco.application.common.catalog.CatalogPortStaticRef;
import co.edu.uco.infraestructure.secondaryadapters.presenter.serializer.AbstractSerializer;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.CrossWordsException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static co.edu.uco.infraestructure.config.InfrastructureConstant.YAML_SERIALIZER_CONTENT_TYPE;
import static co.edu.uco.crosscutting.helpers.EnumConstants.DATE_FORMAT;

public final class YamlSerializer extends AbstractSerializer {
    public YamlSerializer() {
        super(YAML_SERIALIZER_CONTENT_TYPE);
    }
    @Override
    public <T> String serialize(T data) throws CrossWordsException {
        try{
            var mapper = new YAMLMapper();
            var module = new JavaTimeModule();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT.getValue());
            module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(formatter));
            mapper.registerModule(module);
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            return mapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw CrossWordsException.build(CatalogPortStaticRef.getMessage(MessageCatalogCodeEnum.TCH_018.getCode()), e);
        }
    }
}