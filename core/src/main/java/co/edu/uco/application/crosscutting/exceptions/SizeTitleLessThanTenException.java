package co.edu.uco.application.crosscutting.exceptions;

import co.edu.uco.application.common.catalog.CatalogPortStaticRef;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.CrossWordsException;
import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionLocation;
import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionType;

import java.io.Serial;

public final class SizeTitleLessThanTenException extends CrossWordsException {
    @Serial
    private static final long serialVersionUID = 7220210614113459979L;
    private SizeTitleLessThanTenException() {
       super(
           CatalogPortStaticRef.getTitle(MessageCatalogCodeEnum.FUN_020.getCode()),
           CatalogPortStaticRef.getMessage(MessageCatalogCodeEnum.FUN_020.getCode()),
           null,
           ExceptionType.BUSINESS_RULE,
           ExceptionLocation.APPLICATION
       );
    }
    public static void report() {
        throw new SizeTitleLessThanTenException();
    }
}