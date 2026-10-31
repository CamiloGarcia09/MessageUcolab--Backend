package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl;

import co.edu.uco.application.common.catalog.CatalogPortStaticRef;
import co.edu.uco.application.common.catalog.strategy.MessageCatalogStrategy;
import co.edu.uco.application.secondaryports.event.MessageEvent;
import co.edu.uco.application.secondaryports.event.Publisher;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.helpers.UtilText;
import com.surrealdb.Array;
import com.surrealdb.Entry;
import com.surrealdb.RecordId;
import com.surrealdb.Response;
import com.surrealdb.Surreal;
import com.surrealdb.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static co.edu.uco.crosscutting.helpers.UtilObject.isNullObject;
import static co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl.SurrealQLUtil.quote;
import static co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl.SurrealQLUtil.recordIdLiteral;

@Component
@ConditionalOnProperty(prefix = "surreal.projection", name = "enabled", havingValue = "true", matchIfMissing = true)
public final class SurrealDomainEventProjectionConsumer {

    private static final String TABLE_DOMAIN_EVENTS = "domain_events";
    private static final String COLLECTION_DOMAIN_EVENT_DOCUMENT = "domain_event_document";
    private static final String COLLECTION_APPLICATION_DOCUMENT = "application_document";
    private static final String COLLECTION_ENVIRONMENT_DOCUMENT = "environment_document";
    private static final String COLLECTION_MESSAGE_DOCUMENT = "message_data_collection";
    private static final String COLLECTION_MESSAGE_ENVIRONMENT_READMODEL = "message_environment_readmodel";
    private static final String PROJECTION_STATUS_PROCESSED = "processed";
    private static final String PROJECTION_STATUS_FAILED = "failed";
    private static final String DELETE_SUFFIX = "_DELETED";

    private final LoggingPort log;
    private final Surreal surreal;
    private final int batchSize;
    private final Publisher<MessageEvent> publisher;
    private final MessageCatalogStrategy messageCatalogStrategy;

    public SurrealDomainEventProjectionConsumer(
            final Surreal surreal,
            @org.springframework.beans.factory.annotation.Value("${surreal.projection.batch-size:100}") final int batchSize,
            final LoggingPortFactory loggerFactory,
            final Publisher<MessageEvent> publisher,
            final MessageCatalogStrategy messageCatalogStrategy) {
        this.log = loggerFactory.getLogger(SurrealDomainEventProjectionConsumer.class);
        this.surreal = surreal;
        this.batchSize = Math.max(1, batchSize);
        this.publisher = publisher;
        this.messageCatalogStrategy = messageCatalogStrategy;
    }

    @Scheduled(
            initialDelayString = "${surreal.projection.initial-delay-ms:2000}",
            fixedDelayString = "${surreal.projection.poll-delay-ms:2000}"
    )
    public void consumePendingDomainEvents() {
        final String sql = "SELECT * FROM " + TABLE_DOMAIN_EVENTS
                + " WHERE projection_status = NONE OR projection_status != " + quote(PROJECTION_STATUS_PROCESSED)
                + " ORDER BY created_at ASC LIMIT " + batchSize + ";";

        final List<DomainEvent> events = queryObjects(sql).stream()
                .map(DomainEvent::from)
                .toList();

        if (events.isEmpty()) {
            return;
        }

        log.debug(CatalogPortStaticRef.getMessage(MessageCatalogCodeEnum.TCH_063.getCode()).formatted(events.size()));
        events.forEach(this::project);
    }

    private void project(final DomainEvent event) {
        try {
            upsertRawDomainEventDocument(event);
            projectAggregateDocument(event);
            markProcessed(event);
        } catch (final RuntimeException exception) {
            log.warn(CatalogPortStaticRef.getMessage(MessageCatalogCodeEnum.TCH_064.getCode()).formatted(event.fullId()), exception);
            markFailed(event, exception);
        }
    }

    private void projectAggregateDocument(final DomainEvent event) {
        final String aggregateType = event.aggregateType().toLowerCase(Locale.ROOT);
        switch (aggregateType) {
            case "application" -> projectApplication(event);
            case "environment" -> projectEnvironment(event);
            case "message" -> projectMessage(event);
            case "message_environment" -> projectMessageEnvironment(event);
            default -> log.debug(CatalogPortStaticRef.getMessage(MessageCatalogCodeEnum.TCH_065.getCode()).formatted(event.fullId()));
        }
    }

