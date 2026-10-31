package co.edu.uco.application.common.catalog;

import co.edu.uco.application.crosscutting.exceptions.MessageKeyCanNotBeEmptyException;
import co.edu.uco.application.crosscutting.exceptions.MessageKeyCanNotBeNullException;
import co.edu.uco.application.crosscutting.exceptions.MessageNotFoundException;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.crosscutting.catalog.BootstrapMessageCatalogEnum;

import static co.edu.uco.crosscutting.helpers.UtilObject.isNullObject;
import static co.edu.uco.crosscutting.helpers.UtilText.EMPTY;
import static co.edu.uco.crosscutting.helpers.UtilText.isEmpty;
import static co.edu.uco.crosscutting.helpers.UtilText.isEmptyOrNull;
import static co.edu.uco.crosscutting.helpers.UtilText.isNull;
import static co.edu.uco.crosscutting.helpers.UtilText.trim;

public final class BootstrapMessageCatalogPort implements CatalogPort {

    @Override
    public MessageCatalog getMessageModel(String key) {
        if (isNull(key)) {
            MessageKeyCanNotBeNullException.report();
        }
        if (isEmpty(trim(key))) {
            MessageKeyCanNotBeEmptyException.report();
        }
        var message = BootstrapMessageCatalogEnum.findByCode(key).orElse(null);
        if (isNullObject(message)) {
            MessageNotFoundException.report(key);
        }
        return new MessageCatalog(
                message.getCode(),
                message.getTitle(),
                message.getContent(),
                message.getType(),
                message.getCategory());
    }

    @Override
    public String getMessage(String key) {
        if (isEmptyOrNull(trim(key))) {
            return EMPTY;
        }
        return BootstrapMessageCatalogEnum.findByCode(key)
                .map(BootstrapMessageCatalogEnum::getContent)
                .orElse(key);
    }

    @Override
    public String getMessage(String key, String defaultMessage) {
        if (isEmptyOrNull(trim(key))) {
            return defaultMessage;
        }
        return BootstrapMessageCatalogEnum.findByCode(key)
                .map(BootstrapMessageCatalogEnum::getContent)
                .orElse(defaultMessage);
    }

    @Override
    public String getTitle(String key) {
        if (isEmptyOrNull(trim(key))) {
            return EMPTY;
        }
        return BootstrapMessageCatalogEnum.findByCode(key)
                .map(BootstrapMessageCatalogEnum::getTitle)
                .orElse(EMPTY);
    }

    @Override
    public void setMessage(String key, MessageCatalog message) {
    }
}