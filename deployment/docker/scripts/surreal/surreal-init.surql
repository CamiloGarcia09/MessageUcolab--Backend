OPTION IMPORT;

-- =============================================================
-- MESSAGE UCOLAB - CQRS + EVENT SOURCING + CHANGEFEED
-- =============================================================

USE NS messageucolab;
USE DB messageucolab;

-- =============================================================
-- DOMAIN EVENTS
-- =============================================================

DEFINE TABLE IF NOT EXISTS domain_events SCHEMAFULL;

DEFINE FIELD IF NOT EXISTS event_type     ON domain_events TYPE string;
DEFINE FIELD IF NOT EXISTS aggregate_id   ON domain_events TYPE string;
DEFINE FIELD IF NOT EXISTS aggregate_type ON domain_events TYPE string;
DEFINE FIELD IF NOT EXISTS version        ON domain_events TYPE int;
DEFINE FIELD OVERWRITE payload            ON domain_events TYPE object FLEXIBLE;
DEFINE FIELD OVERWRITE metadata           ON domain_events TYPE object FLEXIBLE;
DEFINE FIELD OVERWRITE created_at         ON domain_events TYPE datetime DEFAULT time::now() READONLY;
DEFINE FIELD IF NOT EXISTS created_by     ON domain_events TYPE option<string>;
DEFINE FIELD IF NOT EXISTS correlation_id ON domain_events TYPE option<string>;
DEFINE FIELD IF NOT EXISTS causation_id   ON domain_events TYPE option<string>;
DEFINE FIELD IF NOT EXISTS projection_status ON domain_events TYPE option<string> DEFAULT "pending";
DEFINE FIELD IF NOT EXISTS projected_at      ON domain_events TYPE option<datetime>;
DEFINE FIELD IF NOT EXISTS projection_error  ON domain_events TYPE option<string>;

DEFINE INDEX IF NOT EXISTS domain_events_aggregate_idx
ON domain_events FIELDS aggregate_id, aggregate_type;

DEFINE INDEX IF NOT EXISTS domain_events_event_type_idx
ON domain_events FIELDS event_type;

DEFINE INDEX IF NOT EXISTS domain_events_created_at_idx
ON domain_events FIELDS created_at;

DEFINE INDEX IF NOT EXISTS domain_events_projection_idx
ON domain_events FIELDS projection_status, created_at;

-- =============================================================
-- CATALOGS
-- =============================================================

DEFINE TABLE IF NOT EXISTS language_base SCHEMAFULL;

DEFINE FIELD IF NOT EXISTS language ON language_base TYPE string;
DEFINE FIELD IF NOT EXISTS code     ON language_base TYPE string;

DEFINE INDEX IF NOT EXISTS language_base_code_idx
ON language_base FIELDS code UNIQUE;

DELETE language_base:en;
DELETE language_base:es;
DELETE language_base:fr;
DELETE language_base:`6319ca87-47b3-4e0e-994f-3166378988f2`;
DELETE language_base:`bed4d574-4c7f-4d95-bc12-6dbbcdbbf4fc`;
DELETE language_base:`8cc7ae54-db8c-4ef1-929b-22e17de58129`;

UPSERT language_base:`6319ca87-47b3-4e0e-994f-3166378988f2` CONTENT {
    language: "English",
    code: "EN"
};

UPSERT language_base:`bed4d574-4c7f-4d95-bc12-6dbbcdbbf4fc` CONTENT {
    language: "Spanish",
    code: "ES"
};

UPSERT language_base:`8cc7ae54-db8c-4ef1-929b-22e17de58129` CONTENT {
    language: "French",
    code: "FR"
};

-- =============================================================
-- APPLICATION STATE
-- =============================================================

DEFINE TABLE IF NOT EXISTS application_state SCHEMAFULL;

DEFINE FIELD IF NOT EXISTS name
ON application_state TYPE string;

DEFINE INDEX IF NOT EXISTS application_state_name_idx
ON application_state FIELDS name UNIQUE;

DELETE application_state:active;
DELETE application_state:inactive;
DELETE application_state:`dfb7a7cb-66f6-4b73-a1d8-803336f72ebe`;
DELETE application_state:`2a0ddacd-2dcc-47e2-b56f-03db5207c443`;

UPSERT application_state:`dfb7a7cb-66f6-4b73-a1d8-803336f72ebe` CONTENT {
    name: "Active"
};

UPSERT application_state:`2a0ddacd-2dcc-47e2-b56f-03db5207c443` CONTENT {
    name: "Inactive"
};

-- =============================================================
-- ORGANIZATION
-- =============================================================

DEFINE TABLE IF NOT EXISTS app_user SCHEMAFULL;
DEFINE FIELD IF NOT EXISTS identity_source ON app_user TYPE string;
DEFINE FIELD IF NOT EXISTS tenant_id ON app_user TYPE string;
DEFINE FIELD IF NOT EXISTS subject ON app_user TYPE string;
DEFINE FIELD IF NOT EXISTS email ON app_user TYPE string;
DEFINE FIELD IF NOT EXISTS name ON app_user TYPE string;
DEFINE INDEX IF NOT EXISTS app_user_identity_idx ON app_user FIELDS identity_source, tenant_id, subject UNIQUE;

DEFINE TABLE IF NOT EXISTS organization SCHEMAFULL CHANGEFEED 7d;