    private void projectApplication(final DomainEvent event) {
        final RecordRef aggregate = RecordRef.from(event.aggregateId()).orElseThrow();
        if (event.isDelete()) {
            deleteDocument(COLLECTION_APPLICATION_DOCUMENT, aggregate.id());
            return;
        }

        final Optional<com.surrealdb.Object> application = findByRecordId(event.aggregateId());
        if (application.isEmpty()) {
            deleteDocument(COLLECTION_APPLICATION_DOCUMENT, aggregate.id());
            return;
        }

        final com.surrealdb.Object record = application.get();
        final String organizationId = recordString(record.get("organization_id"));
        final String languageId = recordString(record.get("language_id"));
        final String stateId = recordString(record.get("state_id"));

        final String content = "{ "
                + projectionHeader(event, "application_id")
                + ", name: " + literal(record.get("name"))
                + ", organization_id: " + quote(organizationId)
                + ", organization: " + catalogLiteral(
                        organizationId, findByRecordId(organizationId), "name")
                + ", language_id: " + quote(languageId)
                + ", language: " + catalogLiteral(languageId, findByRecordId(languageId), "language", "code")
                + ", state_id: " + quote(stateId)
                + ", state: " + catalogLiteral(stateId, findByRecordId(stateId), "name")
                + ", created_at: " + literal(record.get("created_at"))
                + ", updated_at: " + literal(record.get("updated_at"))
                + projectionFooter(event)
                + " }";

        upsertDocument(COLLECTION_APPLICATION_DOCUMENT, aggregate.id(), content);
    }

    private void projectEnvironment(final DomainEvent event) {
        final RecordRef aggregate = RecordRef.from(event.aggregateId()).orElseThrow();
        if (event.isDelete()) {
            deleteDocument(COLLECTION_ENVIRONMENT_DOCUMENT, aggregate.id());
            return;
        }

        final Optional<com.surrealdb.Object> environment = findByRecordId(event.aggregateId());
        if (environment.isEmpty()) {
            deleteDocument(COLLECTION_ENVIRONMENT_DOCUMENT, aggregate.id());
            return;
        }

        final com.surrealdb.Object record = environment.get();
        final String applicationId = recordString(record.get("application_id"));
        final String typeId = recordString(record.get("type_id"));
        final String stateId = recordString(record.get("state_id"));

        final String content = "{ "
                + projectionHeader(event, "environment_id")
                + ", name: " + catalogFieldLiteral(typeId, "name")
                + ", application_id: " + quote(applicationId)
                + ", application: " + catalogLiteral(applicationId, findByRecordId(applicationId), "name")
                + ", type_id: " + quote(typeId)
                + ", type: " + catalogLiteral(typeId, findByRecordId(typeId), "name")
                + ", state_id: " + quote(stateId)
                + ", state: " + catalogLiteral(stateId, findByRecordId(stateId), "name")
                + ", created_at: " + literal(record.get("created_at"))
                + ", updated_at: " + literal(record.get("updated_at"))
                + projectionFooter(event)
                + " }";

        upsertDocument(COLLECTION_ENVIRONMENT_DOCUMENT, aggregate.id(), content);
    }

    private void projectMessage(final DomainEvent event) {
        final RecordRef aggregate = RecordRef.from(event.aggregateId()).orElseThrow();
        if (event.isDelete()) {
            deleteDocument(COLLECTION_MESSAGE_DOCUMENT, aggregate.id());
            return;
        }

        final Optional<com.surrealdb.Object> message = findByRecordId(event.aggregateId());
        if (message.isEmpty()) {
            deleteDocument(COLLECTION_MESSAGE_DOCUMENT, aggregate.id());
            return;
        }

        final com.surrealdb.Object record = message.get();
        final String typeId = recordString(record.get("type_id"));
        final String categoryId = recordString(record.get("category_id"));
        final String statusId = recordString(record.get("status_id"));
        final String applicationId = recordString(record.get("application_id"));
        final String functionalityId = recordString(record.get("functionality_id"));
        final String applicationName = applicationName(applicationId);

        final String content = "{ "
                + projectionHeader(event, "message_id")
                + ", code: " + literal(record.get("code"))
                + ", title: " + literal(record.get("title"))
                + ", content: " + literal(record.get("content"))
                + ", type_id: " + quote(typeId)
                + ", type: " + catalogLiteral(typeId, findByRecordId(typeId), "name")
                + ", category_id: " + quote(categoryId)
                + ", category: " + catalogLiteral(categoryId, findByRecordId(categoryId), "name")
                + ", status_id: " + quote(statusId)
                + ", status: " + catalogLiteral(statusId, findByRecordId(statusId), "name")
                + ", application_id: " + quote(applicationId)
                + ", application: " + quote(applicationName)
                + ", application_ref: " + catalogLiteral(applicationId, findByRecordId(applicationId), "name")
                + ", functionality_id: " + quote(functionalityId)
                + ", functionality: " + catalogLiteral(functionalityId, findByRecordId(functionalityId), "name")
                + ", created_at: " + literal(record.get("created_at"))
                + ", updated_at: " + literal(record.get("updated_at"))
                + projectionFooter(event)
                + " }";

        upsertDocument(COLLECTION_MESSAGE_DOCUMENT, aggregate.id(), content);
        propagateMessageUpdateToEnvironmentReadmodel(event.aggregateId(), record);
    }

