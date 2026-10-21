package co.edu.uco.application.common.catalog.strategy.database;

import co.edu.uco.application.secondaryports.entity.MessageData;
import co.edu.uco.application.secondaryports.repository.DataBaseMessageRepository;
import co.edu.uco.application.secondaryports.repository.SimplePage;
import co.edu.uco.application.secondaryports.repository.SimplePageRequest;
import co.edu.uco.application.secondaryports.repository.PageBuilder;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static co.edu.uco.crosscutting.helpers.UtilText.EMPTY;

@Component
public final class DatabaseMessageCatalog extends DatabaseCatalog {
    private final DataBaseMessageRepository repository;
    public DatabaseMessageCatalog(DataBaseMessageRepository repository) {
        this.repository = repository;
    }
    @Override
    public Optional<MessageData> getMessageById(String code) {
        return repository.findById(code);
    }
    @Override
    public String getContent(String code) {
        return repository.findById(code).map(MessageData::getContent).orElse(EMPTY);
    }
    @Override
    public boolean isExist(String key) {
        return getMessageById(key).isPresent();
    }
    @Override
    public SimplePage<MessageData> getMessageWithEnvironment(String environment, SimplePageRequest request) {
        var result = PageBuilder.createPageRequest(request);
        return repository.findMessagesByEnvironment(environment, result, request.getFilter());
    }
    @Override
    public Optional<MessageData> getMessageByCodeAndEnvironment(String code, String environmentId) {
        return repository.findMessageByCodeAndEnvironment(code, environmentId);
    }

    @Override
    public SimplePage<MessageData> getMessagesWithApplication(String applicationId, SimplePageRequest request) {
        var filter = request.getFilter();
        return repository.findMessagesByApplication(applicationId,
                filter == null ? null : filter.getEnvironmentIds(), PageBuilder.createPageRequest(request), filter);
    }

    @Override
    public Optional<MessageData> getMessageByCodeAndApplication(String code, String applicationId) {
        return repository.findByCodeAndApplication(code, applicationId);
    }
}
