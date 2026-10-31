package co.edu.uco.application.secondaryports.repository;

import co.edu.uco.application.secondaryports.entity.MessageData;

import java.util.List;

public interface CreateMessageRepository {
    // Crea el mensaje y su relacion con cada ambiente en una sola transaccion.
    void createMessage(MessageData message, List<String> environmentIds, String messageEnvironmentStateId);
}
