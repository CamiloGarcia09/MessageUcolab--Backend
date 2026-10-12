package co.edu.uco.crosscutting.catalog;

/**
 * Enumerador centralizado con todos los códigos de mensajes del catálogo
 * de la aplicación. Provee un único punto de referencia para cada código,
 * eliminando strings literales dispersos y garantizando type-safety.
 *
 * <p>Convención de prefijos:</p>
 * <ul>
 *   <li>{@code TCH_} — Mensajes técnicos (infraestructura, errores de sistema)</li>
 *   <li>{@code FUN_} — Mensajes funcionales (negocio, usuario)</li>
 * </ul>
 *
 * <p>Uso: {@code catalogPort.getMessage(MessageCatalogCodeEnum.TCH_041.getCode())}</p>
 */
public enum MessageCatalogCodeEnum {

    // =========================================================================
    // Mensajes técnicos — Conexiones / Componentes
    // =========================================================================

    /** Error al conectar con la base de datos. */
    TCH_001("TCH_001"),

    /** Error al conectar con el broker de mensajería. */
    TCH_002("TCH_002"),

    /** Error al conectar con la API de traducción. */
    TCH_003("TCH_003"),

    /** Error al conectar con la caché. */
    TCH_004("TCH_004"),

    /** Error al conectar con el componente de parámetros. */
    TCH_005("TCH_005"),

    /** Error al conectar con el componente de seguridad. */
    TCH_006("TCH_006"),

    // =========================================================================
    // Mensajes técnicos — Validación de argumentos del catálogo
    // =========================================================================

    /** La llave del mensaje es nula. */
    TCH_007("TCH_007"),

    /** La llave del mensaje está vacía o el código no existe. */
    TCH_008("TCH_008"),

    /** El mensaje no fue encontrado con la llave proporcionada. */
    TCH_009("TCH_009"),

    /** El código es requerido. */
    TCH_010("TCH_010"),

    /** El contenido es requerido. */
    TCH_011("TCH_011"),

    /** El título es requerido. */
    TCH_012("TCH_012"),

    /** El tipo es requerido. */
    TCH_013("TCH_013"),

    /** La categoría es requerida. */
    TCH_014("TCH_014"),

    // =========================================================================
    // Mensajes técnicos — Presenter / HTTP
    // =========================================================================

    /** Mensaje encontrado en la aplicación. */
    TCH_015("TCH_015"),

    /** Error de validación con id de correlación. */
    TCH_016("TCH_016"),

    /** No se encontró serializador para el media type. */
    TCH_017("TCH_017"),

    /** Error al serializar el objeto. */
    TCH_018("TCH_018"),

    /** Error al serializar la respuesta de error. */
    TCH_019("TCH_019"),

    /** La respuesta de error es. */
    TCH_020("TCH_020"),

    /** La respuesta exitosa es. */
    TCH_021("TCH_021"),

    /** El media type no es soportado. */
    TCH_022("TCH_022"),

    /** El media type es incorrecto. */
    TCH_023("TCH_023"),

    // =========================================================================
    // Mensajes técnicos — Cifrado / Seguridad
    // =========================================================================

    /** Error al generar el par de llaves. */
    TCH_024("TCH_024"),

    /** Error al generar el token. */
    TCH_025("TCH_025"),

    /** Error al generar las llaves. */
    TCH_026("TCH_026"),

    /** Error al generar la firma. */
    TCH_027("TCH_027"),

    /** Error al verificar acceso con llave privada, firma y secreto. */
    TCH_028("TCH_028"),

    /** Error al enviar la petición a Doppler. */
    TCH_029("TCH_029"),

    /** Código de error de la respuesta de Doppler. */
    TCH_030("TCH_030"),

    /** Acceso denegado, el token es inválido. */
    TCH_031("TCH_031"),

    /** Acceso denegado, el header 'Token' no fue enviado. */
    TCH_032("TCH_032"),

    /** Acceso denegado, el token está expirado o inactivo. */
    TCH_033("TCH_033"),

    /** Algoritmo SHA-256 no disponible. */
    TCH_034("TCH_034"),

    /** El id del entorno proporcionado no existe. */
    TCH_035("TCH_035"),

    /** Traducción dinámica completada. */
    TCH_036("TCH_036"),