DEFINE FIELD IF NOT EXISTS owner_user_id ON organization TYPE option<record<app_user>>;

DEFINE FIELD IF NOT EXISTS name
ON organization TYPE string;

DEFINE FIELD OVERWRITE created_at
ON organization TYPE datetime DEFAULT time::now() READONLY;

DEFINE FIELD IF NOT EXISTS updated_at
ON organization TYPE datetime VALUE time::now();

REMOVE INDEX IF EXISTS organization_name_idx ON organization;
DEFINE INDEX IF NOT EXISTS organization_owner_name_idx
ON organization FIELDS owner_user_id, name UNIQUE;
DEFINE INDEX IF NOT EXISTS organization_owner_idx ON organization FIELDS owner_user_id;

-- =============================================================
-- APPLICATION
-- =============================================================

DEFINE TABLE IF NOT EXISTS application SCHEMAFULL CHANGEFEED 7d;

DEFINE FIELD IF NOT EXISTS name
ON application TYPE string;

DEFINE FIELD IF NOT EXISTS organization_id
ON application TYPE record<organization>;

DEFINE FIELD IF NOT EXISTS language_id
ON application TYPE record<language_base>;

DEFINE FIELD IF NOT EXISTS state_id
ON application TYPE record<application_state>;

DEFINE FIELD OVERWRITE created_at
ON application TYPE datetime DEFAULT time::now() READONLY;

DEFINE FIELD IF NOT EXISTS updated_at
ON application TYPE datetime VALUE time::now();

DEFINE INDEX IF NOT EXISTS application_name_idx
ON application FIELDS name;

DEFINE INDEX IF NOT EXISTS application_organization_idx
ON application FIELDS organization_id;

DEFINE INDEX IF NOT EXISTS application_organization_name_idx
ON application FIELDS organization_id, name UNIQUE;

DEFINE INDEX IF NOT EXISTS application_language_state_idx
ON application FIELDS language_id, state_id;

-- =============================================================
-- APPLICATION EVENTS
-- =============================================================

DEFINE EVENT application_created
ON application
WHEN $event = "CREATE"
THEN (
    CREATE domain_events CONTENT {
        event_type: "APPLICATION_CREATED",
        aggregate_id: <string>$after.id,
        aggregate_type: "application",
        version: 1,
        payload: $after,
        metadata: {
            source: "surrealdb-event"
        },
        created_at: time::now()
    }
);

DEFINE EVENT application_updated
ON application
WHEN $event = "UPDATE"
THEN (
    CREATE domain_events CONTENT {
        event_type: "APPLICATION_UPDATED",
        aggregate_id: <string>$after.id,
        aggregate_type: "application",
        version: 1,
        payload: {
            before: $before,
            after: $after
        },
        metadata: {
            source: "surrealdb-event"
        },
        created_at: time::now()
    }
);

DEFINE EVENT application_deleted
ON application
WHEN $event = "DELETE"
THEN (
    CREATE domain_events CONTENT {
        event_type: "APPLICATION_DELETED",
        aggregate_id: <string>$before.id,
        aggregate_type: "application",
        version: 1,
        payload: $before,
        metadata: {
            source: "surrealdb-event"
        },
        created_at: time::now()
    }
);

-- =============================================================
-- ENVIRONMENT TYPE
-- =============================================================

DEFINE TABLE IF NOT EXISTS environment_type SCHEMAFULL;

DEFINE FIELD IF NOT EXISTS name
ON environment_type TYPE string;

DEFINE INDEX IF NOT EXISTS environment_type_name_idx
ON environment_type FIELDS name UNIQUE;

DELETE environment_type:develop;
DELETE environment_type:production;
DELETE environment_type:testing;
DELETE environment_type:`3ba48618-abb9-40c2-b700-e4413dd9332b`;
DELETE environment_type:`71ff34c1-1f4b-4858-943e-771907071c02`;
DELETE environment_type:`19b374bd-503a-4e65-b729-4c2442a69a4e`;

UPSERT environment_type:`3ba48618-abb9-40c2-b700-e4413dd9332b` CONTENT {
    name: "Develop"
};

UPSERT environment_type:`71ff34c1-1f4b-4858-943e-771907071c02` CONTENT {
    name: "Production"
};

UPSERT environment_type:`19b374bd-503a-4e65-b729-4c2442a69a4e` CONTENT {
    name: "Testing"
};

-- =============================================================
-- ENVIRONMENT STATE
-- =============================================================

DEFINE TABLE IF NOT EXISTS environment_state SCHEMAFULL;

DEFINE FIELD IF NOT EXISTS name
ON environment_state TYPE string;

DEFINE INDEX IF NOT EXISTS environment_state_name_idx
ON environment_state FIELDS name UNIQUE;

DELETE environment_state:active;
DELETE environment_state:inactive;
DELETE environment_state:`e6e00788-eda8-40fd-aa31-5763133e8834`;
DELETE environment_state:`ec30283f-2b63-485e-ab7a-5bd6f44c8878`;

UPSERT environment_state:`e6e00788-eda8-40fd-aa31-5763133e8834` CONTENT {
    name: "Active"
};

UPSERT environment_state:`ec30283f-2b63-485e-ab7a-5bd6f44c8878` CONTENT {
    name: "Inactive"
};

-- =============================================================
-- ENVIRONMENT
-- =============================================================

DEFINE TABLE IF NOT EXISTS environment SCHEMAFULL CHANGEFEED 7d;

