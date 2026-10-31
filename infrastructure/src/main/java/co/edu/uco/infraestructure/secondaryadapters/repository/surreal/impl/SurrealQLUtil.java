package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static co.edu.uco.crosscutting.helpers.UtilDate.nowUtc;
import static co.edu.uco.crosscutting.helpers.UtilObject.isNullObject;
import static co.edu.uco.crosscutting.helpers.UtilText.getDefault;

/**
 * Helpers to build SurrealQL strings safely without relying on
 * {@code queryBind}, whose native binding is missing in the
 * SurrealDB Java SDK 0.2.1.
 */
final class SurrealQLUtil {

    private SurrealQLUtil() {
    }

    /** Escapes a string literal for SurrealQL single-quoted form. */
    static String quote(final String value) {
        if (isNullObject(value)) {
            return "NONE";
        }
        return "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }

    /** Renders a record id literal: {@code table:`id`}. */
    static String recordIdLiteral(final String table, final String id) {
        final String safeId = getDefault(id).replace("`", "");
        return table + ":`" + safeId + "`";
    }

    /** Renders a SurrealQL datetime literal in ISO-8601 / RFC 3339. */
    static String datetime(final LocalDateTime value) {
        final LocalDateTime safe = isNullObject(value) ? nowUtc() : value;
        return "d'" + safe.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) + "'";
    }
}