    /** Traducción dinámica fallida. */
    TCH_037("TCH_037"),

    /** El dominio del mensaje a enviar al broker no puede ser nulo. */
    TCH_038("TCH_038"),

    /** Error al serializar el dominio del mensaje a JSON para el broker Pulsar. */
    TCH_039("TCH_039"),

    /** Los datos o la llave pública para generar la firma no pueden ser nulos. */
    TCH_040("TCH_040"),

    /** La URL del Azure Key Vault está ausente o vacía. */
    TCH_041("TCH_041"),

    /** Error al inicializar el Azure SecretClient con la URL. */
    TCH_042("TCH_042"),

    /** El nombre del secreto no puede ser nulo o vacío. */
    TCH_043("TCH_043"),

    /** El Azure SecretClient no está inicializado o la URL no fue proporcionada. */
    TCH_044("TCH_044"),

    /** El secreto existe pero tiene valor nulo o vacío. */
    TCH_045("TCH_045"),

    /** El secreto está deshabilitado en Azure Key Vault. */
    TCH_046("TCH_046"),

    /** El secreto no fue encontrado en Azure Key Vault (404). */
    TCH_047("TCH_047"),

    /** Falla de autenticación o permisos al acceder a Azure Key Vault. */
    TCH_048("TCH_048"),

    /** Error HTTP al comunicarse con Azure Key Vault. */
    TCH_049("TCH_049"),

    /** Error inesperado al obtener el secreto de Azure Key Vault. */
    TCH_050("TCH_050"),

    /** Error al obtener el modelo de mensaje desde Redis. */
    TCH_051("TCH_051"),

    /** Error al obtener el contenido del mensaje desde Redis. */
    TCH_052("TCH_052"),

    /** Error al obtener el título del mensaje desde Redis. */
    TCH_053("TCH_053"),

    /** Error al guardar el mensaje en Redis. */
    TCH_054("TCH_054"),

    /** La referencia estática al catálogo no está inicializada. */
    TCH_055("TCH_055"),

    /** Error al consultar un entorno en SurrealDB. */
    TCH_056("TCH_056"),

    /** Error al consultar los mensajes por entorno en SurrealDB. */
    TCH_057("TCH_057"),

    /** Error al ejecutar una consulta en SurrealDB. */
    TCH_058("TCH_058"),

    /** Error al consultar múltiples mensajes en SurrealDB. */
    TCH_059("TCH_059"),

    /** Error al contar registros en SurrealDB. */
    TCH_060("TCH_060"),

    /** Error al guardar el token en SurrealDB. */
    TCH_061("TCH_061"),

    /** Token persistido en SurrealDB. */
    TCH_062("TCH_062"),

    /** Proyección de eventos de dominio en SurrealDB. */
    TCH_063("TCH_063"),

    /** Error al proyectar un evento de dominio en SurrealDB. */
    TCH_064("TCH_064"),

    /** Evento de dominio almacenado solo como documento crudo. */
    TCH_065("TCH_065"),

    /** Error al persistir el mensaje en SurrealDB. */
    TCH_066("TCH_066"),

    /** Error técnico al persistir el mensaje en la base de datos SurrealDB. */
    TCH_067("TCH_067"),

    /**Parameter code is null or empty */
    TCH_068("TCH_068"),

    /**Unexpected parameter retrieval error */
    TCH_069("TCH_069"),

    /**Technical parameter retrieval error */
    TCH_070("TCH_070"),

    /** El parámetro con código '%s' no fue encontrado en el catálogo. */
    TCH_071("TCH_071"),


    // =========================================================================
    // Mensajes funcionales — Validación y caché
    // =========================================================================

    /** El código de mensaje es inválido o no está dentro de los valores permitidos. */
    FUN_001("FUN_001"),

    /** El título está vacío o es nulo. */
    FUN_002("FUN_002"),

    /** El contenido está vacío o es nulo. */
    FUN_003("FUN_003"),

    /** El tipo de mensaje no está definido. */
    FUN_004("FUN_004"),

    /** La categoría del mensaje no está definida. */
    FUN_005("FUN_005"),

    /** El mensaje no fue encontrado en caché, se procede a buscar en base de datos. */
    FUN_006("FUN_006"),