DEFINE FIELD IF NOT EXISTS application_id
ON environment TYPE record<application>;

DEFINE FIELD IF NOT EXISTS type_id
ON environment TYPE record<environment_type>;

DEFINE FIELD IF NOT EXISTS state_id
ON environment TYPE record<environment_state>;

DEFINE FIELD OVERWRITE created_at
ON environment TYPE datetime DEFAULT time::now() READONLY;

DEFINE FIELD IF NOT EXISTS updated_at
ON environment TYPE datetime VALUE time::now();

DEFINE INDEX IF NOT EXISTS environment_application_idx
ON environment FIELDS application_id;

DEFINE INDEX IF NOT EXISTS environment_application_type_idx
ON environment FIELDS application_id, type_id UNIQUE;

DEFINE INDEX IF NOT EXISTS environment_type_state_idx
ON environment FIELDS type_id, state_id;

-- =============================================================
-- ENVIRONMENT EVENTS
-- =============================================================

DEFINE EVENT environment_created
ON environment
WHEN $event = "CREATE"
THEN (
    CREATE domain_events CONTENT {
        event_type: "ENVIRONMENT_CREATED",
        aggregate_id: <string>$after.id,
        aggregate_type: "environment",
        version: 1,
        payload: $after,
        metadata: {
            source: "surrealdb-event"
        },
        created_at: time::now()
    }
);

DEFINE EVENT environment_updated
ON environment
WHEN $event = "UPDATE"
THEN (
    CREATE domain_events CONTENT {
        event_type: "ENVIRONMENT_UPDATED",
        aggregate_id: <string>$after.id,
        aggregate_type: "environment",
        version: 1,
        payload: {
            before: $before,
            after: $after
        },
        metadata: {
            source: "surrealdb-event"
        },
        created_at: time::now()
    }
);

DEFINE EVENT environment_deleted
ON environment
WHEN $event = "DELETE"
THEN (
    CREATE domain_events CONTENT {
        event_type: "ENVIRONMENT_DELETED",
        aggregate_id: <string>$before.id,
        aggregate_type: "environment",
        version: 1,
        payload: $before,
        metadata: {
            source: "surrealdb-event"
        },
        created_at: time::now()
    }
);

-- =============================================================
-- FUNCTIONALITY STATE
-- =============================================================

DEFINE TABLE IF NOT EXISTS functionality_state SCHEMAFULL;

DEFINE FIELD IF NOT EXISTS name
ON functionality_state TYPE string;

DEFINE INDEX IF NOT EXISTS functionality_state_name_idx
ON functionality_state FIELDS name UNIQUE;

DELETE functionality_state:active;
DELETE functionality_state:inactive;
DELETE functionality_state:`407de974-c8aa-43e2-a5cf-22dc7c44054b`;
DELETE functionality_state:`505cde87-96f2-4926-8dc6-b04529469bd3`;

UPSERT functionality_state:`407de974-c8aa-43e2-a5cf-22dc7c44054b` CONTENT {
    name: "Active"
};

UPSERT functionality_state:`505cde87-96f2-4926-8dc6-b04529469bd3` CONTENT {
    name: "Inactive"
};

-- =============================================================
-- FUNCTIONALITY
-- =============================================================

DEFINE TABLE IF NOT EXISTS functionality SCHEMAFULL CHANGEFEED 7d;

DEFINE FIELD IF NOT EXISTS name
ON functionality TYPE string;

DEFINE FIELD IF NOT EXISTS application_id
ON functionality TYPE record<application>;

DEFINE FIELD IF NOT EXISTS state_id
ON functionality TYPE record<functionality_state>;

DEFINE FIELD OVERWRITE created_at
ON functionality TYPE datetime DEFAULT time::now() READONLY;

DEFINE FIELD IF NOT EXISTS updated_at
ON functionality TYPE datetime VALUE time::now();

DEFINE INDEX IF NOT EXISTS functionality_application_idx
ON functionality FIELDS application_id;

DEFINE INDEX IF NOT EXISTS functionality_state_idx
ON functionality FIELDS state_id;

DEFINE EVENT functionality_created
ON functionality
WHEN $event = "CREATE"
THEN (
    CREATE domain_events CONTENT {
        event_type: "FUNCTIONALITY_CREATED",
        aggregate_id: <string>$after.id,
        aggregate_type: "functionality",
        version: 1,
        payload: $after,
        metadata: {
            source: "surrealdb-event"
        },
        created_at: time::now()
    }
);

DEFINE EVENT functionality_updated
ON functionality
WHEN $event = "UPDATE"
THEN (
    CREATE domain_events CONTENT {
        event_type: "FUNCTIONALITY_UPDATED",
        aggregate_id: <string>$after.id,
        aggregate_type: "functionality",
        version: 1,
        payload: {
            before: $before,
            after: $after
        },
        metadata: {
            source: "surrealdb-event"
        },
        created_at: time::now()
    }
);

DEFINE EVENT functionality_deleted
ON functionality
WHEN $event = "DELETE"
THEN (
    CREATE domain_events CONTENT {
        event_type: "FUNCTIONALITY_DELETED",
        aggregate_id: <string>$before.id,
        aggregate_type: "functionality",
        version: 1,
        payload: $before,
        metadata: {
            source: "surrealdb-event"
        },
        created_at: time::now()
    }
);

-- =============================================================
-- MESSAGE STATE
-- =============================================================

