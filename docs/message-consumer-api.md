# Consulta de mensajes para aplicaciones consumidoras

## Petición

```http
GET /messageucolab/v1/messages/consumer?code=MSG_WELCOME
Accept: application/json
X-Application-Id: <UUID de una aplicación de una organización propia>
X-Environment-Id: <UUID de un ambiente de esa aplicación>
```

Esta ruta reemplaza `/messageucolab/v1/messages/resolve`, sin alias de compatibilidad.
Los clientes deben actualizar su URL y el responsable de seguridad debe registrar
la nueva ruta y su permiso en el PEP. Este cambio no modifica seguridad ni sus datos.

La URL base depende del despliegue, por ejemplo `http://localhost:8000` detrás
del gateway. El componente externo de seguridad autentica y autoriza la petición.
El navegador administrativo conserva la sesión BFF y sus cookies; los servicios
consumidores deben acordar sus credenciales y permisos con el responsable de seguridad.
No se emiten credenciales en MessageUcoLab ni se acepta el header propietario retirado.
En el flujo predeterminado de sesión BFF, `X-Application-Id` debe corresponder a una aplicación de una
organización propiedad del usuario autenticado y `X-Environment-Id` a un ambiente de esa aplicación. El ambiente se pasa
explícitamente a la operación, no forma parte de `MessageAccessContext` ni se usa
para inferir la aplicación. Se validan su UUID, existencia y pertenencia antes de
consultar el mensaje. Este flujo no necesita perfiles de activación ni banderas
del frontend; no existe un destino fijo de desarrollo.

Las aplicaciones de negocio tienen UUID propios, no los de las aplicaciones de
seguridad. Este modelo personal no implementa credenciales máquina-a-máquina ni
autoriza consumidores sin identidad BFF; esa evolución requiere un contrato
explícito, sin compartir la sesión del propietario. Ver [espacios personales](personal-workspaces.md).

## Respuesta exitosa: HTTP 200

```json
{
  "code": "MSG_WELCOME",
  "title": "Bienvenido",
  "content": "Bienvenido a MessageUcoLab",
  "type": "Functional",
  "category": "Information",
  "functionality": "Inicio de sesión",
  "created_at": "2026-10-20T14:30:00Z",
  "updated_at": "2026-10-26T09:15:00Z"
}
```

Este endpoint devuelve directamente el objeto JSON, sin el envoltorio
`data`/`errors` de las consultas administrativas. Contiene `code`, `title`, `content`,
`type`, `category`, `functionality`, `created_at` y `updated_at`. No expone IDs,
estado ni ambientes asociados. Las fechas son ISO 8601 UTC o `null` si no están
disponibles. Los nombres de tipo, categoría y funcionalidad son descriptivos,
no identificadores estables para reglas del consumidor. Los endpoints
administrativos conservan sus contratos.

El contenido es el original registrado. Se asume que está escrito en el idioma
base de la aplicación (`application.language_id`); no se detecta ni se traduce
el idioma y no se admite selección de idioma en esta versión. La aplicación
consumidora debe tratar los textos como texto plano y decidir cómo mostrarlos.
Los tokens dentro del contenido se entregan sin sustitución.

## Disponibilidad y aislamiento

Solo se entrega un mensaje si está activo y tiene una asociación activa con el
ambiente autorizado. Además, la aplicación del mensaje debe coincidir con la
del ambiente. El código se resuelve dentro de ese contexto, no globalmente.

Se reutiliza `MessageCatalogStrategy` y el adaptador Redis existente:

1. Tras validar la propiedad y la pertenencia del ambiente, se busca por código
   y ambiente en Redis. El payload debe corresponder a la aplicación autorizada.
2. Ante un acierto, una consulta ligera sobre `message_environment` y sus referencias
   comprueba disponibilidad, ID y `updated_at` del mensaje en SurrealDB. No recarga
   el contenido si coincide la versión. Si ya no está disponible, responde `404`.
3. Si no hay entrada válida, se carga el mensaje completo desde las tablas fuente
   de SurrealDB, no desde las proyecciones administrativas, y se guarda en Redis.
4. Los fallos de Redis permiten consultar la base de datos. Un fallo de SurrealDB
   no autoriza servir una entrada sin comprobar su disponibilidad.

Se mantiene el TTL de Redis de 120 segundos y sus claves existentes; no se vacía
la caché. Las entradas anteriores sin `updatedAt` se recargan. La edición y la
eliminación invalidan la caché por código; los cambios de estado o disponibilidad
también se comprueban en cada acierto. Los nombres de los catálogos relacionados
pueden mantenerse en caché hasta su invalidación o expiración.

El esquema Redis existente guarda una entrada por ID de mensaje: alternar consultas
entre ambientes puede reemplazar la entrada y reducir los aciertos, pero nunca
autoriza reutilizarla fuera del ambiente consultado. No se rediseñan sus claves.

La caché interna del servidor es independiente de la HTTP: la respuesta conserva
`Cache-Control: no-store` para evitar reutilización de respuestas entre contextos.

## Errores

