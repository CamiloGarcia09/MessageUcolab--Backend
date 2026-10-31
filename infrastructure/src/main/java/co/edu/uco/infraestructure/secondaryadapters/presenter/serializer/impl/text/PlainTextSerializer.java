package co.edu.uco.infraestructure.secondaryadapters.presenter.serializer.impl.text;

import co.edu.uco.application.common.catalog.CatalogPortStaticRef;
import co.edu.uco.infraestructure.secondaryadapters.presenter.serializer.AbstractSerializer;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.CrossWordsException;

import static co.edu.uco.infraestructure.config.InfrastructureConstant.TEXT_SERIALIZER_CONTENT_TYPE;

public final class PlainTextSerializer extends AbstractSerializer {
    public PlainTextSerializer() {
        super(TEXT_SERIALIZER_CONTENT_TYPE);
    }
    @Override
    public <T> String serialize(T data) throws CrossWordsException {
        try {
            return data.toString();
        } catch (Exception e) {
            throw CrossWordsException.build(CatalogPortStaticRef.getMessage(MessageCatalogCodeEnum.TCH_018.getCode()), e);
        }
    }
}