DEFINE TABLE IF NOT EXISTS message_state SCHEMAFULL;

DEFINE FIELD IF NOT EXISTS name
ON message_state TYPE string;

DEFINE INDEX IF NOT EXISTS message_state_name_idx
ON message_state FIELDS name UNIQUE;

DELETE message_state:active;
DELETE message_state:inactive;
DELETE message_state:Active;
DELETE message_state:Inactive;
DELETE message_state:`4223b3dc-c991-4603-a746-d37ce5ecd976`;
DELETE message_state:`5861f44a-1303-41dc-a079-a7bd7c5cfc47`;

UPSERT message_state:`4223b3dc-c991-4603-a746-d37ce5ecd976` CONTENT {
    name: "Active"
};

UPSERT message_state:`5861f44a-1303-41dc-a079-a7bd7c5cfc47` CONTENT {
    name: "Inactive"
};

-- =============================================================
-- MESSAGE TYPE
-- =============================================================

DEFINE TABLE IF NOT EXISTS message_type SCHEMAFULL;

DEFINE FIELD IF NOT EXISTS name
ON message_type TYPE string;

DEFINE INDEX IF NOT EXISTS message_type_name_idx
ON message_type FIELDS name UNIQUE;

DELETE message_type:functional;
DELETE message_type:technical;
DELETE message_type:Functional;
DELETE message_type:Technical;
DELETE message_type:`0c71601d-96b3-417e-b385-06b10ba126d9`;
DELETE message_type:`d6cca1fe-1801-48c7-98ce-0e63255d187e`;

UPSERT message_type:`0c71601d-96b3-417e-b385-06b10ba126d9` CONTENT {
    name: "Functional"
};

UPSERT message_type:`d6cca1fe-1801-48c7-98ce-0e63255d187e` CONTENT {
    name: "Technical"
};

-- =============================================================
-- MESSAGE CATEGORY
-- =============================================================

DEFINE TABLE IF NOT EXISTS message_category SCHEMAFULL;

DEFINE FIELD IF NOT EXISTS name
ON message_category TYPE string;

DEFINE INDEX IF NOT EXISTS message_category_name_idx
ON message_category FIELDS name UNIQUE;

DELETE message_category:error;
DELETE message_category:information;
DELETE message_category:confirmation;
DELETE message_category:warning;
DELETE message_category:debug;
DELETE message_category:Error;
DELETE message_category:Information;
DELETE message_category:Confirmation;
DELETE message_category:Warning;
DELETE message_category:Debug;
DELETE message_category:`c1e02ac3-02c3-4818-8f94-f35d852db9b0`;
DELETE message_category:`3b337f14-dde1-436e-829a-eebacb556eb4`;
DELETE message_category:`4276d9a3-c403-4c35-9167-97da025a2fd5`;
DELETE message_category:`9eaca010-c577-4af0-8b05-4a0a82450311`;
DELETE message_category:`bf416ec8-64a9-43a4-b3db-0c8a79ba525f`;

UPSERT message_category:`c1e02ac3-02c3-4818-8f94-f35d852db9b0` CONTENT {
    name: "Error"
};

UPSERT message_category:`3b337f14-dde1-436e-829a-eebacb556eb4` CONTENT {
    name: "Information"
};

UPSERT message_category:`4276d9a3-c403-4c35-9167-97da025a2fd5` CONTENT {
    name: "Confirmation"
};

UPSERT message_category:`9eaca010-c577-4af0-8b05-4a0a82450311` CONTENT {
    name: "Warning"
};

UPSERT message_category:`bf416ec8-64a9-43a4-b3db-0c8a79ba525f` CONTENT {
    name: "Debug"
};

-- =============================================================
-- MESSAGE
-- =============================================================

DEFINE TABLE IF NOT EXISTS message SCHEMAFULL CHANGEFEED 7d;

DEFINE FIELD IF NOT EXISTS code
ON message TYPE string;

DEFINE FIELD IF NOT EXISTS title
ON message TYPE string;

DEFINE FIELD IF NOT EXISTS content
ON message TYPE string;

DEFINE FIELD IF NOT EXISTS type_id
ON message TYPE record<message_type>;

DEFINE FIELD IF NOT EXISTS category_id
ON message TYPE record<message_category>;

DEFINE FIELD IF NOT EXISTS status_id
ON message TYPE record<message_state>;

DEFINE FIELD IF NOT EXISTS application_id
ON message TYPE option<record<application>>;

DEFINE FIELD IF NOT EXISTS functionality_id
ON message TYPE option<record<functionality>>;

DEFINE FIELD OVERWRITE created_at
ON message TYPE datetime DEFAULT time::now() READONLY;

DEFINE FIELD IF NOT EXISTS updated_at
ON message TYPE datetime VALUE time::now();

DEFINE INDEX IF NOT EXISTS message_code_idx
ON message FIELDS code;

DEFINE INDEX IF NOT EXISTS message_application_code_idx
ON message FIELDS application_id, code UNIQUE;

DEFINE INDEX IF NOT EXISTS message_catalogs_idx
ON message FIELDS type_id, category_id, status_id;

-- =============================================================
-- MESSAGE EVENTS
-- =============================================================

