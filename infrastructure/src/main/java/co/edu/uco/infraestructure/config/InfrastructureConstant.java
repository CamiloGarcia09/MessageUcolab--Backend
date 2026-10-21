package co.edu.uco.infraestructure.config;

public final class InfrastructureConstant {

    private InfrastructureConstant() {}

    public static final String FIELD_ID = "id";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_CREATION_DATE = "creation_date";
    public static final String FIELD_EXPIRATION_DATE = "expiration_date";
    public static final String FIELD_ENVIRONMENT_ID = "environment_id";
    public static final String FIELD_MESSAGE_ENVIRONMENT_ID = "message_environment_id";
    public static final String FIELD_MESSAGE = "message";
    public static final String FIELD_STATE_ID = "state_id";
    public static final String FIELD_TYPE_ID = "type_id";
    public static final String FIELD_APPLICATION_ID = "application_id";
    public static final String ENVIRONMENT_ID_ATTRIBUTE = "environmentId";
    public static final String APPLICATION_ID_ATTRIBUTE = "applicationId";
    public static final String EXTERNAL_IDENTITY_ATTRIBUTE = "externalIdentity";

    public static final String PACKAGE_BASE = "co.edu.uco";
    public static final String PACKAGE_REPOSITORY_ADAPTER = "co.edu.uco.infraestructure.secondaryadapters.repository";

    public static final int CACHE_EXPIRATION_TIME = 15;
    public static final int CACHE_MAXIMUM_SIZE = 500;

    public static final long MICROSECONDS_PER_MILLISECOND = 1_000;

    public static final String PULSAR_CONFIG_PREFIX = "pulsar";

    public static final String HTML_OPEN_TAG = "<html>";
    public static final String HTML_CLOSE_TAG = "</html>";
    public static final String BODY_OPEN_TAG = "<body>";
    public static final String BODY_CLOSE_TAG = "</body>";
    public static final String PRE_OPEN_TAG = "<pre>";
    public static final String PRE_CLOSE_TAG = "</pre>";

    public static final String REDIS_HASH = "Message";
    public static final String CACHE_REDIS_ADAPTER = "messageRedisAdapter";
    public static final String TRANSLATION_CACHE_REDIS_ADAPTER = "messageTranslationRedisAdapter";
    public static final String TRANSLATION_CACHE_KEY_SEPARATOR = "::";
    public static final long TRANSLATION_CACHE_TTL_SECONDS = 900;
    public static final String CORRELATION_ID = "X-Correlation-ID";
    public static final String LOGGING_REQUEST_URI = "REQUEST_URI";
    public static final String LOGGING_HTTP_METHOD = "HTTP_METHOD";
    public static final String LOGGING_SESSION_ID = "JSESSIONID";
    public static final String LOGGING_QUERY_STRING = "QUERY_STRING";
    public static final String LOGGING_HTTP_STATUS = "HTTP_STATUS";
    public static final String LOGGING_DURATION_MS = "DURATION_MS";
    public static final String LOGGING_START_TIME = "LOGGING_START_TIME";
    public static final String LOGGING_PARAMETER_APPLICATION_NAME = "MessageUcoLab";
    public static final String LOGGING_PARAMETER_CODE_MESSAGE = "codeMessage";
    public static final String LOGGING_PARAMETER_APPLICATION = "application";
    public static final String LOGGING_TIMESTAMP = "TS";
    public static final String LOGGING_THREAD = "THREAD";
    public static final String LOGGING_APP_NAME = "APP";
    public static final String REQUEST_GET_HEADER_ACCEPT = "Accept";
    public static final String REQUEST_GET_HEADER_CONTENT_TYPE = "Content-Type";
    public static final String REQUEST_GET_HEADER_AUTHORIZATION = "Authorization";
    public static final String MEDIA_TYPE_DEFAULT = "*/*";
    public static final String JSON_SERIALIZER_CONTENT_TYPE = "application/json";
    public static final String YAML_SERIALIZER_CONTENT_TYPE = "application/yaml";
    public static final String HTML_SERIALIZER_CONTENT_TYPE = "text/html";
    public static final String TEXT_SERIALIZER_CONTENT_TYPE = "text/plain";
    public static final String XML_SERIALIZER_CONTENT_TYPE = "application/xml";
    public static final String PATTERN_TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSX";
    public static final String WEB_CONFIG_API_APPLICATION = "/messageucolab/v1/application/**/message/*";
    public static final String WEB_CONFIG_API_MESSAGES = "/messageucolab/v1/messages/**";
    public static final String WEB_CONFIG_API_CATALOG = "/messageucolab/v1/catalog/**";
    public static final String LOCAL_WORKSPACE_PATH = "/messageucolab/v1/workspace/local";
    public static final String SESSION_APPLICATIONS_PATH = "/messageucolab/v1/session/applications";
    public static final String SESSION_APPLICATIONS_ATTRIBUTE = "sessionApplications";
    public static final String SECURITY_SESSION_COOKIE = "SECURITY_BASELINE_SESSION";
    public static final String REQUEST_HEADER_APPLICATION_ID = "X-Application-Id";
    public static final String REQUEST_HEADER_ENVIRONMENT_ID = "X-Environment-Id";

    public static final String SWAGGER_UI_HTML = "/swagger-ui.html";
    public static final String SWAGGER_UI = "/swagger-ui/**";
    public static final String SWAGGER_RESOURCES = "/swagger-resources/**";
    public static final String SWAGGER_API_DOCS = "/v3/api-docs/**";
    public static final String SWAGGER_WEBJARS = "/webjars/**";
    public static final String SWAGGER_OPENAPI_SPEC = "/openapi.yaml";

    public static final String MESSAGE_CODE_PARAMETER = "messageCode";

    public static final String SURREAL_CONFIG_PREFIX = "surreal";
    public static final String SURREAL_ADAPTER = "surrealRepositoryAdapter";

    public static final String PARAMETER_PROPERTIES_FILE = "parameter.properties";

}