- `400`: falta el parámetro `code`.
- Código vacío: se aplica el validador de códigos existente y su respuesta de dominio.
- `401`: credencial ausente o inválida.
- `403`: sesión o identidad sin permiso para consultar mensajes.
- `404`: mensaje inexistente, inactivo, deshabilitado para el ambiente o de otra aplicación.
- `500`: fallo técnico; no se presenta como mensaje inexistente.

El detalle del `404` indica que el mensaje no está disponible para la aplicación y
el ambiente solicitados, con cada UUID en su lugar. No distingue entre inexistencia
e inactividad. La plantilla específica `FUN_218` se obtiene del catálogo Redis;
debe cargarse antes de desplegar esta versión. `FUN_012` conserva su significado
para las consultas administrativas por aplicación.

Los errores conservan el formato centralizado existente de la API. El adaptador
HTTP del consumidor debe comprobar el estado antes de deserializar el mensaje
y definir timeout y comportamiento de respaldo ante indisponibilidad.

La consulta entrega un solo código en su idioma base. Las consultas por conjuntos
quedan fuera de este contrato. Para obtenerlo traducido se usa la operación siguiente.

## Traducción para consumo

```http
POST /messageucolab/v1/messages/consumer/translations?code=MSG_WELCOME
Accept: application/json
Content-Type: application/json
X-Application-Id: <UUID de una aplicación de una organización propia>
X-Environment-Id: <UUID de un ambiente de esa aplicación>

{ "targetLanguage": "en" }
```

La autenticación BFF y los permisos del PEP siguen siendo obligatorios. El nuevo
path necesita su propio registro y permiso en seguridad; MessageUcoLab no cambia
ese componente. El minifront administrativo conserva su endpoint de traducción.

El body solo necesita `targetLanguage`: un código de dos o tres letras, con subtags
opcionales de 2 a 8 caracteres alfanuméricos (`en`, `es`, `pt-BR`, `zh-Hans`), hasta
50 caracteres en total. Se recortan espacios exteriores, se convierten guiones bajos
a guiones y se normaliza a minúsculas. No acepta nombres libres de idiomas ni `auto`.
El idioma origen se lee de `application.language_id.code` (la referencia a
`language_base`); no se toma del body ni se autodetecta. Si falta o es inválido,
se rechaza con `422` y `FUN_219`, sin llamar al traductor.

### Respuesta HTTP 200

```json
{
  "code": "MSG_WELCOME",
  "sourceLanguage": "es",
  "targetLanguage": "en",
  "title": "Welcome",
  "content": "Welcome to MessageUcoLab",
  "type": "Functional",
  "category": "Information",
  "functionality": "Sign in",
  "created_at": "2026-10-20T14:30:00Z",
  "updated_at": "2026-10-26T09:15:00Z"
}
```

Es un objeto JSON directo de diez campos, sin envelope, IDs, estado, listado de
ambientes ni metadatos del proveedor. Los cinco textos descriptivos son los del
resultado traducido; el código permanece intacto. Las fechas son las del mensaje
original. Si origen y destino coinciden, se entregan los textos originales sin
invocar la IA. Se conserva `Cache-Control: no-store`.

### Disponibilidad, consistencia y caché

- Se reutiliza la misma operación de resolución de `/consumer` antes de consultar
  traducciones: comprueba propiedad, ambiente, mensaje activo y asociación activa.
  Si el mensaje no está disponible, devuelve `404` aunque exista traducción en Redis;
  no consulta el cache de traducciones ni invoca la IA.
- `ConsumerMessageTranslationStrategy` reutiliza `MessageTranslationCachePort` y
  el adaptador Redis existente. Busca primero en caché; ante ausencia o fallo Redis,
  invoca el motor existente y guarda el resultado con el TTL ya configurado.
- El scope es `consumer-v1:<applicationId>:<environmentId>:<huella SHA-256>`; la
  clave existente añade código e idiomas. La huella incluye los textos, código,
  fechas e idioma base, con separación inequívoca por longitud. No se cambian ni
  se borran las claves administrativas. Una edición genera un scope distinto;
  las traducciones anteriores expiran por TTL sin utilizarse para la nueva versión.
- Tras obtener la traducción, incluso en un hit, se ejecuta otra resolución de
  consumo y se vuelve a leer el idioma base antes de devolver el resultado.
  Si ya no está disponible, responde `404`; si difieren los textos, fechas o idioma
  base, responde `409` con `FUN_220`, y el consumidor puede reintentar.
- La verificación final ocurre inmediatamente antes de construir la respuesta;
  no es una transacción que mantenga bloqueado el mensaje durante la IA ni durante
  la transmisión HTTP. Un fallo técnico no autoriza omitir la comprobación.
- Solo se guarda la traducción temporal en Redis, no como mensaje ni como traducción
  persistida en SurrealDB. Los fallos Redis no convierten la operación en un error
  si el motor y la validación autoritativa funcionan. Los nombres de catálogos
  relacionados conservan las condiciones de frescura de la resolución de consumo.

Además de los errores de consulta, aplican `422` por idioma destino inválido o idioma
base no configurado y `409` por cambios concurrentes. Los fallos del motor conservan
la jerarquía y el manejo de errores existentes. Antes de desplegar, cargar los códigos
`FUN_219` y `FUN_220` desde `deployment/docker/scripts/redis/CatalogMessageInit.sh`,
sin borrar Redis ni reimportar los datos de SurrealDB.