    /** El mensaje fue encontrado en base de datos, se procede a retornar y cachear. */
    FUN_007("FUN_007"),

    /** La cantidad de mensajes en caché y base de datos no coincide. */
    FUN_008("FUN_008"),

    /** El mensaje fue encontrado en caché, se procede a retornar. */
    FUN_009("FUN_009"),

    /** Validator nulo, el atributo es requerido. */
    FUN_010("FUN_010"),

    /** No se pudieron obtener los mensajes de la aplicación, verifique que exista. */
    FUN_011("FUN_011"),

    /** No hay mensaje con el código para la aplicación. */
    FUN_012("FUN_012"),

    /** Fallo al conectar a Redis. */
    FUN_013("FUN_013"),

    /** Excepción de acceso a datos al conectar a Redis. */
    FUN_014("FUN_014"),

    /** Excepción inesperada al conectar a Redis. */
    FUN_015("FUN_015"),

    /** El contenido no puede estar vacío. */
    FUN_017("FUN_017"),

    /** El tamaño del contenido no debe ser menor a 10. */
    FUN_018("FUN_018"),

    /** El tamaño del contenido no puede ser mayor a 100. */
    FUN_019("FUN_019"),

    /** El tamaño del título no puede ser menor a 10. */
    FUN_020("FUN_020"),

    /** El tamaño del título no puede ser mayor a 50. */
    FUN_021("FUN_021"),

    /** El título no puede estar vacío. */
    FUN_022("FUN_022"),

    /** Ha ocurrido un error inesperado. */
    FUN_023("FUN_023"),

    /** Información consultada. */
    FUN_024("FUN_024"),

    /** Error al verificar el token, intente nuevamente más tarde. */
    FUN_025("FUN_025"),

    /** El token no fue encontrado. */
    FUN_026("FUN_026"),

    /** Información consultada. */
    FUN_027("FUN_027"),

    /** El número de página debe estar entre 1 y el total. */
    FUN_028("FUN_028"),

    /** El tamaño de página debe estar entre 1 y el máximo. */
    FUN_029("FUN_029"),

    /** La columna de ordenamiento no es válida. */
    FUN_030("FUN_030"),

    /** La dirección de ordenamiento debe ser 'ASC' o 'DESC'. */
    FUN_031("FUN_031"),

    /** La página no puede ser menor a 1. */
    FUN_032("FUN_032"),

    /** El valor del atributo debe ser un entero válido mayor a 1. */
    FUN_033("FUN_033"),

    /** No se han agregado validadores. */
    FUN_034("FUN_034"),

    /** El entorno no existe. */
    FUN_035("FUN_035"),

    /** El entorno no pertenece a la aplicación indicada. */
    FUN_036("FUN_036"),

    /** La fecha de expiración debe ser una fecha mayor a hoy. */
    FUN_037("FUN_037"),

    /** El id no debe ser el UUID por defecto. */
    FUN_038("FUN_038"),

    /** La fecha contiene caracteres no permitidos. */
    FUN_039("FUN_039"),

    /** El código de mensaje no puede estar vacío o ser nulo. */
    FUN_040("FUN_040"),

    /** Error al buscar el token. */
    FUN_041("FUN_041"),

    /** El número de página excede el total de páginas. */
    FUN_042("FUN_042"),

    /** El valor del atributo no puede contener caracteres especiales. */
    FUN_043("FUN_043"),

    /** El idioma destino es requerido. */
    FUN_044("FUN_044"),

    /** El idioma destino es inválido. */
    FUN_045("FUN_045"),

    /** La traducción dinámica está deshabilitada. */
    FUN_046("FUN_046"),

    /** La traducción dinámica necesita la API key de traducción. */
    FUN_047("FUN_047"),

    /** El mensaje no pudo ser traducido dinámicamente. */
    FUN_048("FUN_048"),

    /** El token no fue encontrado en la base de datos. */
    FUN_049("FUN_049"),

    /** El UUID a convertir no tiene un formato válido. */
    FUN_050("FUN_050"),

    /** Error inesperado al convertir la entrada a UUID. */
    FUN_051("FUN_051"),

    /** La fecha a convertir no tiene un formato válido. */
    FUN_052("FUN_052"),

    /** Ha ocurrido un error inesperado. */
    FUN_053("FUN_053"),

