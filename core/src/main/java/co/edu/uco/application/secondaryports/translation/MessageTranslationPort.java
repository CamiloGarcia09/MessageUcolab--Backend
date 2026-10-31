package co.edu.uco.application.secondaryports.translation;

import co.edu.uco.application.secondaryports.entity.MessageTranslationRequestData;
import co.edu.uco.application.secondaryports.entity.MessageTranslationResponseData;

public interface MessageTranslationPort {
    MessageTranslationResponseData translate(MessageTranslationRequestData requestData);
}
