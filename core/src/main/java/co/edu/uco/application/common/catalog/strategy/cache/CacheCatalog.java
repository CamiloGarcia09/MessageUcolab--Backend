package co.edu.uco.application.common.catalog.strategy.cache;

import co.edu.uco.application.common.catalog.strategy.MessageCatalog;
import co.edu.uco.application.secondaryports.entity.MessageData;
import co.edu.uco.application.secondaryports.repository.SimplePage;
import co.edu.uco.application.secondaryports.repository.SimplePageRequest;

import java.util.Optional;

public abstract class CacheCatalog extends MessageCatalog<String, Optional<MessageData>> {
    public abstract void addMessage(MessageData messageModel);
    public abstract void addMessageWithEnvironment(MessageData messageModel, String environmentId);
    public abstract SimplePage<MessageData> getMessageWithEnvironment(String environment, SimplePageRequest request);
    public abstract Optional<MessageData> getMessageByCodeAndEnvironment(String code, String environmentId);
    public abstract void evictMessage(String code);
}