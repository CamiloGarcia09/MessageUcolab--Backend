package co.edu.uco.crosscutting.helpers;

import co.edu.uco.crosscutting.catalog.BootstrapMessageCatalogEnum;

public enum EnumConstants {
    PULSAR_URL("pulsar://localhost:6650"),
    DATE_FORMAT("yyyy-MM-dd'T'HH:mm:ss"),
    DEFAULT_UUID_STRING("00000000-0000-0000-0000-000000000000"),
    ERROR_UUID_INVALID_FORMAT(BootstrapMessageCatalogEnum.FUN_050.getContent()),
    UNEXPECTED_ERROR_UUID_FORMAT(BootstrapMessageCatalogEnum.FUN_051.getContent()),
    STANDARD_HYPHEN("-"),
    STANDARD_UNDERSCORE("_"),
    ERROR_DATE_FORMAT_INVALID(BootstrapMessageCatalogEnum.FUN_052.getContent());
    private final String value;
    EnumConstants(String value) {
        this.value = value;
    }
    public String getValue() {
        return value;
    }
}
