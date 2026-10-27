# Contract First — API de MessageUcoLab

La referencia detallada de entradas, salidas, ejemplos y errores está en
[`infrastructure/src/main/resources/static/openapi.yaml`](../infrastructure/src/main/resources/static/openapi.yaml).
El contrato está definido en OpenAPI 3.0.3 y se puede compartir directamente e importar en Swagger Editor o Postman.

## Cómo consultar y compartir

- Compartir el archivo `openapi.yaml` junto con esta guía.
- Para consultarlo sin ejecutar la aplicación, importar el YAML en [Swagger Editor](https://editor.swagger.io/).
- Con la aplicación en ejecución, abrir `/swagger-ui.html` en la URL del despliegue.
- El mismo archivo está publicado en `/openapi.yaml`.
- La URL base definida en el contrato es `http://localhost:8000`.

Esta documentación cubre **17 operaciones HTTP**. La creación de credenciales está fuera de su alcance.
Las credenciales para consumir las operaciones protegidas se obtienen del responsable del ambiente.

## Operaciones incluidas

Todas las rutas de la tabla llevan el prefijo `/messageucolab/v1`.

| Método | Ruta | Uso | Autenticación en el contrato |
|---|---|---|---|
| GET | `/session/user` | Consultar referencia local del usuario autenticado | Sesión BFF |
| GET | `/organizations` | Listar mis organizaciones | Sesión BFF |
| POST | `/organizations` | Crear organización personal | Sesión BFF |
| GET | `/organizations/{organizationId}/applications` | Listar aplicaciones de una organización propia | Sesión BFF |
| POST | `/application` | Crear aplicación y sus ambientes Develop, Testing y Production | Sesión BFF |
| POST | `/application/functionality` | Crear funcionalidad | Sesión BFF |
| POST | `/messages` | Crear el mensaje en uno o varios ambientes de la aplicación | Sesión BFF |
| GET | `/messages` | Buscar por código o listar con paginación y filtro `filterEnvironments` | Sesión BFF |
| POST | `/messages/{messageCode}/translations` | Traducir sin guardar la traducción | Sesión BFF |
| GET | `/session/applications` | Listar aplicaciones de las organizaciones propias | Sesión BFF |
| GET | `/catalog/applications` | Listar aplicaciones propias | Sesión BFF |
| GET | `/catalog/applications/{applicationId}/environments` | Listar ambientes | Sesión BFF |
| GET | `/catalog/applications/{applicationId}/functionalities` | Listar funcionalidades | Sesión BFF |
| GET | `/catalog/message-types` | Listar tipos de mensaje | Sesión BFF |
| GET | `/catalog/message-categories` | Listar categorías | Sesión BFF |
| GET | `/catalog/message-states` | Listar estados | Sesión BFF |
| GET | `/catalog/message-environment-states` | Listar estados por ambiente | Sesión BFF |

## Headers y contexto

Para seguir los ejemplos JSON:

```http
Accept: application/json
Content-Type: application/json
```

`Content-Type` se necesita en las solicitudes con cuerpo.

Las operaciones usan la cookie de sesión BFF emitida por seguridad:

```http
Cookie: SECURITY_BASELINE_SESSION=<sesion>
```

El navegador envía la cookie con `credentials: 'include'`. Para mensajes también
envía `X-Application-Id`; el backend valida que la aplicación pertenezca a una
organización propia. El ambiente es explícito cuando la operación lo necesita.

Las validaciones y filtros de autorización incluyen:

- Crear una organización asigna su propietario desde la identidad validada del BFF, no desde un correo del formulario.
- Crear una aplicación requiere una organización propia; el UUID lo genera el servidor.
- Crear una funcionalidad requiere una aplicación de una organización propia.
- El catálogo de aplicaciones devuelve solo las propias; sin organizaciones propias devuelve una lista vacía.
- Los catálogos de ambientes y funcionalidades rechazan aplicaciones ajenas al usuario.
- El PEP mantiene los permisos de negocio por ruta. No existen roles, permisos ni contextos persistidos localmente.

Una sesión ausente o inválida produce `401`.
Los UUID de los ejemplos son ilustrativos y deben sustituirse por identificadores reales.

## Entradas de creación

| Operación | Campos obligatorios |
|---|---|
| Crear organización | `name` |
| Crear aplicación | `name`, `organizationId`, `languageId`, `stateId` |
| Crear funcionalidad | `name`, `applicationId`, `stateId` |
| Crear mensaje | `code`, `title`, `content`, `typeId`, `categoryId`, `statusId`, `functionalityId`, `environmentIds` |
| Traducir mensaje | `targetLanguage` (`sourceLanguage` es opcional y su valor predeterminado es `auto`) |

El nombre de aplicación o funcionalidad admite hasta 50 caracteres. Para crear mensajes:

- `code` es un sufijo de hasta 10 caracteres alfanuméricos o espacios. Ejemplo: `WELCOME`.
  El servidor construye `MSG_WELCOME` y normaliza espacios a guiones bajos.
- `title` tiene entre 10 y 50 caracteres.
- `content` tiene entre 10 y 100 caracteres.
- Los identificadores referenciados deben existir; la funcionalidad debe pertenecer a la aplicación autorizada.
- Para **crear** un mensaje, la funcionalidad además debe estar activa. Si está inactiva o no puede
  resolverse su estado, el backend rechaza la creación con `422` y el mensaje de catálogo `FUN_217`.
  Ocultarla en el selector no sustituye esta validación del servidor. Esta regla no oculta mensajes
  históricos ni cambia por sí misma el comportamiento de edición de mensajes existentes.
- `environmentIds` lista los ambientes donde se crea el mismo mensaje (uno, dos o todos). Deben
  existir, pertenecer a la aplicación y no repetirse. El contenido es único: editarlo cambia todos
  sus ambientes. En la edición, `environmentIds` es opcional y, si se envía, reemplaza los ambientes.
- Eliminar un mensaje lo borra de todos sus ambientes.
- El listado devuelve en `environments` los ambientes de cada mensaje; `filterEnvironments` (ids
  separados por coma) limita el resultado. Sin mensajes devuelve una página vacía.

La creación responde con HTTP `200` y una confirmación, por ejemplo:

```json
{
  "data": ["Mensaje creado exitosamente"],
  "errors": []
}
```

No se devuelve el UUID del recurso en esa confirmación.

## Consulta de mensajes

### Por código

```http
GET /messageucolab/v1/messages?code=MSG_WELCOME
```

`data` contiene un arreglo con el mensaje. Cuando se envía `code`, no se aplican la paginación
ni los filtros del listado. Para combinar búsqueda por código y filtros, usar `filterCode` sin `code`.

### Paginada

```http
GET /messageucolab/v1/messages?page=1&size=50&sort=ASC&columnSort=id
```

- `page`: comienza en 1; predeterminado 1.
- `size`: entre 1 y 100; predeterminado 50.
- `sort`: `ASC` o `DESC`; predeterminado `ASC`.
- `columnSort`: campo de ordenamiento; predeterminado `id`.
- `filterCode`: coincidencia parcial por código, sin distinguir mayúsculas.
- `filterStatusId`: UUID de `/catalog/message-states`, con coincidencia exacta.
- `filterFunctionalityId`: UUID de `/catalog/applications/{applicationId}/functionalities`, con coincidencia exacta.
- `filterEnvironments`: UUID de ambientes separados por coma; vacío incluye todos los de la aplicación.

Los filtros se combinan con **AND** en el servidor, antes de paginar y contar resultados. Los filtros
de texto legados `filterStatus` y `filterFunctionality` siguen disponibles, pero para selectores se
recomiendan los identificadores exactos. Un UUID de filtro mal formado produce `422`.
Los filtros no amplían el acceso: los resultados siempre pertenecen a la aplicación autorizada.

El listado y la consulta por código incluyen `status`, `statusId`, `functionalityId`, `createdAt`,
`updatedAt` y `environments`. `status` es el estado del mensaje, no el de su vínculo con un ambiente.
`updatedAt` procede de `message.updated_at` y se transmite en UTC; no es la fecha de la consulta ni
la de la proyección. Si un documento histórico carece del dato, se devuelve `null`, sin sustituirlo
por una fecha inventada. La proyección y los eventos SSE mantienen la consistencia eventual existente.

Ejemplo (reemplazar los UUID por valores reales):

```http
GET /messageucolab/v1/messages?page=1&size=20&filterStatusId=123e4567-e89b-12d3-a456-426614174060&filterFunctionalityId=123e4567-e89b-12d3-a456-426614174070
```

La respuesta tiene un objeto de página dentro del arreglo exterior `data`:

```json
{
  "data": [
    {
      "data": [],
      "page": 1,
      "size": 50,
      "totalItems": 0,
      "totalPages": 0
    }
  ],
  "errors": []
}
```

## Traducción

```http
POST /messageucolab/v1/messages/MSG_WELCOME/translations
Content-Type: application/json
Accept: application/json
```

```json
{
  "sourceLanguage": "auto",
  "targetLanguage": "en"
}
```

La respuesta incluye textos originales y traducidos, idiomas, proveedor, modelo,
tiempo de traducción en milisegundos y `dynamicTranslation`. La traducción no se guarda.
El modelo y proveedor concretos dependen de la configuración del ambiente.

## Respuestas y errores

Las respuestas exitosas se envuelven en `data` y `errors`, con `errors: []`.
Los catálogos devuelven elementos `{ "id": "UUID", "name": "nombre" }`.

El catálogo de funcionalidades amplía ese formato, sin rutas nuevas:

```http
GET /messageucolab/v1/catalog/applications/{applicationId}/functionalities
```

Cada elemento conserva `id` y `name` e incluye `stateId`, `state` (nombre del estado) y `active`
(booleano calculado por el backend). La consulta incluye activas e inactivas para Configuración y
filtros de mensajes existentes. El selector de **Crear mensaje** debe usar solo `active: true`.
Cuando falta la referencia de estado o no puede resolverse, `stateId` puede ser `null`, `state`
queda vacío y `active` es `false`; nunca se interpreta un estado ausente como activo.
Una funcionalidad creada como inactiva desde el modal integrado se conserva en Configuración,
pero no se añade ni se selecciona en Crear mensaje; el resto del formulario se mantiene.

El estado se obtiene de `functionality.state_id` y `functionality_state.name`, no del estado de un
mensaje. No hay que registrar permisos ni endpoints nuevos en seguridad. Al desplegar, actualizar
el catálogo Redis con la entrada `FUN_217` del seed; no es necesario modificar ni recrear los datos
de funcionalidades en SurrealDB.

Los errores gestionados por la aplicación tienen esta estructura:

```json
{
  "data": [],
  "errors": [
    {
      "code": "UNAUTHORIZED",
      "message": "Credencial ausente o invalida."
    }
  ],
  "timestamp": "2026-10-06T12:00:00.000Z",
  "path": "/messageucolab/v1/messages"
}
```

El código y mensaje concreto dependen de la regla que falla. Los errores generados
directamente por Spring o un proxy pueden tener otra estructura.

| Estado | Significado general |
|---|---|
| 400 | Cuerpo o solicitud inválidos |
| 401 | Credencial ausente o inválida |
| 403 | Sin autorización o fuera del contexto permitido |
| 404 | Recurso no encontrado |
| 406 | Formato de respuesta no aceptado |
| 409 | Conflicto de duplicidad o jerarquía |
| 422 | Incumplimiento de validaciones de dominio |
| 500 | Error interno |

El OpenAPI especifica los estados documentados para cada operación.

Además de JSON, administración, mensajes y catálogos anuncian YAML, XML, texto plano
y HTML mediante `Accept`. Los endpoints de contexto anuncian JSON. Para integrar
el equipo con una estructura predecible, los ejemplos de esta guía usan JSON.
