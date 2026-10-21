package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl;

import co.edu.uco.application.common.catalog.CatalogPortStaticRef;
import co.edu.uco.application.secondaryports.entity.ApplicationData;
import co.edu.uco.application.secondaryports.entity.FunctionalityData;
import co.edu.uco.application.secondaryports.entity.MessageCategoryData;
import co.edu.uco.application.secondaryports.entity.MessageData;
import co.edu.uco.application.secondaryports.entity.MessageEnvironmentRefData;
import co.edu.uco.application.secondaryports.entity.MessageTypeData;
import co.edu.uco.application.secondaryports.entity.StatusMessageData;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.DataBaseMessageRepository;
import co.edu.uco.application.secondaryports.repository.MessageFilter;
import co.edu.uco.application.secondaryports.repository.SimplePage;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import com.surrealdb.Array;
import com.surrealdb.Object;
import com.surrealdb.Response;
import com.surrealdb.Surreal;
import com.surrealdb.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.time.Instant;

import static co.edu.uco.crosscutting.helpers.UtilObject.isNullObject;
import static co.edu.uco.crosscutting.helpers.UtilUUID.DEFAULT_UUID;
import static co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl.SurrealQLUtil.quote;
import static co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl.SurrealQLUtil.recordIdLiteral;

@Repository
public class MessageSurrealRepositoryAdapterImpl extends SurrealCatalogSupport implements DataBaseMessageRepository {

    private static final String SURREAL_TABLE_MESSAGE_DOCUMENT = "message_data_collection";
    private static final String SURREAL_TABLE_MESSAGE_ENVIRONMENT_READMODEL = "message_environment_readmodel";

    public MessageSurrealRepositoryAdapterImpl(final Surreal surreal, final LoggingPortFactory loggerFactory) {
        super(surreal, loggerFactory.getLogger(MessageSurrealRepositoryAdapterImpl.class));
    }

    @Override
    public Optional<MessageData> findById(final String id) {
        final String messageId = "message:" + id;
        final String sql = "SELECT * FROM " + SURREAL_TABLE_MESSAGE_DOCUMENT
                + " WHERE message_id = " + quote(messageId)
                + " OR message_id = " + recordIdLiteral("message", id)
                + " LIMIT 1;";
        return findOneMessageDocument(sql);
    }

    @Override
    public SimplePage<MessageData> findMessagesByEnvironment(final String environmentId, final Pageable pageable, final MessageFilter filter) {
        long offset = pageable.getOffset();
        int pageSize = pageable.getPageSize();
        final String whereClause = environmentFilter(environmentId) + buildFilterConditions(filter);

        final String countSql = "SELECT COUNT() as count FROM " + SURREAL_TABLE_MESSAGE_ENVIRONMENT_READMODEL
                + " WHERE " + whereClause + ";";

        final String dataSql = "SELECT * FROM " + SURREAL_TABLE_MESSAGE_ENVIRONMENT_READMODEL
                + " WHERE " + whereClause
                + " ORDER BY message.code ASC"
                + " LIMIT " + pageSize + " START " + offset + ";";

        try {
            long totalElements = getCount(countSql);
            List<MessageData> messages = findMultipleMessageDocuments(dataSql);

            int totalPages = pageSize == 0 ? 0 : (int) Math.ceil((double) totalElements / pageSize);
            return SimplePage.of(
                    messages,
                    pageable.getPageNumber(),
                    pageSize,
                    totalElements,
                    totalPages
            );
        } catch (final RuntimeException ex) {
            log.error(CatalogPortStaticRef.getMessage(MessageCatalogCodeEnum.TCH_057.getCode()).formatted(environmentId), ex);
            throw ex;
        }
    }

    private static String buildFilterConditions(final MessageFilter filter) {
        return buildFilterConditions(filter, "message.");
    }

    // prefix "message." para la tabla por ambiente; vacio para el documento del mensaje.
    private static String buildFilterConditions(final MessageFilter filter, final String prefix) {
        if (filter == null || !filter.hasFilters()) {
            return "";
        }
        final var sb = new StringBuilder();
        appendContains(sb, prefix + "code", filter.getCode());
        appendContains(sb, prefix + "title", filter.getTitle());
        appendContains(sb, prefix + "category.name", filter.getCategory());
        appendContains(sb, prefix + "functionality.name", filter.getFunctionality());
        appendContains(sb, prefix + "type.name", filter.getType());
        appendContains(sb, prefix + "status.name", filter.getStatus());
        return sb.toString();
    }

    @Override
    public Optional<MessageData> findByCodeAndApplication(final String code, final String applicationId) {
        final String sql = "SELECT * FROM " + SURREAL_TABLE_MESSAGE_DOCUMENT
                + " WHERE code = " + quote(code)
                + " AND " + applicationFilter(applicationId)
                + " LIMIT 1;";
        final Optional<MessageData> message = findOneMessageDocument(sql);
        message.ifPresent(data -> attachEnvironments(List.of(data),
                environmentsByCode(applicationEnvironments(applicationId))));
        return message;
    }

