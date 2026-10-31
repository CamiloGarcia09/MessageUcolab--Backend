package co.edu.uco.infraestructure.secondaryadapters.presenter.serializer.impl.html;

import co.edu.uco.application.common.catalog.CatalogPortStaticRef;
import co.edu.uco.infraestructure.secondaryadapters.presenter.serializer.AbstractSerializer;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.CrossWordsException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import static co.edu.uco.infraestructure.config.InfrastructureConstant.*;

public final class HTMLSerializer extends AbstractSerializer {
    public HTMLSerializer() {
        super(HTML_SERIALIZER_CONTENT_TYPE);
    }
    @Override
    public <T> String serialize(T data) {
        try {
            var html = new StringBuilder();
            var objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            var result = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
            html.append(HTML_OPEN_TAG).append(BODY_OPEN_TAG);
            html.append(PRE_OPEN_TAG).append(result).append(PRE_CLOSE_TAG);
            html.append(BODY_CLOSE_TAG).append(HTML_CLOSE_TAG);
            return html.toString();
        } catch (Exception e) {
            throw CrossWordsException.build(CatalogPortStaticRef.getMessage(MessageCatalogCodeEnum.TCH_018.getCode()), e);
        }
    }
}