DEFINE EVENT message_created
ON message
WHEN $event = "CREATE"
THEN (
    CREATE domain_events CONTENT {
        event_type: "MESSAGE_CREATED",
        aggregate_id: <string>$after.id,
        aggregate_type: "message",
        version: 1,
        payload: $after,
        metadata: {
            source: "surrealdb-event"
        },
        created_at: time::now()
    }
);

DEFINE EVENT message_updated
ON message
WHEN $event = "UPDATE"
THEN (
    CREATE domain_events CONTENT {
        event_type: "MESSAGE_UPDATED",
        aggregate_id: <string>$after.id,
        aggregate_type: "message",
        version: 1,
        payload: {
            before: $before,
            after: $after
        },
        metadata: {
            source: "surrealdb-event"
        },
        created_at: time::now()
    }
);

DEFINE EVENT message_deleted
ON message
WHEN $event = "DELETE"
THEN (
    CREATE domain_events CONTENT {
        event_type: "MESSAGE_DELETED",
        aggregate_id: <string>$before.id,
        aggregate_type: "message",
        version: 1,
        payload: $before,
        metadata: {
            source: "surrealdb-event"
        },
        created_at: time::now()
    }
);

-- =============================================================
-- MESSAGE ENVIRONMENT STATE
-- =============================================================

DEFINE TABLE IF NOT EXISTS message_environment_state SCHEMAFULL;

DEFINE FIELD IF NOT EXISTS name
ON message_environment_state TYPE string;

DEFINE INDEX IF NOT EXISTS message_environment_state_name_idx
ON message_environment_state FIELDS name UNIQUE;

DELETE message_environment_state:active;
DELETE message_environment_state:inactive;
DELETE message_environment_state:Active;
DELETE message_environment_state:Inactive;
DELETE message_environment_state:`10ddbab2-352e-48de-b9eb-de0895ddb944`;
DELETE message_environment_state:`e68930ad-772a-4d38-beae-b20434372957`;

UPSERT message_environment_state:`10ddbab2-352e-48de-b9eb-de0895ddb944` CONTENT {
    name: "Active"
};

UPSERT message_environment_state:`e68930ad-772a-4d38-beae-b20434372957` CONTENT {
    name: "Inactive"
};

-- =============================================================
-- MESSAGE ENVIRONMENT
-- =============================================================

DEFINE TABLE IF NOT EXISTS message_environment SCHEMAFULL CHANGEFEED 7d;

DEFINE FIELD IF NOT EXISTS message_id
ON message_environment TYPE record<message>;

DEFINE FIELD IF NOT EXISTS environment_id
ON message_environment TYPE record<environment>;

DEFINE FIELD IF NOT EXISTS state_data_id
ON message_environment TYPE record<message_environment_state>;

DEFINE FIELD OVERWRITE created_at
ON message_environment TYPE datetime DEFAULT time::now() READONLY;

DEFINE FIELD IF NOT EXISTS updated_at
ON message_environment TYPE datetime VALUE time::now();

DEFINE INDEX IF NOT EXISTS message_environment_environment_idx
ON message_environment FIELDS environment_id;

DEFINE INDEX IF NOT EXISTS message_environment_message_idx
ON message_environment FIELDS message_id;

DEFINE EVENT message_environment_created
ON message_environment
WHEN $event = "CREATE"
THEN (
    CREATE domain_events CONTENT {
        event_type: "MESSAGE_ENVIRONMENT_CREATED",
        aggregate_id: <string>$after.id,
        aggregate_type: "message_environment",
        version: 1,
        payload: $after,
        metadata: {
            source: "surrealdb-event"
        },
        created_at: time::now()
    }
);

DEFINE EVENT message_environment_updated
ON message_environment
WHEN $event = "UPDATE"
THEN (
    CREATE domain_events CONTENT {
        event_type: "MESSAGE_ENVIRONMENT_UPDATED",
        aggregate_id: <string>$after.id,
        aggregate_type: "message_environment",
        version: 1,
        payload: {
            before: $before,
            after: $after
        },
        metadata: {
            source: "surrealdb-event"
        },
        created_at: time::now()
    }
);

DEFINE EVENT message_environment_deleted
ON message_environment
WHEN $event = "DELETE"
THEN (
    CREATE domain_events CONTENT {
        event_type: "MESSAGE_ENVIRONMENT_DELETED",
        aggregate_id: <string>$before.id,
        aggregate_type: "message_environment",
        version: 1,
        payload: $before,
        metadata: {
            source: "surrealdb-event"
        },
        created_at: time::now()
    }
);

-- =============================================================
-- DOCUMENTAL PROJECTIONS
-- =============================================================

DEFINE TABLE IF NOT EXISTS domain_event_document SCHEMALESS;

DEFINE FIELD IF NOT EXISTS source_id
ON domain_event_document TYPE option<string>;

DEFINE FIELD IF NOT EXISTS aggregate_id
ON domain_event_document TYPE option<string>;

DEFINE FIELD IF NOT EXISTS aggregate_type
ON domain_event_document TYPE option<string>;

DEFINE FIELD IF NOT EXISTS event_type
ON domain_event_document TYPE option<string>;

DEFINE FIELD IF NOT EXISTS generated_at
ON domain_event_document TYPE option<datetime>;

DEFINE INDEX IF NOT EXISTS domain_event_document_source_idx
ON domain_event_document FIELDS source_id UNIQUE;

DEFINE INDEX IF NOT EXISTS domain_event_document_aggregate_idx
ON domain_event_document FIELDS aggregate_id, aggregate_type;

