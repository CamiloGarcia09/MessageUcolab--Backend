package co.edu.uco.messageucolab.client.api.model;

import java.util.List;

public record MessageCollection(String language, int total, List<MessageDetails> messages) {

    public MessageCollection {
        messages = List.copyOf(messages);
    }
}
