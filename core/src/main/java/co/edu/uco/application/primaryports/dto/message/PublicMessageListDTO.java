package co.edu.uco.application.primaryports.dto.message;

import lombok.Getter;

import java.util.List;

/**
 * Mensajes disponibles de una aplicacion en un ambiente, en el idioma base de la aplicacion.
 */
@Getter
public final class PublicMessageListDTO {

    private final String language;
    private final int total;
    private final List<ResolvedMessageDTO> messages;

    public PublicMessageListDTO(String language, List<ResolvedMessageDTO> messages) {
        this.language = language;
        this.messages = List.copyOf(messages);
        this.total = this.messages.size();
    }
}
