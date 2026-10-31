package co.edu.uco.messageucolab.client.http;

import java.util.List;

record MessageListResponse(String language, int total, List<MessageResponse> messages) {
}
