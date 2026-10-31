package co.edu.uco.application.secondaryports.repository;

import co.edu.uco.application.secondaryports.entity.MessageData;

import java.util.Optional;

public interface CacheMessageRepository extends MessageRepository {
    void saveWithEnvironment(MessageData data, String environmentId);
    Optional<MessageData> findMessageByCodeAndEnvironment(String code, String environmentId);
    void evictByCode(String code);
}