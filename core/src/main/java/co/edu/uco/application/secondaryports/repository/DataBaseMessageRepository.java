package co.edu.uco.application.secondaryports.repository;

import co.edu.uco.application.secondaryports.entity.MessageData;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface DataBaseMessageRepository {
    Optional<MessageData> findById(String id);
    Optional<MessageData> findByCode(String code);
    SimplePage<MessageData> findMessagesByEnvironment(String id, Pageable pageable, MessageFilter filter);
    Optional<MessageData> findMessageByCodeAndEnvironment(String code, String environmentId);
}