    /** Plantilla del prompt de traducción dinámica. */
    FUN_054("FUN_054"),

    /** Nombre del esquema de traducción. */
    FUN_055("FUN_055"),

    /** Campo de título traducido. */
    FUN_056("FUN_056"),

    /** Campo de contenido traducido. */
    FUN_057("FUN_057"),

    /** Nombre del proveedor de traducción OpenAI. */
    FUN_058("FUN_058"),

    /** Patrón del nombre del proveedor de traducción. */
    FUN_059("FUN_059"),

    /** Columna de ordenamiento por defecto. */
    FUN_060("FUN_060"),

    /** Identificador del secreto del token. */
    FUN_061("FUN_061"),

    /** Nombre del campo secreto del puerto de secretos. */
    FUN_062("FUN_062"),

    /** Nombre del campo llave privada del puerto de secretos. */
    FUN_063("FUN_063"),

    /** Patrón de validación de fechas. */
    FUN_064("FUN_064"),

    /** Atributo de página. */
    FUN_065("FUN_065"),

    /** Atributo de tamaño. */
    FUN_066("FUN_066"),

    /** Atributo de columna de ordenamiento. */
    FUN_067("FUN_067"),

    /** Atributo de ordenamiento. */
    FUN_068("FUN_068"),

    /** Estado activo. */
    FUN_069("FUN_069"),

    /** Estado inactivo. */
    FUN_070("FUN_070"),

    /** Dirección de ordenamiento ascendente. */
    FUN_071("FUN_071"),

    /** Dirección de ordenamiento descendente. */
    FUN_072("FUN_072"),

    /** Id del estado de token activo. */
    FUN_073("FUN_073"),

    /** Colección de tokens. */
    FUN_074("FUN_074"),

    /** Colección de estados de token. */
    FUN_075("FUN_075"),

    /** Colección de mensajes por entorno. */
    FUN_076("FUN_076"),

    /** Colección de entornos. */
    FUN_077("FUN_077"),

    /** Colección de aplicaciones. */
    FUN_078("FUN_078"),

    /** Colección de estados de mensaje por entorno. */
    FUN_079("FUN_079"),

    /** Colección de tipos de entorno. */
    FUN_080("FUN_080"),

    /** Colección de parámetros representados. */
    FUN_081("FUN_081"),

    /** Colección de parámetros. */
    FUN_082("FUN_082"),

    /** Campo de nombre. */
    FUN_083("FUN_083"),

    /** Campo de fecha de creación. */
    FUN_084("FUN_084"),

    /** Campo de fecha de expiración. */
    FUN_085("FUN_085"),

    /** Campo de id de entorno. */
    FUN_086("FUN_086"),

    /** Campo de id de mensaje por entorno. */
    FUN_087("FUN_087"),

    /** Campo de mensaje. */
    FUN_088("FUN_088"),

    /** Campo de nombre de secreto. */
    FUN_089("FUN_089"),

    /** Campo de id de estado. */
    FUN_090("FUN_090"),

    /** Campo de id de tipo. */
    FUN_091("FUN_091"),

    /** Campo de id de aplicación. */
    FUN_092("FUN_092"),

    /** Atributo de id de entorno. */
    FUN_093("FUN_093"),

    /** Entidad de datos del token. */
    FUN_094("FUN_094"),

    /** Id del estado de token activo en infraestructura. */
    FUN_095("FUN_095"),

    /** Etiqueta HTML de apertura. */
    FUN_096("FUN_096"),

    /** Etiqueta HTML de cierre. */
    FUN_097("FUN_097"),

    /** Etiqueta de apertura del cuerpo. */
    FUN_098("FUN_098"),

    /** Etiqueta de cierre del cuerpo. */
    FUN_099("FUN_099"),

    /** Etiqueta de apertura de preformateado. */
    FUN_100("FUN_100"),

    /** Etiqueta de cierre de preformateado. */
    FUN_101("FUN_101"),

    /** Header de id de correlación. */
    FUN_102("FUN_102"),

    /** Atributo de uri de la petición para logging. */
    FUN_103("FUN_103"),

    /** Atributo de método HTTP para logging. */
    FUN_104("FUN_104"),

    /** Atributo de id de sesión para logging. */
    FUN_105("FUN_105"),

