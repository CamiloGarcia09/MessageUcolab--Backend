package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl;

import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.crosscutting.helpers.UtilText;
import com.surrealdb.Array;
import com.surrealdb.Object;
import com.surrealdb.Response;
import com.surrealdb.Surreal;
import com.surrealdb.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static co.edu.uco.crosscutting.helpers.UtilObject.isNullObject;
import static co.edu.uco.crosscutting.helpers.UtilUUID.DEFAULT_UUID;

public abstract class SurrealCatalogSupport {

    protected final LoggingPort log;
    protected final Surreal surreal;

    protected SurrealCatalogSupport(final Surreal surreal, final LoggingPort log) {
        this.surreal = surreal;
        this.log = log;
    }

    protected <T> List<T> queryAll(final String table, final String errorMessage, final RowMapper<T> mapper) {
        final String sql = "SELECT * FROM " + table + ";";
        return query(sql, errorMessage + sql, mapper);
    }

    protected <T> List<T> query(final String sql, final String errorMessage, final RowMapper<T> mapper) {
        final List<T> list = new ArrayList<>();
        try {
            final Response response = surreal.query(sql);
            if (isNullObject(response) || response.size() == 0) {
                return list;
            }
            final Value statementResult = response.take(0);
            if (isNullObject(statementResult) || !statementResult.isArray()) {
                return list;
            }
            final Array array = statementResult.getArray();
            for (int i = 0; i < array.len(); i++) {
                final Value item = array.get(i);
                if (!isNullObject(item) && item.isObject()) {
                    list.add(mapper.map(item.getObject()));
                }
            }
        } catch (final RuntimeException ex) {
            log.error(errorMessage, ex);
            throw ex;
        }
        return list;
    }

    protected <T> Optional<T> queryOne(final String sql, final String errorMessage, final RowMapper<T> mapper) {
        try {
            final Response response = surreal.query(sql);
            if (isNullObject(response) || response.size() == 0) {
                return Optional.empty();
            }
            final Value statementResult = response.take(0);
            if (isNullObject(statementResult) || !statementResult.isArray()) {
                return Optional.empty();
            }
            final Array array = statementResult.getArray();
            if (array.len() == 0) {
                return Optional.empty();
            }
            final Value first = array.get(0);
            if (isNullObject(first) || !first.isObject()) {
                return Optional.empty();
            }
            return Optional.of(mapper.map(first.getObject()));
        } catch (final RuntimeException ex) {
            log.error(errorMessage, ex);
            throw ex;
        }
    }

    protected static UUID extractIdAsUUID(final Value value) {
        if (isNullObject(value)) return DEFAULT_UUID;
        if (value.isRecordId()) {
            try {
                final String fullId = value.getRecordId().toString();
                final int separator = fullId.indexOf(':');
                if (separator > 0) {
                    final String idPart = cleanThingId(fullId.substring(separator + 1));
                    return UUID.fromString(idPart);
                }
            } catch (Exception e) {
                return DEFAULT_UUID;
            }
        }
        if (value.isString()) {
            try {
                final String raw = value.getString();
                final int separator = raw.indexOf(':');
                final String idPart = separator > 0 ? cleanThingId(raw.substring(separator + 1)) : cleanThingId(raw);
                return UUID.fromString(idPart);
            } catch (Exception e) {
                return DEFAULT_UUID;
            }
        }
        return DEFAULT_UUID;
    }

    protected static String cleanThingId(String id) {
        id = UtilText.getDefault(id).trim();
        if (id.length() >= 2 && id.startsWith("`") && id.endsWith("`")) {
            id = id.substring(1, id.length() - 1);
        }
        if (id.length() >= 2 && id.startsWith("\u27E8") && id.endsWith("\u27E9")) {
            id = id.substring(1, id.length() - 1);
        }
        final int separator = id.indexOf(':');
        if (separator > 0) {
            id = id.substring(separator + 1);
        }
        return id.trim();
    }

    protected static UUID extractCatalogId(final Value value) {
        if (isNullObject(value)) return DEFAULT_UUID;
        final String raw;
        if (value.isRecordId()) {
            raw = value.getRecordId().toString();
        } else if (value.isString()) {
            raw = value.getString();
        } else {
            raw = value.toString();
        }
        final String idPart;
        final int separator = raw.indexOf(':');
        if (separator > 0) {
            idPart = cleanThingId(raw.substring(separator + 1));
        } else {
            idPart = cleanThingId(raw);
        }
        try {
            return UUID.fromString(idPart);
        } catch (final Exception ignored) {
            return UUID.nameUUIDFromBytes(raw.getBytes());
        }
    }

    protected static String stringOf(final Value value) {
        if (isNullObject(value) || value.isNull() || value.isNone()) return "";
        if (value.isString()) return value.getString();
        if (value.isUuid()) return value.getUuid().toString();
        return value.toString();
    }

    @FunctionalInterface
    protected interface RowMapper<T> {
        T map(Object object);
    }
}