DEFINE TABLE IF NOT EXISTS application_document SCHEMALESS;

DEFINE FIELD IF NOT EXISTS source_id
ON application_document TYPE option<string>;

DEFINE FIELD IF NOT EXISTS application_id
ON application_document TYPE option<string>;

DEFINE FIELD IF NOT EXISTS organization_id
ON application_document TYPE option<string>;

DEFINE FIELD IF NOT EXISTS projection_version
ON application_document TYPE option<number>;

DEFINE FIELD IF NOT EXISTS generated_at
ON application_document TYPE option<datetime>;

DEFINE INDEX IF NOT EXISTS application_document_id_idx
ON application_document FIELDS application_id;

DEFINE INDEX IF NOT EXISTS application_document_organization_idx
ON application_document FIELDS organization_id;

DEFINE INDEX IF NOT EXISTS application_document_state_name_idx
ON application_document FIELDS state.name;

-- =============================================================
-- ENVIRONMENT DOCUMENT COLLECTION
-- =============================================================

DEFINE TABLE IF NOT EXISTS environment_document SCHEMALESS;

DEFINE FIELD IF NOT EXISTS source_id
ON environment_document TYPE option<string>;

DEFINE FIELD IF NOT EXISTS environment_id
ON environment_document TYPE option<string>;

DEFINE FIELD IF NOT EXISTS application_id
ON environment_document TYPE option<string>;

DEFINE FIELD IF NOT EXISTS projection_version
ON environment_document TYPE option<number>;

DEFINE FIELD IF NOT EXISTS generated_at
ON environment_document TYPE option<datetime>;

DEFINE INDEX IF NOT EXISTS environment_document_id_idx
ON environment_document FIELDS environment_id;

DEFINE INDEX IF NOT EXISTS environment_document_application_idx
ON environment_document FIELDS application_id;

DEFINE INDEX IF NOT EXISTS environment_document_state_name_idx
ON environment_document FIELDS state.name;

-- =============================================================
-- MESSAGE DOCUMENT COLLECTION
-- =============================================================

DEFINE TABLE IF NOT EXISTS message_data_collection SCHEMALESS;

DEFINE FIELD IF NOT EXISTS source_id
ON message_data_collection TYPE option<string>;

DEFINE FIELD IF NOT EXISTS message_id
ON message_data_collection TYPE option<string>;

DEFINE FIELD IF NOT EXISTS environment_id
ON message_data_collection TYPE option<string>;

DEFINE FIELD IF NOT EXISTS projection_version
ON message_data_collection TYPE option<number>;

DEFINE FIELD IF NOT EXISTS generated_at
ON message_data_collection TYPE option<datetime>;

DEFINE INDEX IF NOT EXISTS message_data_collection_message_id_idx
ON message_data_collection FIELDS message_id;

DEFINE INDEX IF NOT EXISTS message_data_collection_status_name_idx
ON message_data_collection FIELDS status.name;

-- =============================================================
-- MESSAGE ENVIRONMENT READ MODEL
-- =============================================================

DEFINE TABLE IF NOT EXISTS message_environment_readmodel SCHEMALESS;

DEFINE FIELD IF NOT EXISTS source_id
ON message_environment_readmodel TYPE option<string>;

DEFINE FIELD IF NOT EXISTS message_environment_id
ON message_environment_readmodel TYPE option<string>;

DEFINE FIELD IF NOT EXISTS message_id
ON message_environment_readmodel TYPE option<string>;

DEFINE FIELD IF NOT EXISTS environment_id
ON message_environment_readmodel TYPE option<string>;

DEFINE FIELD IF NOT EXISTS projection_version
ON message_environment_readmodel TYPE option<number>;

DEFINE FIELD IF NOT EXISTS generated_at
ON message_environment_readmodel TYPE option<datetime>;

DEFINE INDEX IF NOT EXISTS message_environment_readmodel_environment_idx
ON message_environment_readmodel FIELDS environment_id;

DEFINE INDEX IF NOT EXISTS message_environment_readmodel_message_code_idx
ON message_environment_readmodel FIELDS message.code;

-- =============================================================
-- LOCAL DEVELOPMENT SEED
-- Matches the simulated identity configured through Doppler.
-- Fixed UUID v4 record IDs make repeated imports idempotent.
-- =============================================================

-- Migrate references created with the former symbolic catalog record IDs.
UPDATE application SET language_id = language_base:`6319ca87-47b3-4e0e-994f-3166378988f2`
WHERE language_id = language_base:en;
UPDATE application SET language_id = language_base:`bed4d574-4c7f-4d95-bc12-6dbbcdbbf4fc`
WHERE language_id = language_base:es;
UPDATE application SET language_id = language_base:`8cc7ae54-db8c-4ef1-929b-22e17de58129`
WHERE language_id = language_base:fr;
UPDATE application SET state_id = application_state:`dfb7a7cb-66f6-4b73-a1d8-803336f72ebe`
WHERE state_id = application_state:active;
UPDATE application SET state_id = application_state:`2a0ddacd-2dcc-47e2-b56f-03db5207c443`
WHERE state_id = application_state:inactive;