    private void propagateMessageUpdateToEnvironmentReadmodel(final String messageRecordId,
                                                              final com.surrealdb.Object record) {
        final String typeId = recordString(record.get("type_id"));
        final String categoryId = recordString(record.get("category_id"));
        final String statusId = recordString(record.get("status_id"));
        final String functionalityId = recordString(record.get("functionality_id"));

        final String sql = "UPDATE " + COLLECTION_MESSAGE_ENVIRONMENT_READMODEL
                + " SET message.title = " + literal(record.get("title"))
                + ", message.content = " + literal(record.get("content"))
                + ", message.type = " + catalogLiteral(typeId, findByRecordId(typeId), "name")
                + ", message.category = " + catalogLiteral(categoryId, findByRecordId(categoryId), "name")
                + ", message.status = " + catalogLiteral(statusId, findByRecordId(statusId), "name")
                + ", message.functionality = " + catalogLiteral(functionalityId, findByRecordId(functionalityId), "name")
                + ", message.updated_at = " + literal(record.get("updated_at"))
                + " WHERE message_id = " + quote(messageRecordId) + ";";
        execute(sql);
        final String messageCode = stringOf(record.get("code"));
        messageCatalogStrategy.evictMessage(messageCode);
        publishMessageUpdatedEventsForEnvironments(messageRecordId, messageCode);
    }

    private void publishMessageUpdatedEventsForEnvironments(final String messageRecordId, final String messageCode) {
        final String querySql = "SELECT environment_id FROM " + COLLECTION_MESSAGE_ENVIRONMENT_READMODEL
                + " WHERE message_id = " + quote(messageRecordId) + ";";
        queryObjects(querySql).forEach(row -> {
            final String environmentId = recordString(row.get("environment_id"));
            if (!environmentId.isBlank()) {
                publisher.publish(new MessageEvent(
                        UUID.randomUUID().toString(),
                        "MessageUpdated",
                        cleanRecordId(environmentId),
                        messageCode
                ));
            }
        });
    }

    private void projectMessageEnvironment(final DomainEvent event) {
        final RecordRef aggregate = RecordRef.from(event.aggregateId()).orElseThrow();
        if (event.isDelete()) {
            final Optional<com.surrealdb.Object> existing = findOne(
                    "SELECT * FROM " + recordIdLiteral(COLLECTION_MESSAGE_ENVIRONMENT_READMODEL, aggregate.id()) + ";");
            deleteDocument(COLLECTION_MESSAGE_ENVIRONMENT_READMODEL, aggregate.id());
            existing.ifPresent(doc -> {
                final String environmentId = recordString(doc.get("environment_id"));
                final Value messageValue = doc.get("message");
                final String messageCode = (!isNullObject(messageValue) && messageValue.isObject())
                        ? stringOf(messageValue.getObject().get("code"))
                        : "";
                if (!messageCode.isBlank()) {
                    messageCatalogStrategy.evictMessage(messageCode);
                }
                if (!environmentId.isBlank()) {
                    publisher.publish(new MessageEvent(
                            UUID.randomUUID().toString(),
                            "MessageDeleted",
                            cleanRecordId(environmentId),
                            messageCode
                    ));
                }
            });
            return;
        }

        final Optional<com.surrealdb.Object> messageEnvironment = findByRecordId(event.aggregateId());
        if (messageEnvironment.isEmpty()) {
            deleteDocument(COLLECTION_MESSAGE_ENVIRONMENT_READMODEL, aggregate.id());
            return;
        }

        final com.surrealdb.Object record = messageEnvironment.get();
        final String messageId = recordString(record.get("message_id"));
        final String environmentId = recordString(record.get("environment_id"));
        final String stateId = recordString(record.get("state_data_id"));

        final Optional<com.surrealdb.Object> message = findByRecordId(messageId);
        final String messageLiteral = message
                .map(value -> messageEmbeddedLiteral(messageId, value))
                .orElse("{ id: " + quote(messageId) + " }");

        final String content = "{ "
                + projectionHeader(event, "message_environment_id")
                + ", message_id: " + quote(messageId)
                + ", environment_id: " + quote(environmentId)
                + ", state_data_id: " + quote(stateId)
                + ", environment: " + environmentLiteral(environmentId)
                + ", message: " + messageLiteral
                + ", status: " + catalogLiteral(stateId, findByRecordId(stateId), "name")
                + ", created_at: " + literal(record.get("created_at"))
                + ", updated_at: " + literal(record.get("updated_at"))
                + projectionFooter(event)
                + " }";

        upsertDocument(COLLECTION_MESSAGE_ENVIRONMENT_READMODEL, aggregate.id(), content);
        publishProjectedMessageEvent(event, environmentId, message.map(value -> stringOf(value.get("code"))).orElse(""));
    }

