package co.edu.uco.application.common.catalog.strategy.database;

import co.edu.uco.application.secondaryports.entity.MessageData;
import co.edu.uco.application.secondaryports.repository.SimplePage;
import co.edu.uco.application.secondaryports.repository.SimplePageRequest;
import co.edu.uco.application.common.catalog.strategy.MessageCatalog;

import java.util.Optional;

public abstract class DatabaseCatalog extends MessageCatalog<String, Optional<MessageData>> {
    public abstract SimplePage<MessageData> getMessageWithEnvironment(String environment, SimplePageRequest request);
    public abstract Optional<MessageData> getMessageByCodeAndEnvironment(String code, String environmentId);
    public abstract SimplePage<MessageData> getMessagesWithApplication(String applicationId, SimplePageRequest request);
    public abstract Optional<MessageData> getMessageByCodeAndApplication(String code, String applicationId);
}