package co.edu.uco.application.secondaryports.repository;

import co.edu.uco.application.secondaryports.entity.MessageData;

import java.util.List;

public interface UpdateMessageRepository {
    // environmentIds nulo conserva los ambientes; con valor, el mensaje queda solo en esos ambientes.
    void updateMessage(MessageData message, List<String> environmentIds, String messageEnvironmentStateId);
}