    private void publishProjectedMessageEvent(final DomainEvent event, final String environmentId,
                                              final String messageCode) {
        publisher.publish(new MessageEvent(
                UUID.randomUUID().toString(),
                toMessageEventType(event.eventType()),
                cleanRecordId(environmentId),
                messageCode
        ));
    }

    private static String toMessageEventType(final String eventType) {
        if (eventType == null || eventType.isBlank()) {
            return "MessageChanged";
        }
        final String normalized = eventType.toUpperCase(Locale.ROOT);
        if (normalized.endsWith("_CREATED")) {
            return "MessageCreated";
        }
        if (normalized.endsWith("_UPDATED")) {
            return "MessageUpdated";
        }
        if (normalized.endsWith("_DELETED")) {
            return "MessageDeleted";
        }
        return "MessageChanged";
    }

    private static String cleanRecordId(final String recordId) {
        return RecordRef.from(recordId).map(RecordRef::id).orElse(recordId);
    }

    private String messageEmbeddedLiteral(final String messageId, final com.surrealdb.Object message) {
        final String typeId = recordString(message.get("type_id"));
        final String categoryId = recordString(message.get("category_id"));
        final String statusId = recordString(message.get("status_id"));
        final String applicationId = recordString(message.get("application_id"));
        final String functionalityId = recordString(message.get("functionality_id"));
        return "{ id: " + quote(messageId)
                + ", code: " + literal(message.get("code"))
                + ", title: " + literal(message.get("title"))
                + ", content: " + literal(message.get("content"))
                + ", type: " + catalogLiteral(typeId, findByRecordId(typeId), "name")
                + ", category: " + catalogLiteral(categoryId, findByRecordId(categoryId), "name")
                + ", status: " + catalogLiteral(statusId, findByRecordId(statusId), "name")
                + ", application: " + quote(applicationName(applicationId))
                + ", functionality: " + catalogLiteral(functionalityId, findByRecordId(functionalityId), "name")
                + ", created_at: " + literal(message.get("created_at"))
                + ", updated_at: " + literal(message.get("updated_at"))
                + " }";
    }

    private String applicationName(final String applicationId) {
        return findByRecordId(applicationId)
                .map(application -> stringOf(application.get("name")))
                .orElse("");
    }

    private String environmentLiteral(final String environmentId) {
        final Optional<com.surrealdb.Object> environment = findByRecordId(environmentId);
        final String typeId = environment.map(value -> recordString(value.get("type_id"))).orElse("");
        return "{ id: " + quote(environmentId) + ", name: " + catalogFieldLiteral(typeId, "name") + " }";
    }

    private String catalogFieldLiteral(final String recordId, final String fieldName) {
        return findByRecordId(recordId)
                .map(record -> literal(record.get(fieldName)))
                .orElse("NONE");
    }

    private void upsertRawDomainEventDocument(final DomainEvent event) {
        final String content = "{ "
                + "source_id: " + quote(event.fullId())
                + ", domain_event_id: " + quote(event.fullId())
                + ", event_type: " + quote(event.eventType())
                + ", aggregate_id: " + quote(event.aggregateId())
                + ", aggregate_type: " + quote(event.aggregateType())
                + ", aggregate_version: " + event.version()
                + ", payload: " + literal(event.payload())
                + ", metadata: " + literal(event.metadata())
                + ", occurred_at: " + literal(event.createdAt())
                + ", generated_at: time::now()"
                + " }";
        upsertDocument(COLLECTION_DOMAIN_EVENT_DOCUMENT, event.id(), content);
    }

