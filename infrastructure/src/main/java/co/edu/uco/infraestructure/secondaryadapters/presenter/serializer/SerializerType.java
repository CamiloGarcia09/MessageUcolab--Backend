package co.edu.uco.infraestructure.secondaryadapters.presenter.serializer;

import co.edu.uco.crosscutting.exceptions.CrossWordsException;

public interface SerializerType {
    <T> String serialize(T data) throws CrossWordsException;
    String getSupportedContentType();
    boolean supports(String mediaType);
    boolean isDefault();
}