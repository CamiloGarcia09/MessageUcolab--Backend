package co.edu.uco.application.primaryports.dto.message;

import lombok.Getter;

import static co.edu.uco.crosscutting.helpers.UtilText.getDefault;

@Getter
public final class MessageCodeDTO {
    private String code;
    public MessageCodeDTO(String code) {
        setCode(code);
    }
    public void setCode(String code) {
        this.code = getDefault(code);
    }
    public static MessageCodeDTO create(String code) {
        return new MessageCodeDTO(code);
    }
}