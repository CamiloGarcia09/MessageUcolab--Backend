package co.edu.uco.crosscutting.helpers;


import co.edu.uco.crosscutting.exceptions.CrossWordsException;

import java.util.UUID;

import static co.edu.uco.crosscutting.helpers.EnumConstants.*;
import static co.edu.uco.crosscutting.helpers.UtilObject.*;
import static co.edu.uco.crosscutting.helpers.UtilText.*;

public final class UtilUUID {
    public static final UUID DEFAULT_UUID = getStringToUUID(DEFAULT_UUID_STRING.getValue());
    private UtilUUID() {}
    public static UUID getDefaultUUID(final UUID uuid) {
        return getDefaultIsNullObject(uuid, DEFAULT_UUID);
    }
    public static boolean isEqual(final UUID uuidOne, final UUID uuidTwo) {
        return getDefaultUUID(uuidOne).equals(getDefaultUUID(uuidTwo));
    }
    public static UUID getNewUUID() {
        UUID uuid;
        do {
            uuid = UUID.randomUUID();
        } while (isEqual(uuid, DEFAULT_UUID));
        return uuid;
    }
    public static UUID getUUIDFromString(final String uuidString) {
        UUID uuid = DEFAULT_UUID;
        if(!isEmpty(trim(uuidString))) {
            try {
                uuid = UUID.fromString(uuidString);
            } catch (IllegalArgumentException exception) {
                throw CrossWordsException.build(ERROR_UUID_INVALID_FORMAT.getValue(),exception);
            } catch (Exception exception){
                throw CrossWordsException.build(UNEXPECTED_ERROR_UUID_FORMAT.getValue(), exception);
            }
        }
        return uuid;
    }
    public static String getStringFromUUID(final UUID uuid) {
        String uuidString = DEFAULT_UUID_STRING.getValue();
        if(!isNull(uuid)) {
            uuidString = uuid.toString();
        }
        return uuidString;
    }
    public static UUID getStringToUUID(final String uuid){
        return getUUIDFromString(uuid);
    }
    public static boolean isNull(UUID value) {
        return isNullObject(value);
    }
    public static String formatUUID(UUID uuid) {
        return uuid.toString().replace(STANDARD_HYPHEN.getValue(), STANDARD_UNDERSCORE.getValue());
    }
    public static UUID unformatUUID(String uuid) {
        return UUID.fromString(uuid.replace(STANDARD_UNDERSCORE.getValue(), STANDARD_HYPHEN.getValue()));
    }
}