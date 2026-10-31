package co.edu.uco.application.crosscutting.exceptions;

import co.edu.uco.application.common.catalog.CatalogPortStaticRef;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.NotFoundException;

import java.io.Serial;

public final class MessageNotFoundException extends NotFoundException {
    @Serial
    private static final long serialVersionUID = 1L;
    private MessageNotFoundException(String key) {
        super(
            CatalogPortStaticRef.getTitle(MessageCatalogCodeEnum.TCH_009.getCode()),
            CatalogPortStaticRef.getMessage(MessageCatalogCodeEnum.TCH_009.getCode()).formatted(key),
            null,
            co.edu.uco.crosscutting.exceptions.enumeration.ExceptionType.BUSINESS,
            co.edu.uco.crosscutting.exceptions.enumeration.ExceptionLocation.APPLICATION
        );
        setCode("MESSAGE_NOT_FOUND");
    }
    public static void report(String key) {
        throw new MessageNotFoundException(key);
    }
}