    private String projectionHeader(final DomainEvent event, final String aggregateFieldName) {
        return "source_id: " + quote(event.aggregateId())
                + ", " + aggregateFieldName + ": " + quote(event.aggregateId());
    }

    private String projectionFooter(final DomainEvent event) {
        return ", projection_version: " + event.version()
                + ", domain_event_id: " + quote(event.fullId())
                + ", event_type: " + quote(event.eventType())
                + ", generated_at: time::now()";
    }

    private String catalogLiteral(final String recordId,
                                  final Optional<com.surrealdb.Object> catalog,
                                  final String... fieldNames) {
        final StringBuilder builder = new StringBuilder("{ id: ").append(quote(recordId));
        catalog.ifPresent(value -> {
            for (final String fieldName : fieldNames) {
                builder.append(", ")
                        .append(fieldName)
                        .append(": ")
                        .append(literal(value.get(fieldName)));
            }
        });
        return builder.append(" }").toString();
    }

    private void upsertDocument(final String collection, final String id, final String content) {
        execute("UPSERT " + recordIdLiteral(collection, id) + " CONTENT " + content + ";");
    }

    private void deleteDocument(final String collection, final String id) {
        execute("DELETE " + recordIdLiteral(collection, id) + ";");
    }

    private Optional<com.surrealdb.Object> findByRecordId(final String recordId) {
        return RecordRef.from(recordId)
                .flatMap(ref -> findOne("SELECT * FROM " + recordIdLiteral(ref.table(), ref.id()) + " LIMIT 1;"));
    }

