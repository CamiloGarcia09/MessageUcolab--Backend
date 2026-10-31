package co.edu.uco.infraestructure.secondaryadapters.presenter.serializer.impl.xml;

import co.edu.uco.application.common.catalog.CatalogPortStaticRef;
import co.edu.uco.infraestructure.secondaryadapters.presenter.serializer.AbstractSerializer;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.CrossWordsException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import static co.edu.uco.infraestructure.config.InfrastructureConstant.XML_SERIALIZER_CONTENT_TYPE;

public final class XMLSerializer extends AbstractSerializer {
    public XMLSerializer() {
        super(XML_SERIALIZER_CONTENT_TYPE);
    }
    @Override
    public <T> String serialize(T data) {
        try {
            var xmlMapper = new XmlMapper();
            xmlMapper.registerModule(new JavaTimeModule());
            return xmlMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw CrossWordsException.build(CatalogPortStaticRef.getMessage(MessageCatalogCodeEnum.TCH_018.getCode()), e);
        }
    }
}