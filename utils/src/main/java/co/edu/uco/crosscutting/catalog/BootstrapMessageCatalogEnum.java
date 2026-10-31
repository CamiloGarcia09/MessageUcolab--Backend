package co.edu.uco.crosscutting.catalog;

import java.util.Arrays;
import java.util.Optional;

import static co.edu.uco.crosscutting.helpers.UtilText.trim;

public enum BootstrapMessageCatalogEnum {

    TCH_001("TCH_001", "Base de datos desconectada", "No fue posible conectarse a la base de datos %s.", "TECHNICAL", "ERROR"),
    TCH_002("TCH_002", "Intermediario de mensajes desconectado", "No fue posible conectarse al intermediario de mensajes.", "TECHNICAL", "ERROR"),
    TCH_003("TCH_003", "API de traducción desconectada", "No fue posible conectarse a la API de traducción.", "TECHNICAL", "ERROR"),
    TCH_004("TCH_004", "Caché desconectada", "No fue posible conectarse a la caché.", "TECHNICAL", "ERROR"),
    TCH_005("TCH_005", "Componente de parámetros desconectado", "No fue posible conectarse al componente de parámetros.", "TECHNICAL", "ERROR"),
    TCH_006("TCH_006", "Componente de seguridad desconectado", "No fue posible conectarse al componente de seguridad.", "TECHNICAL", "ERROR"),
    TCH_007("TCH_007", "Clave nula", "La clave del mensaje es nula.", "TECHNICAL", "ERROR"),
    TCH_008("TCH_008", "Clave vacía", "El código del mensaje no existe.", "TECHNICAL", "ERROR"),
    TCH_009("TCH_009", "Mensaje no encontrado", "No existe un código de mensaje con la clave %s.", "TECHNICAL", "ERROR"),

    TCH_016("TCH_016", "Error de validación", "Error de validación con identificador de correlación {}", "TECHNICAL", "ERROR"),
    TCH_017("TCH_017", "Serializador no encontrado", "No se encontró un serializador para el tipo de contenido '{}' ni hay uno predeterminado configurado.", "TECHNICAL", "ERROR"),
    TCH_018("TCH_018", "Error de serialización", "Ocurrió un error al serializar el objeto.", "TECHNICAL", "ERROR"),
    TCH_019("TCH_019", "Error al serializar la respuesta de error", "Ocurrió un error al serializar la respuesta de error.", "TECHNICAL", "ERROR"),
    TCH_020("TCH_020", "Respuesta de error", "La respuesta de error es: {}", "TECHNICAL", "ERROR"),
    TCH_021("TCH_021", "Respuesta correcta", "La respuesta correcta es: {}", "TECHNICAL", "INFORMATION"),
    TCH_022("TCH_022", "Tipo de contenido no admitido", "El sistema no admite el tipo de contenido %s.", "TECHNICAL", "ERROR"),
    TCH_023("TCH_023", "Tipo de contenido incorrecto", "No se admite el tipo de contenido {}.", "TECHNICAL", "ERROR"),
    TCH_041("TCH_041", "URL de Key Vault no configurada", "La URL de Azure Key Vault no está configurada o está vacía.", "TECHNICAL", "ERROR"),
    TCH_042("TCH_042", "Error al iniciar el cliente de secretos", "No fue posible inicializar el cliente de secretos de Azure con la URL: %s", "TECHNICAL", "ERROR"),