    /** Atributo de query string para logging. */
    FUN_106("FUN_106"),

    /** Nombre de la aplicación para logging. */
    FUN_107("FUN_107"),

    /** Parámetro de código de mensaje para logging. */
    FUN_108("FUN_108"),

    /** Parámetro de aplicación para logging. */
    FUN_109("FUN_109"),

    /** Atributo de timestamp para logging. */
    FUN_110("FUN_110"),

    /** Atributo de hilo para logging. */
    FUN_111("FUN_111"),

    /** Atributo de nombre de aplicación para logging. */
    FUN_112("FUN_112"),

    /** Header Accept. */
    FUN_113("FUN_113"),

    /** Header Token. */
    FUN_114("FUN_114"),

    /** Header Content-Type. */
    FUN_115("FUN_115"),

    /** Header Authorization. */
    FUN_116("FUN_116"),

    /** Media type por defecto. */
    FUN_117("FUN_117"),

    /** Content type JSON. */
    FUN_118("FUN_118"),

    /** Patrón de token Bearer. */
    FUN_119("FUN_119"),

    /** Content type YAML. */
    FUN_120("FUN_120"),

    /** Content type HTML. */
    FUN_121("FUN_121"),

    /** Content type de texto plano. */
    FUN_122("FUN_122"),

    /** Content type XML. */
    FUN_123("FUN_123"),

    /** Patrón de formato de timestamp. */
    FUN_124("FUN_124"),

    /** Algoritmo de generación de llaves. */
    FUN_125("FUN_125"),

    /** Algoritmo de cifrado de llaves. */
    FUN_126("FUN_126"),

    /** Campo secretName del DTO de Doppler. */
    FUN_127("FUN_127"),

    /** Campo privateKey del DTO de Doppler. */
    FUN_128("FUN_128"),

    /** Campo raw del DTO de Doppler. */
    FUN_129("FUN_129"),

    /** Ruta del interceptor de la API de mensajes. */
    FUN_130("FUN_130"),

    /** Ruta del interceptor de la API de aplicaciones. */
    FUN_131("FUN_131"),

    /** Ruta del interceptor de la API de entornos. */
    FUN_132("FUN_132"),

    /** Ruta del interceptor de la API de listado de mensajes. */
    FUN_133("FUN_133"),

    /** Ruta del interceptor de la API de código de mensaje. */
    FUN_134("FUN_134"),

    /** Ruta del HTML de Swagger UI. */
    FUN_135("FUN_135"),

    /** Ruta de Swagger UI. */
    FUN_136("FUN_136"),

    /** Ruta de recursos de Swagger. */
    FUN_137("FUN_137"),

    /** Ruta de API docs de Swagger. */
    FUN_138("FUN_138"),

    /** Ruta de webjars de Swagger. */
    FUN_139("FUN_139"),

    /** Adaptador de repositorio Surreal. */
    FUN_140("FUN_140"),

    /** Tabla de tokens en Surreal. */
    FUN_141("FUN_141"),

    /** Tabla de estados de token en Surreal. */
    FUN_142("FUN_142"),

    /** Ruta del interceptor de la API de traducción de mensajes. */
    FUN_143("FUN_143"),

    /** Id del estado de token inactivo. */
    FUN_144("FUN_144"),

    /** El entorno solicitado no coincide con el entorno autenticado. */
    FUN_145("FUN_145"),

    /** La funcionalidad no pertenece a la aplicación indicada. */
    FUN_146("FUN_146"),

    /** El nombre de la organización es requerido. */
    FUN_147("FUN_147"),

    /** El nombre de la organización supera la longitud permitida. */
    FUN_148("FUN_148"),

    /** Ya existe una organización con el nombre indicado. */
    FUN_149("FUN_149"),

    /** El identificador de la organización es requerido. */
    FUN_150("FUN_150"),

    /** La organización asociada a la aplicación no existe. */
    FUN_151("FUN_151"),

    /** Se requiere autenticación para acceder al recurso. */
    FUN_152("FUN_152"),

    /** La identidad no tiene permiso para acceder al recurso. */
    FUN_153("FUN_153"),

    /** No se ha seleccionado un contexto activo. */
    FUN_154("FUN_154"),

    /** Los identificadores del contexto son requeridos. */
    FUN_155("FUN_155"),