    @Override
    public SimplePage<MessageData> findMessagesByApplication(final String applicationId,
                                                             final List<String> environmentIds,
                                                             final Pageable pageable,
                                                             final MessageFilter filter) {
        final long offset = pageable.getOffset();
        final int pageSize = pageable.getPageSize();
        try {
            final Map<UUID, String> applicationEnvironments = applicationEnvironments(applicationId);
            final Map<UUID, String> selected = selectedEnvironments(applicationEnvironments, environmentIds);
            final Map<String, List<MessageEnvironmentRefData>> environmentsByCode =
                    environmentsByCode(applicationEnvironments);
            final boolean filtered = environmentIds != null && !environmentIds.isEmpty();
            final Set<String> codes = environmentsByCode.entrySet().stream()
                    .filter(entry -> entry.getValue().stream().anyMatch(env -> selected.containsKey(env.getId())))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (filtered && codes.isEmpty()) {
                return SimplePage.of(List.of(), pageable.getPageNumber(), pageSize, 0, 0);
            }

            final String whereClause = applicationFilter(applicationId)
                    + (filtered ? " AND code IN [" + codes.stream().map(SurrealQLUtil::quote)
                    .collect(Collectors.joining(", ")) + "]" : "")
                    + buildFilterConditions(filter, "");
            final String countSql = "SELECT count() AS count FROM " + SURREAL_TABLE_MESSAGE_DOCUMENT
                    + " WHERE " + whereClause + " GROUP ALL;";
            final String dataSql = "SELECT * FROM " + SURREAL_TABLE_MESSAGE_DOCUMENT
                    + " WHERE " + whereClause
                    + " ORDER BY code ASC"
                    + " LIMIT " + pageSize + " START " + offset + ";";

            final long totalElements = getCount(countSql);
            final List<MessageData> messages = findMultipleMessageDocuments(dataSql);
            attachEnvironments(messages, environmentsByCode);
            final int totalPages = pageSize == 0 ? 0 : (int) Math.ceil((double) totalElements / pageSize);
            return SimplePage.of(messages, pageable.getPageNumber(), pageSize, totalElements, totalPages);
        } catch (final RuntimeException ex) {
            log.error(CatalogPortStaticRef.getMessage(MessageCatalogCodeEnum.TCH_057.getCode())
                    .formatted(applicationId), ex);
            throw ex;
        }
    }

    // Ambientes de la aplicacion desde la tabla fuente, que usa referencias tipadas.
    private Map<UUID, String> applicationEnvironments(final String applicationId) {
        final String sql = "SELECT id, type_id.name AS name FROM environment"
                + " WHERE application_id = " + recordIdLiteral("application", applicationId) + ";";
        final Map<UUID, String> environments = new LinkedHashMap<>();
        query(sql, CatalogPortStaticRef.getMessage(MessageCatalogCodeEnum.TCH_059.getCode()).formatted(sql),
                row -> environments.put(extractIdAsUUID(row.get("id")), stringOf(row.get("name"))));
        return environments;
    }

    private static Map<UUID, String> selectedEnvironments(final Map<UUID, String> applicationEnvironments,
                                                          final List<String> environmentIds) {
        if (environmentIds == null || environmentIds.isEmpty()) {
            return applicationEnvironments;
        }
        final Set<UUID> requested = environmentIds.stream().map(UUID::fromString).collect(Collectors.toSet());
        final Map<UUID, String> selected = new LinkedHashMap<>(applicationEnvironments);
        selected.keySet().retainAll(requested);
        return selected;
    }

    // Codigo de mensaje -> ambientes donde existe. El codigo es unico dentro de la aplicacion.
    private Map<String, List<MessageEnvironmentRefData>> environmentsByCode(final Map<UUID, String> environments) {
        final Map<String, List<MessageEnvironmentRefData>> result = new LinkedHashMap<>();
        if (environments.isEmpty()) {
            return result;
        }
        final String sql = "SELECT message.code AS code, environment_id FROM "
                + SURREAL_TABLE_MESSAGE_ENVIRONMENT_READMODEL
                + " WHERE " + environments.keySet().stream()
                .map(id -> "string::contains(<string> environment_id, " + quote(id.toString()) + ")")
                .collect(Collectors.joining(" OR ", "(", ")")) + ";";
        query(sql, CatalogPortStaticRef.getMessage(MessageCatalogCodeEnum.TCH_059.getCode()).formatted(sql), row -> {
            final UUID environmentId = extractIdAsUUID(row.get("environment_id"));
            final String code = stringOf(row.get("code"));
            if (!code.isBlank() && environments.containsKey(environmentId)) {
                final List<MessageEnvironmentRefData> list = result.computeIfAbsent(code, key -> new ArrayList<>());
                if (list.stream().noneMatch(env -> env.getId().equals(environmentId))) {
                    list.add(new MessageEnvironmentRefData(environmentId, environments.get(environmentId)));
                }
            }
            return code;
        });
        return result;
    }

