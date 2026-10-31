package co.edu.uco.messageucolab.client.api.exception;

public final class MessageClientResponseException extends RuntimeException {

    private final int statusCode;

    public MessageClientResponseException(int statusCode, Throwable cause) {
        super(cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