    TCH_051("TCH_051", "Error al obtener el modelo desde Redis", "No fue posible obtener el modelo del mensaje desde Redis para la clave: %s", "TECHNICAL", "ERROR"),
    TCH_052("TCH_052", "Error al obtener el contenido desde Redis", "No fue posible obtener el contenido del mensaje desde Redis para la clave: %s", "TECHNICAL", "ERROR"),
    TCH_053("TCH_053", "Error al obtener el título desde Redis", "No fue posible obtener el título del mensaje desde Redis para la clave: %s", "TECHNICAL", "ERROR"),
    TCH_054("TCH_054", "Error al guardar el mensaje en Redis", "No fue posible guardar el mensaje en Redis para la clave: %s", "TECHNICAL", "ERROR"),
    TCH_055("TCH_055", "Referencia del catálogo no inicializada", "La referencia estática del catálogo no está inicializada.", "TECHNICAL", "ERROR"),
    TCH_056("TCH_056", "Error al consultar ambientes", "Ocurrió un error al consultar ambientes en SurrealDB. Consulta: %s", "TECHNICAL", "ERROR"),
    TCH_057("TCH_057", "Error al consultar mensajes por ambiente", "Ocurrió un error al buscar mensajes por ambiente en SurrealDB. Identificador del ambiente: %s", "TECHNICAL", "ERROR"),
    TCH_058("TCH_058", "Error de consulta en SurrealDB", "Ocurrió un error al consultar SurrealDB. Consulta: %s", "TECHNICAL", "ERROR"),
    TCH_059("TCH_059", "Error al consultar varios mensajes", "Ocurrió un error al consultar varios mensajes en SurrealDB. Consulta: %s", "TECHNICAL", "ERROR"),
    TCH_060("TCH_060", "Error al contar registros", "Ocurrió un error al contar registros en SurrealDB. Consulta: %s", "TECHNICAL", "ERROR"),
    TCH_066("TCH_066", "Error al guardar el mensaje", "Error al persistir mensaje en SurrealDB", "TECHNICAL", "ERROR"),
    TCH_067("TCH_067", "Error técnico al guardar el mensaje", "Error al persistir el mensaje en la base de datos SurrealDB", "TECHNICAL", "ERROR"),
    TCH_078("TCH_078", "Error al consultar mensajes por aplicación", "Ocurrió un error al buscar mensajes por aplicación en SurrealDB. Identificador de aplicación: %s", "TECHNICAL", "ERROR"),

    FUN_013("FUN_013", "Caché desconectada", "No fue posible conectarse a Redis.", "FUNCTIONAL", "ERROR"),
    FUN_014("FUN_014", "Error al conectarse a la caché", "Ocurrió un error de acceso a datos al conectarse a Redis.", "FUNCTIONAL", "ERROR"),
    FUN_015("FUN_015", "Excepción inesperada", "Ocurrió una excepción inesperada al conectarse a Redis.", "FUNCTIONAL", "ERROR"),
    FUN_023("FUN_023", "Error inesperado", "Ocurrió un error inesperado.", "FUNCTIONAL", "ERROR"),
    FUN_050("FUN_050", "Formato UUID inválido", "El valor que se convertirá a UUID no tiene un formato válido.", "FUNCTIONAL", "ERROR"),
    FUN_051("FUN_051", "Error de conversión a UUID", "Ocurrió un error inesperado al convertir el valor a UUID.", "FUNCTIONAL", "ERROR"),
    FUN_052("FUN_052", "Formato de fecha inválido", "La fecha que se convertirá no tiene un formato válido.", "FUNCTIONAL", "ERROR"),
    TCH_080("TCH_080", "Nombre de archivo obligatorio", "El nombre del archivo de propiedades no puede ser nulo ni estar vacío.", "TECHNICAL", "ERROR"),
    TCH_081("TCH_081", "Clave de propiedad obligatoria", "La clave no puede ser nula ni estar vacía al leer: %s", "TECHNICAL", "ERROR"),
    TCH_082("TCH_082", "Archivo de propiedades no encontrado", "No se encontró el archivo de propiedades: %s", "TECHNICAL", "ERROR"),
    TCH_083("TCH_083", "Propiedad no encontrada", "No se encontró la clave '%s' en: %s", "TECHNICAL", "ERROR"),
    TCH_084("TCH_084", "Error al leer propiedades", "Ocurrió un error al leer el archivo de propiedades: %s", "TECHNICAL", "ERROR"),
    TCH_085("TCH_085", "Conexión con SurrealDB", "Conectando con SurrealDB en: {}", "TECHNICAL", "INFORMATION"),
    TCH_086("TCH_086", "Conexión establecida con SurrealDB", "Se inició sesión correctamente en SurrealDB.", "TECHNICAL", "INFORMATION"),
    TCH_104("TCH_104", "Contexto de SurrealDB", "Se utiliza el espacio de nombres {} y la base de datos {}.", "TECHNICAL", "INFORMATION"),
    TCH_105("TCH_105", "Solicitud completada", "Solicitud completada con estado {}", "TECHNICAL", "INFORMATION");

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