UPDATE environment SET type_id = environment_type:`3ba48618-abb9-40c2-b700-e4413dd9332b`
WHERE type_id = environment_type:develop;
UPDATE environment SET type_id = environment_type:`71ff34c1-1f4b-4858-943e-771907071c02`
WHERE type_id = environment_type:production;
UPDATE environment SET type_id = environment_type:`19b374bd-503a-4e65-b729-4c2442a69a4e`
WHERE type_id = environment_type:testing;
UPDATE environment SET state_id = environment_state:`e6e00788-eda8-40fd-aa31-5763133e8834`
WHERE state_id = environment_state:active;
UPDATE environment SET state_id = environment_state:`ec30283f-2b63-485e-ab7a-5bd6f44c8878`
WHERE state_id = environment_state:inactive;

UPDATE functionality SET state_id = functionality_state:`407de974-c8aa-43e2-a5cf-22dc7c44054b`
WHERE state_id = functionality_state:active;
UPDATE functionality SET state_id = functionality_state:`505cde87-96f2-4926-8dc6-b04529469bd3`
WHERE state_id = functionality_state:inactive;

UPDATE message SET type_id = message_type:`0c71601d-96b3-417e-b385-06b10ba126d9`
WHERE type_id IN [message_type:functional, message_type:Functional];
UPDATE message SET type_id = message_type:`d6cca1fe-1801-48c7-98ce-0e63255d187e`
WHERE type_id IN [message_type:technical, message_type:Technical];
UPDATE message SET category_id = message_category:`c1e02ac3-02c3-4818-8f94-f35d852db9b0`
WHERE category_id IN [message_category:error, message_category:Error];
UPDATE message SET category_id = message_category:`3b337f14-dde1-436e-829a-eebacb556eb4`
WHERE category_id IN [message_category:information, message_category:Information];
UPDATE message SET category_id = message_category:`4276d9a3-c403-4c35-9167-97da025a2fd5`
WHERE category_id IN [message_category:confirmation, message_category:Confirmation];
UPDATE message SET category_id = message_category:`9eaca010-c577-4af0-8b05-4a0a82450311`
WHERE category_id IN [message_category:warning, message_category:Warning];
UPDATE message SET category_id = message_category:`bf416ec8-64a9-43a4-b3db-0c8a79ba525f`
WHERE category_id IN [message_category:debug, message_category:Debug];
UPDATE message SET status_id = message_state:`4223b3dc-c991-4603-a746-d37ce5ecd976`
WHERE status_id IN [message_state:active, message_state:Active];
UPDATE message SET status_id = message_state:`5861f44a-1303-41dc-a079-a7bd7c5cfc47`
WHERE status_id IN [message_state:inactive, message_state:Inactive];

UPDATE message_environment
SET state_data_id = message_environment_state:`10ddbab2-352e-48de-b9eb-de0895ddb944`
WHERE state_data_id IN [message_environment_state:active, message_environment_state:Active];
UPDATE message_environment
SET state_data_id = message_environment_state:`e68930ad-772a-4d38-beae-b20434372957`
WHERE state_data_id IN [message_environment_state:inactive, message_environment_state:Inactive];

-- Recreate only this seed's deterministic records. SurrealDB 3.1.4 can
-- report a self-conflict when UPSERT CONTENT meets an existing unique value.
LET $init_now = time::now();

DELETE message_environment_readmodel:`2e7a4c91-6b35-48fd-a2e8-1c9d5f703b64`;
DELETE message_data_collection:`b6d3f821-7a4e-49c5-8d12-3f0b9e6a274c`;
DELETE message_environment:`2e7a4c91-6b35-48fd-a2e8-1c9d5f703b64`;
DELETE message:`b6d3f821-7a4e-49c5-8d12-3f0b9e6a274c`;
DELETE functionality:`5a8e2c74-1d39-4f6b-b7c2-0e9a3d5f8164`;
DELETE environment:`de1b8223-1e9c-4807-beec-b2c1cff44e73`;
DELETE environment:`ebe114e4-01d4-427b-9773-b63325ddd55c`;
DELETE environment:`7b3e5d91-a2c8-46f0-9d14-5e7a1b6c3f82`;
DELETE application:`c4a91e72-8d36-4f5b-b2a7-6e1c9d804f23`;
DELETE organization:`8f2d3a10-6b47-4c91-a5e8-2d7f9b3c1a40`;

-- Remove the obsolete predictable development records from earlier seeds.
DELETE environment:`33333333-3333-3333-3333-333333333333`;
DELETE application:`22222222-2222-2222-2222-222222222222`;
DELETE organization:`11111111-1111-1111-1111-111111111111`;

UPSERT organization:`8f2d3a10-6b47-4c91-a5e8-2d7f9b3c1a40` CONTENT {
    name: "UCO",
    created_at: $init_now,
    updated_at: $init_now
};

UPSERT application:`c4a91e72-8d36-4f5b-b2a7-6e1c9d804f23` CONTENT {
    name: "Arquisoft",
    organization_id: organization:`8f2d3a10-6b47-4c91-a5e8-2d7f9b3c1a40`,
    language_id: language_base:`6319ca87-47b3-4e0e-994f-3166378988f2`,
    state_id: application_state:`dfb7a7cb-66f6-4b73-a1d8-803336f72ebe`,
    created_at: $init_now,
    updated_at: $init_now
};

UPSERT environment:`7b3e5d91-a2c8-46f0-9d14-5e7a1b6c3f82` CONTENT {
    application_id: application:`c4a91e72-8d36-4f5b-b2a7-6e1c9d804f23`,
    type_id: environment_type:`3ba48618-abb9-40c2-b700-e4413dd9332b`,
    state_id: environment_state:`e6e00788-eda8-40fd-aa31-5763133e8834`,
    created_at: $init_now,
    updated_at: $init_now
};

