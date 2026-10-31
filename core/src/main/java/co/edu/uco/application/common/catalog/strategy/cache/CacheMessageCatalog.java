package co.edu.uco.application.common.catalog.strategy.cache;

import co.edu.uco.application.secondaryports.entity.MessageData;
import co.edu.uco.application.secondaryports.repository.CacheMessageRepository;
import co.edu.uco.application.secondaryports.repository.SimplePage;
import co.edu.uco.application.secondaryports.repository.SimplePageRequest;
import co.edu.uco.application.secondaryports.repository.PageBuilder;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static co.edu.uco.crosscutting.helpers.UtilText.EMPTY;
import static co.edu.uco.crosscutting.helpers.UtilUUID.getUUIDFromString;

@Component
public final class CacheMessageCatalog extends CacheCatalog {
    private final CacheMessageRepository repository;
    public CacheMessageCatalog(CacheMessageRepository repository) {
        this.repository = repository;
    }
    @Override
    public Optional<MessageData> getMessageById(String id) {
        return repository.findById(getUUIDFromString(id));
    }
    @Override
    public String getContent(String code) {
        return repository.findById(getUUIDFromString(code)).map(MessageData::getContent).orElse(EMPTY);
    }
    @Override
    public void addMessage(MessageData messageModel) {
        repository.save(messageModel);
    }
    @Override
    public void addMessageWithEnvironment(MessageData messageModel, String environmentId) {
        repository.saveWithEnvironment(messageModel, environmentId);
    }
    @Override
    public boolean isExist(String key) {
        return getMessageById(key).isPresent();
    }
    @Override
    public SimplePage<MessageData> getMessageWithEnvironment(String environment, SimplePageRequest request) {
        var result = PageBuilder.createPageRequest(request);
        return repository.findMessagesByEnvironment(environment, result);
    }
    @Override
    public Optional<MessageData> getMessageByCodeAndEnvironment(String code, String environmentId) {
        return repository.findMessageByCodeAndEnvironment(code, environmentId);
    }

    @Override
    public void evictMessage(String code) {
        repository.evictByCode(code);
    }
}