    private Optional<com.surrealdb.Object> findOne(final String sql) {
        final List<com.surrealdb.Object> rows = queryObjects(sql);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.get(0));
    }

    private List<com.surrealdb.Object> queryObjects(final String sql) {
        final Response response = query(sql);
        final List<com.surrealdb.Object> values = new ArrayList<>();
        if (isNullObject(response) || response.size() == 0) {
            return values;
        }
        final Value statement = response.take(0);
        if (isNullObject(statement) || !statement.isArray()) {
            return values;
        }
        final Array array = statement.getArray();
        for (int index = 0; index < array.len(); index++) {
            final Value value = array.get(index);
            if (!isNullObject(value) && value.isObject()) {
                values.add(value.getObject());
            }
        }
        return values;
    }

    private Response query(final String sql) {
        synchronized (surreal) {
            return surreal.query(sql);
        }
    }

    private void execute(final String sql) {
        query(sql);
    }

    private void markProcessed(final DomainEvent event) {
        mark(event, PROJECTION_STATUS_PROCESSED, "NONE");
    }

    private void markFailed(final DomainEvent event, final RuntimeException exception) {
        final String message = Optional.ofNullable(exception.getMessage()).orElse(exception.getClass().getSimpleName());
        mark(event, PROJECTION_STATUS_FAILED, quote(message));
    }

    private void mark(final DomainEvent event, final String status, final String errorLiteral) {
        execute("UPDATE " + recordIdLiteral(TABLE_DOMAIN_EVENTS, event.id())
                + " SET projection_status = " + quote(status)
                + ", projected_at = time::now()"
                + ", projection_error = " + errorLiteral
                + ";");
    }

    private static String literal(final Value value) {
        if (isNullObject(value) || value.isNull() || value.isNone()) {
            return "NONE";
        }
        if (value.isString()) {
            return quote(value.getString());
        }
        if (value.isBoolean()) {
            return Boolean.toString(value.getBoolean());
        }
        if (value.isLong()) {
            return Long.toString(value.getLong());
        }
        if (value.isDouble()) {
            return Double.toString(value.getDouble());
        }
        if (value.isBigDecimal()) {
            return value.getBigDecimal().toPlainString();
        }
        if (value.isUuid()) {
            return quote(value.getUuid().toString());
        }
        if (value.isRecordId()) {
            return quote(recordIdToString(value.getRecordId()));
        }
        if (value.isDateTime()) {
            return "d'" + value.getDateTime().withZoneSameInstant(ZoneOffset.UTC)
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) + "'";
        }
        if (value.isArray()) {
            return arrayLiteral(value.getArray());
        }
        if (value.isObject()) {
            return objectLiteral(value.getObject());
        }
        return quote(value.toString());
    }

    private static String arrayLiteral(final Array array) {
        final StringBuilder builder = new StringBuilder("[");
        for (int index = 0; index < array.len(); index++) {
            if (index > 0) {
                builder.append(", ");
            }
            builder.append(literal(array.get(index)));
        }
        return builder.append("]").toString();
    }

    private static String objectLiteral(final com.surrealdb.Object object) {
        final StringBuilder builder = new StringBuilder("{ ");
        int index = 0;
        for (final Entry entry : object) {
            if (index > 0) {
                builder.append(", ");
            }
            builder.append(keyLiteral(entry.getKey()))
                    .append(": ")
                    .append(literal(entry.getValue()));
            index++;
        }
        return builder.append(" }").toString();
    }

    private static String keyLiteral(final String key) {
        if (!isNullObject(key) && key.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            return key;
        }
        final String safe = UtilText.getDefault(key).replace("`", "");
        return "`" + safe + "`";
    }

    private static String recordString(final Value value) {
        final String text = stringOf(value);
        if (!text.isBlank()) {
            return text;
        }
        if (!isNullObject(value) && value.isObject()) {
            return recordString(value.getObject().get("id"));
        }
        return "";
    }

    private static String stringOf(final Value value) {
        if (isNullObject(value) || value.isNull() || value.isNone()) {
            return "";
        }
        if (value.isString()) {
            return value.getString();
        }
        if (value.isRecordId()) {
            return recordIdToString(value.getRecordId());
        }
        if (value.isUuid()) {
            return value.getUuid().toString();
        }
        if (value.isDateTime()) {
            return value.getDateTime().withZoneSameInstant(ZoneOffset.UTC)
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        }
        return value.toString();
    }

    private static long longOf(final Value value) {
        if (isNullObject(value) || value.isNull() || value.isNone()) {
            return 0L;
        }
        if (value.isLong()) {
            return value.getLong();
        }
        if (value.isDouble()) {
            return (long) value.getDouble();
        }
        if (value.isBigDecimal()) {
            return value.getBigDecimal().longValue();
        }
        try {
            return Long.parseLong(stringOf(value));
        } catch (final NumberFormatException exception) {
            return 0L;
        }
    }

    private static String recordIdToString(final RecordId recordId) {
        return recordId.getTable() + ":" + cleanIdPart(recordId.getId().toString());
    }

    private record DomainEvent(String id,
                               String eventType,
                               String aggregateId,
                               String aggregateType,
                               long version,
                               Value payload,
                               Value metadata,
                               Value createdAt) {
        static DomainEvent from(final com.surrealdb.Object object) {
            return new DomainEvent(
                    recordIdPart(object.get("id")),
                    stringOf(object.get("event_type")),
                    stringOf(object.get("aggregate_id")),
                    stringOf(object.get("aggregate_type")),
                    longOf(object.get("version")),
                    object.get("payload"),
                    object.get("metadata"),
                    object.get("created_at")
            );
        }

        String fullId() {
            return TABLE_DOMAIN_EVENTS + ":" + id;
        }

        boolean isDelete() {
            return eventType.endsWith(DELETE_SUFFIX);
        }
    }

    private record RecordRef(String table, String id) {
        static Optional<RecordRef> from(final String recordId) {
            if (isNullObject(recordId) || recordId.isBlank()) {
                return Optional.empty();
            }
            final int separator = recordId.indexOf(':');
            if (separator <= 0 || separator == recordId.length() - 1) {
                return Optional.empty();
            }
            final String table = recordId.substring(0, separator).trim();
            final String id = cleanIdPart(recordId.substring(separator + 1).trim());
            if (table.isBlank() || id.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(new RecordRef(table, id));
        }
    }

    private static String recordIdPart(final Value value) {
        if (isNullObject(value) || value.isNull() || value.isNone()) {
            return "";
        }
        if (value.isRecordId()) {
            return cleanIdPart(value.getRecordId().getId().toString());
        }
        return RecordRef.from(stringOf(value)).map(RecordRef::id).orElse(stringOf(value));
    }

    private static String cleanIdPart(final String id) {
        if (isNullObject(id)) {
            return "";
        }
        if (id.length() >= 2 && id.startsWith("`") && id.endsWith("`")) {
            return id.substring(1, id.length() - 1);
        }
        if (id.length() >= 2 && id.startsWith("⟨") && id.endsWith("⟩")) {
            return id.substring(1, id.length() - 1);
        }
        return id;
    }
}
