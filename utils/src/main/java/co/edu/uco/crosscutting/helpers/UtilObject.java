package co.edu.uco.crosscutting.helpers;

public final class UtilObject {
    private UtilObject() {}
    public static <T> boolean isNullObject(T object) {
        return object == null;
    }
    public static <T> T getDefaultIsNullObject(T object, T defaultValue) {
        return (isNullObject(object) ? defaultValue : object);
    }
}