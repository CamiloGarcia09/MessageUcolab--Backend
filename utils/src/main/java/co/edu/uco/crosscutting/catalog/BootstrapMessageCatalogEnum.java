package co.edu.uco.crosscutting.catalog;

import java.util.Arrays;
import java.util.Optional;

import static co.edu.uco.crosscutting.helpers.UtilText.trim;

public enum BootstrapMessageCatalogEnum {

    TCH_001("TCH_001", "Database not connected", "Unable to establish connection to %s database", "TECHNICAL", "ERROR"),
    TCH_002("TCH_002", "Message Broker not connected", "Unable to establish connection to Message Broker", "TECHNICAL", "ERROR"),
    TCH_003("TCH_003", "API Translate not connected", "Unable to establish connection to translate API", "TECHNICAL", "ERROR"),
    TCH_004("TCH_004", "Cache not connected", "Unable to establish connection to cache", "TECHNICAL", "ERROR"),
    TCH_005("TCH_005", "Parameters component not connected", "Unable to establish connection to Parameters component", "TECHNICAL", "ERROR"),
    TCH_006("TCH_006", "Security component not connected", "Unable to establish connection to Security component", "TECHNICAL", "ERROR"),
    TCH_007("TCH_007", "Key is null", "The message key is null", "TECHNICAL", "ERROR"),
    TCH_008("TCH_008", "Key is empty", "Message code does not exist", "TECHNICAL", "ERROR"),
    TCH_009("TCH_009", "Message not found", "Message code does not exist with %s key", "TECHNICAL", "ERROR"),

    TCH_016("TCH_016", "Validation Error", "Validation error with correlation id {}", "TECHNICAL", "ERROR"),
    TCH_017("TCH_017", "Serializer not found", "No serializer was found for media type '{}' and there is no default serializer configured.", "TECHNICAL", "ERROR"),
    TCH_018("TCH_018", "Serializer error", "Error when serializing the object", "TECHNICAL", "ERROR"),
    TCH_019("TCH_019", "Error when serializing error response", "An error occurred while trying to serialize the error response.", "TECHNICAL", "ERROR"),
    TCH_020("TCH_020", "Response Error", "The error response is: {}", "TECHNICAL", "ERROR"),
    TCH_021("TCH_021", "Response success", "The successful response is: {}", "TECHNICAL", "INFORMATION"),
    TCH_022("TCH_022", "Media type not supported", "The media type %s is not supported by the system", "TECHNICAL", "ERROR"),
    TCH_023("TCH_023", "Incorrect Media type", "Media type is not supported {}", "TECHNICAL", "ERROR"),
    TCH_041("TCH_041", "Key Vault URL missing", "Azure Key Vault URL is missing or empty.", "TECHNICAL", "ERROR"),
    TCH_042("TCH_042", "SecretClient init failed", "Failed to initialize Azure SecretClient with URL: %s", "TECHNICAL", "ERROR"),

    TCH_051("TCH_051", "Redis message model retrieval failed", "Failed to retrieve message model from Redis for key: %s", "TECHNICAL", "ERROR"),
    TCH_052("TCH_052", "Redis message content retrieval failed", "Failed to retrieve message content from Redis for key: %s", "TECHNICAL", "ERROR"),
    TCH_053("TCH_053", "Redis message title retrieval failed", "Failed to retrieve message title from Redis for key: %s", "TECHNICAL", "ERROR"),
    TCH_054("TCH_054", "Redis message save failed", "Failed to save message to Redis for key: %s", "TECHNICAL", "ERROR"),
    TCH_055("TCH_055", "Catalog static reference not initialized", "CatalogPortStaticRef is not initialized", "TECHNICAL", "ERROR"),
    TCH_056("TCH_056", "Surreal environment query error", "Error querying SurrealDB for environment. Query: %s", "TECHNICAL", "ERROR"),
    TCH_057("TCH_057", "Surreal messages by environment query error", "Error finding messages by environment in SurrealDB. Environment ID: %s", "TECHNICAL", "ERROR"),
    TCH_058("TCH_058", "Surreal query error", "Error querying SurrealDB. Query: %s", "TECHNICAL", "ERROR"),
    TCH_059("TCH_059", "Surreal multiple messages query error", "Error querying SurrealDB for multiple messages. Query: %s", "TECHNICAL", "ERROR"),
    TCH_060("TCH_060", "Surreal count error", "Error counting records in SurrealDB. Query: %s", "TECHNICAL", "ERROR"),
    TCH_066("TCH_066", "Surreal message persist error", "Error al persistir mensaje en SurrealDB", "TECHNICAL", "ERROR"),
    TCH_067("TCH_067", "Surreal message persist technical error", "Error al persistir el mensaje en la base de datos SurrealDB", "TECHNICAL", "ERROR"),

    FUN_013("FUN_013", "Cache not connected", "Failed to connect to Redis", "FUNCTIONAL", "ERROR"),
    FUN_014("FUN_014", "Error during cache connection", "Data access exception while connecting to Redis", "FUNCTIONAL", "ERROR"),
    FUN_015("FUN_015", "Unexpected exception", "Unexpected exception while connecting to Redis", "FUNCTIONAL", "ERROR"),
    FUN_023("FUN_023", "Unexpected Error", "An unexpected error has occurred", "FUNCTIONAL", "ERROR");

    private final String code;
    private final String title;
    private final String content;
    private final String type;
    private final String category;

    BootstrapMessageCatalogEnum(String code, String title, String content, String type, String category) {
        this.code = code;
        this.title = title;
        this.content = content;
        this.type = type;
        this.category = category;
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getType() {
        return type;
    }

    public String getCategory() {
        return category;
    }

    public static Optional<BootstrapMessageCatalogEnum> findByCode(String code) {
        var normalized = trim(code);
        return Arrays.stream(values())
                .filter(value -> value.code.equals(normalized))
                .findFirst();
    }

    public static boolean exists(String code) {
        return findByCode(code).isPresent();
    }
}
