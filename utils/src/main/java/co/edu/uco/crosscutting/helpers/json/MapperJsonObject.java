package co.edu.uco.crosscutting.helpers.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;
import java.util.Optional;

public interface MapperJsonObject {
    Optional<String> execute(Object object);
    <T> List<T> execute(String json, TypeReference<List<T>> typeReference) throws JsonProcessingException;
    <T> Optional<T> execute(String json, Class<T> targetClass);
    Optional<String> executeGson (Object object);
}
