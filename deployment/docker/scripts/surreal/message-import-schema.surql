-- Additive schema only: no seed replacement, deletes or changes to existing messages.
DEFINE TABLE IF NOT EXISTS message_import_receipt SCHEMAFULL;
DEFINE FIELD IF NOT EXISTS application_id ON message_import_receipt TYPE record<application>;
DEFINE FIELD IF NOT EXISTS receipt_json ON message_import_receipt TYPE string;
DEFINE FIELD IF NOT EXISTS created_at ON message_import_receipt TYPE datetime DEFAULT time::now() READONLY;
