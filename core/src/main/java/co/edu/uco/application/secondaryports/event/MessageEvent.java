package co.edu.uco.application.secondaryports.event;

public record MessageEvent(String id, String type, String environmentId, String messageCode) {
}
