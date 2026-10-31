package co.edu.uco.application.crosscutting.exceptions;

import co.edu.uco.application.common.catalog.CatalogPortStaticRef;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.CrossWordsException;
import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionLocation;
import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionType;

import java.io.Serial;

public final class TitleCanNotBeEmptyException extends CrossWordsException {
    @Serial
    private static final long serialVersionUID = 41765698692188909L;
    private TitleCanNotBeEmptyException() {
        super(
            CatalogPortStaticRef.getTitle(MessageCatalogCodeEnum.FUN_022.getCode()),
            CatalogPortStaticRef.getMessage(MessageCatalogCodeEnum.FUN_022.getCode()),
            null,
            ExceptionType.BUSINESS_RULE,
            ExceptionLocation.APPLICATION
        );
    }
    public static void report() {
        throw new TitleCanNotBeEmptyException();
    }
}