    /** La organización seleccionada no existe. */
    FUN_156("FUN_156"),

    /** La aplicación seleccionada no existe. */
    FUN_157("FUN_157"),

    /** El entorno seleccionado no existe. */
    FUN_158("FUN_158"),

    /** La aplicación no pertenece a la organización seleccionada. */
    FUN_159("FUN_159"),

    /** El entorno no pertenece a la aplicación seleccionada. */
    FUN_160("FUN_160"),

    /** El nombre de la aplicación es requerido. */
    FUN_161("FUN_161"),

    /** El nombre de la aplicación no puede superar los 50 caracteres. */
    FUN_162("FUN_162"),

    /** El idioma de la aplicación es requerido. */
    FUN_163("FUN_163"),

    /** El idioma de la aplicación no existe. */
    FUN_164("FUN_164"),

    /** El estado de la aplicación es requerido. */
    FUN_167("FUN_167"),

    /** El estado de la aplicación no existe. */
    FUN_168("FUN_168"),

    /** Ya existe una aplicación con el nombre proporcionado. */
    FUN_169("FUN_169"),

    /** El id de la aplicación es requerido. */
    FUN_172("FUN_172"),

    /** La aplicación a la que se asocia el entorno no existe. */
    FUN_173("FUN_173"),

    /** El tipo del entorno es requerido. */
    FUN_174("FUN_174"),

    /** El tipo de entorno no existe. */
    FUN_175("FUN_175"),

    /** El estado del entorno es requerido. */
    FUN_176("FUN_176"),

    /** El estado del entorno no existe. */
    FUN_177("FUN_177"),

    /** Ya existe un entorno del mismo tipo para la aplicación. */
    FUN_178("FUN_178"),

    /** El nombre de la funcionalidad es requerido. */
    FUN_179("FUN_179"),

    /** El nombre de la funcionalidad no puede superar los 50 caracteres. */
    FUN_180("FUN_180"),

    /** La aplicación a la que se asocia la funcionalidad no existe. */
    FUN_181("FUN_181"),

    /** El estado de la funcionalidad es requerido. */
    FUN_184("FUN_184"),

    /** El estado de la funcionalidad no existe. */
    FUN_185("FUN_185"),

    /** Ya existe una funcionalidad con el mismo nombre para la aplicación. */
    FUN_186("FUN_186"),

    /** El id del entorno es requerido. */
    FUN_187("FUN_187"),

    /** El id de la funcionalidad es requerido. */
    FUN_188("FUN_188"),

    /** El tipo del mensaje es requerido. */
    FUN_190("FUN_190"),

    /** El tipo del mensaje no existe. */
    FUN_191("FUN_191"),

    /** La categoría del mensaje es requerida. */
    FUN_192("FUN_192"),

    /** La categoría del mensaje no existe. */
    FUN_193("FUN_193"),

    /** El estado del mensaje es requerido. */
    FUN_194("FUN_194"),

    /** El estado del mensaje no existe. */
    FUN_195("FUN_195"),

    /** El estado del mensaje en el ambiente es requerido. */
    FUN_196("FUN_196"),

    /** El estado del mensaje en el ambiente no existe. */
    FUN_197("FUN_197"),

    /** El sufijo del código del mensaje supera la longitud máxima. */
    FUN_198("FUN_198"),

    /** El sufijo del código del mensaje tiene un formato inválido. */
    FUN_199("FUN_199"),

    /** Ya existe un mensaje con el mismo código para la aplicación. */
    FUN_200("FUN_200"),

    /** Instrucciones de sistema para la traducción dinámica. */
    FUN_201("FUN_201"),

    /** Plantilla del bloque JSON de textos a traducir. */
    FUN_202("FUN_202"),

    /** Campo de tipo traducido. */
    FUN_203("FUN_203"),

    /** Campo de categoría traducida. */
    FUN_204("FUN_204"),

    /** Campo de funcionalidad traducida. */
    FUN_205("FUN_205");

    // =========================================================================

    private final String code;

    MessageCatalogCodeEnum(String code) {
        this.code = code;
    }

    /**
     * Retorna el código de mensaje tal como está definido en el catálogo.
     *
     * @return el código del mensaje (p.ej. {@code "TCH_041"})
     */
    public String getCode() {
        return code;
    }
}