    private static void attachEnvironments(final List<MessageData> messages,
                                           final Map<String, List<MessageEnvironmentRefData>> environmentsByCode) {
        messages.forEach(message ->
                message.setEnvironments(environmentsByCode.getOrDefault(message.getCode(), List.of())));
    }

    private static String applicationFilter(final String applicationId) {
        return "string::contains(<string> application_id, "
                + quote(UUID.fromString(applicationId).toString()) + ")";
    }

    private static void appendContains(final StringBuilder sb, final String field, final String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        sb.append(" AND string::contains(string::lowercase(").append(field).append("), ")
          .append(quote(value.toLowerCase())).append(")");
    }

    @Override
    public Optional<MessageData> findByCode(final String code) {
        final String sql = "SELECT * FROM " + SURREAL_TABLE_MESSAGE_DOCUMENT
                + " WHERE code = " + quote(code)
                + " LIMIT 1;";
        return findOneMessageDocument(sql);
    }

    @Override
    public Optional<MessageData> findMessageByCodeAndEnvironment(final String code, final String environmentId) {
        final String sql = "SELECT * FROM " + SURREAL_TABLE_MESSAGE_ENVIRONMENT_READMODEL
                + " WHERE " + environmentFilter(environmentId)
                + " AND message.code = " + quote(code)
                + " LIMIT 1;";
        return findOneMessageDocument(sql);
    }

    private Optional<MessageData> findOneMessageDocument(final String sql) {
        return queryOne(sql, CatalogPortStaticRef.getMessage(MessageCatalogCodeEnum.TCH_058.getCode()).formatted(sql), this::toMessageDataFromDocument);
    }

    private List<MessageData> findMultipleMessageDocuments(final String sql) {
        return query(sql, CatalogPortStaticRef.getMessage(MessageCatalogCodeEnum.TCH_059.getCode()).formatted(sql), this::toMessageDataFromDocument);
    }

    private long getCount(final String sql) {
        try {
            final Response response = surreal.query(sql);
            if (isNullObject(response) || response.size() == 0) {
                return 0;
            }
            final Value statementResult = response.take(0);
            if (isNullObject(statementResult) || !statementResult.isArray()) {
                return 0;
            }
            final Array array = statementResult.getArray();
            if (array.len() == 0) {
                return 0;
            }
            final Value first = array.get(0);
            if (isNullObject(first) || !first.isObject()) {
                return 0;
            }
            final Object obj = first.getObject();
            final Value count = obj.get("count");
            if (isNullObject(count) || count.isNull()) {
                return 0;
            }
            try {
                if (count.isString()) {
                    return Long.parseLong(count.getString());
                }
                return Long.parseLong(count.toString());
            } catch (final Exception e) {
                return 0;
            }
        } catch (final RuntimeException ex) {
            log.error(CatalogPortStaticRef.getMessage(MessageCatalogCodeEnum.TCH_060.getCode()).formatted(sql), ex);
            return 0;
        }
    }

    private static String environmentFilter(final String environmentId) {
        final String environmentRecordId = "environment:" + environmentId;
        return "(environment_id = " + quote(environmentRecordId)
                + " OR environment_id = " + recordIdLiteral("environment", environmentId) + ")";
    }

    private MessageData toMessageDataFromDocument(final Object obj) {
        final Object message = objectOf(obj.get("message")).orElse(obj);
        final MessageData data = MessageData.build();
        data.setId(extractIdAsUUID(message.get("id")));
        data.setCode(stringOf(message.get("code")));
        data.setTitle(stringOf(message.get("title")));
        data.setContent(stringOf(message.get("content")));
        data.setCreatedAt(instantOf(message.get("created_at")));
        data.setApplication(stringOf(message.get("application")));
        data.setType(new MessageTypeData(catalogIdOf(message.get("type")), nameOf(message.get("type"))));
        data.setCategory(new MessageCategoryData(catalogIdOf(message.get("category")), nameOf(message.get("category"))));
        data.setStatus(new StatusMessageData(catalogIdOf(message.get("status")), nameOf(message.get("status"))));
        data.setFunctionality(new FunctionalityData(catalogIdOf(message.get("functionality")),
                nameOf(message.get("functionality")), ApplicationData.build()));
        return data;
    }

    private static Optional<Object> objectOf(final Value value) {
        if (isNullObject(value) || !value.isObject()) {
            return Optional.empty();
        }
        return Optional.of(value.getObject());
    }

    private static Instant instantOf(final Value value) {
        if (isNullObject(value) || value.isNull() || value.isNone()) {
            return null;
        }
        if (value.isDateTime()) {
            return value.getDateTime().toInstant();
        }
        return Instant.parse(value.getString());
    }

    private static UUID catalogIdOf(final Value value) {
        if (isNullObject(value) || !value.isObject()) {
            return DEFAULT_UUID;
        }
        return extractCatalogId(value.getObject().get("id"));
    }

    private static String nameOf(final Value value) {
        if (isNullObject(value) || value.isNull() || value.isNone()) {
            return "";
        }
        if (value.isObject()) {
            return stringOf(value.getObject().get("name"));
        }
        return stringOf(value);
    }
}
