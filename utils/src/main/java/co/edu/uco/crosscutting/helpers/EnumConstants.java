package co.edu.uco.crosscutting.helpers;

public enum EnumConstants {
    PULSAR_URL("pulsar://localhost:6650"),
    DATE_FORMAT("yyyy-MM-dd'T'HH:mm:ss"),
    DEFAULT_UUID_STRING("00000000-0000-0000-0000-000000000000"),
    ERROR_UUID_INVALID_FORMAT("The UUID to be converted has no valid format."),
    UNEXPECTED_ERROR_UUID_FORMAT("An unexpected error occurred while trying to convert the entry to UUID."),
    STANDARD_HYPHEN("-"),
    STANDARD_UNDERSCORE("_"),
    ERROR_DATE_FORMAT_INVALID("The date to be converted has no valid format.");
    private final String value;
    EnumConstants(String value) {
        this.value = value;
    }
    public String getValue() {
        return value;
    }
}
