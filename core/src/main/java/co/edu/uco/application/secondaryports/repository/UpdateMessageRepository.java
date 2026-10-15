package co.edu.uco.application.secondaryports.repository;

import co.edu.uco.application.secondaryports.entity.MessageData;

public interface UpdateMessageRepository {
    void updateMessage(MessageData message);
}