UPSERT environment:`ebe114e4-01d4-427b-9773-b63325ddd55c` CONTENT {
    application_id: application:`c4a91e72-8d36-4f5b-b2a7-6e1c9d804f23`,
    type_id: environment_type:`19b374bd-503a-4e65-b729-4c2442a69a4e`,
    state_id: environment_state:`e6e00788-eda8-40fd-aa31-5763133e8834`,
    created_at: $init_now,
    updated_at: $init_now
};

UPSERT environment:`de1b8223-1e9c-4807-beec-b2c1cff44e73` CONTENT {
    application_id: application:`c4a91e72-8d36-4f5b-b2a7-6e1c9d804f23`,
    type_id: environment_type:`71ff34c1-1f4b-4858-943e-771907071c02`,
    state_id: environment_state:`e6e00788-eda8-40fd-aa31-5763133e8834`,
    created_at: $init_now,
    updated_at: $init_now
};

UPSERT functionality:`5a8e2c74-1d39-4f6b-b7c2-0e9a3d5f8164` CONTENT {
    name: "General Messages",
    application_id: application:`c4a91e72-8d36-4f5b-b2a7-6e1c9d804f23`,
    state_id: functionality_state:`407de974-c8aa-43e2-a5cf-22dc7c44054b`,
    created_at: $init_now,
    updated_at: $init_now
};

UPSERT message:`b6d3f821-7a4e-49c5-8d12-3f0b9e6a274c` CONTENT {
    code: "MSG_WELCOME",
    title: "Welcome",
    content: "Welcome to MessageUcoLab",
    type_id: message_type:`0c71601d-96b3-417e-b385-06b10ba126d9`,
    category_id: message_category:`3b337f14-dde1-436e-829a-eebacb556eb4`,
    status_id: message_state:`4223b3dc-c991-4603-a746-d37ce5ecd976`,
    application_id: application:`c4a91e72-8d36-4f5b-b2a7-6e1c9d804f23`,
    functionality_id: functionality:`5a8e2c74-1d39-4f6b-b7c2-0e9a3d5f8164`,
    created_at: $init_now,
    updated_at: $init_now
};

UPSERT message_environment:`2e7a4c91-6b35-48fd-a2e8-1c9d5f703b64` CONTENT {
    message_id: message:`b6d3f821-7a4e-49c5-8d12-3f0b9e6a274c`,
    environment_id: environment:`7b3e5d91-a2c8-46f0-9d14-5e7a1b6c3f82`,
    state_data_id: message_environment_state:`10ddbab2-352e-48de-b9eb-de0895ddb944`,
    created_at: $init_now,
    updated_at: $init_now
};

UPSERT message_data_collection:`b6d3f821-7a4e-49c5-8d12-3f0b9e6a274c` CONTENT {
    source_id: "message:b6d3f821-7a4e-49c5-8d12-3f0b9e6a274c",
    message_id: "message:b6d3f821-7a4e-49c5-8d12-3f0b9e6a274c",
    code: "MSG_WELCOME",
    title: "Welcome",
    content: "Welcome to MessageUcoLab",
    type: { id: "message_type:0c71601d-96b3-417e-b385-06b10ba126d9", name: "Functional" },
    category: { id: "message_category:3b337f14-dde1-436e-829a-eebacb556eb4", name: "Information" },
    status: { id: "message_state:4223b3dc-c991-4603-a746-d37ce5ecd976", name: "Active" },
    functionality: { id: "functionality:5a8e2c74-1d39-4f6b-b7c2-0e9a3d5f8164", name: "General Messages" },
    created_at: $init_now,
    projection_version: 1,
    generated_at: $init_now
};

UPSERT message_environment_readmodel:`2e7a4c91-6b35-48fd-a2e8-1c9d5f703b64` CONTENT {
    source_id: "message_environment:2e7a4c91-6b35-48fd-a2e8-1c9d5f703b64",
    message_environment_id: "message_environment:2e7a4c91-6b35-48fd-a2e8-1c9d5f703b64",
    message_id: "message:b6d3f821-7a4e-49c5-8d12-3f0b9e6a274c",
    environment_id: "environment:7b3e5d91-a2c8-46f0-9d14-5e7a1b6c3f82",
    environment: { id: "environment:7b3e5d91-a2c8-46f0-9d14-5e7a1b6c3f82" },
    message: {
        id: "message:b6d3f821-7a4e-49c5-8d12-3f0b9e6a274c",
        code: "MSG_WELCOME",
        title: "Welcome",
        content: "Welcome to MessageUcoLab",
        type: { id: "message_type:0c71601d-96b3-417e-b385-06b10ba126d9", name: "Functional" },
        category: { id: "message_category:3b337f14-dde1-436e-829a-eebacb556eb4", name: "Information" },
        status: { id: "message_state:4223b3dc-c991-4603-a746-d37ce5ecd976", name: "Active" },
        functionality: { id: "functionality:5a8e2c74-1d39-4f6b-b7c2-0e9a3d5f8164", name: "General Messages" },
        created_at: $init_now
    },
    status: { id: "message_environment_state:10ddbab2-352e-48de-b9eb-de0895ddb944", name: "Active" },
    projection_version: 1,
    generated_at: $init_now
};
