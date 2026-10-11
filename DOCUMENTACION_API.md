# Contract First — API de MessageUcoLab

La referencia detallada de entradas, salidas, ejemplos y errores está en
[`infrastructure/src/main/resources/static/openapi.yaml`](infrastructure/src/main/resources/static/openapi.yaml).
El contrato está definido en OpenAPI 3.0.3 y se puede compartir directamente e importar en Swagger Editor o Postman.

## Cómo consultar y compartir

- Compartir el archivo `openapi.yaml` junto con esta guía.
- Para consultarlo sin ejecutar la aplicación, importar el YAML en [Swagger Editor](https://editor.swagger.io/).
- Con la aplicación en ejecución, abrir `/swagger-ui.html` en la URL del despliegue.
- El mismo archivo está publicado en `/openapi.yaml`.
- La URL base definida en el contrato es `http://localhost:8000`.

Esta documentación cubre **15 operaciones HTTP**. La creación de credenciales está fuera de su alcance.
Las credenciales para consumir las operaciones protegidas se obtienen del responsable del ambiente.

## Operaciones incluidas

Todas las rutas de la tabla llevan el prefijo `/messageucolab/v1`.

| Método | Ruta | Uso | Autenticación en el contrato |
|---|---|---|---|
| POST | `/application` | Crear aplicación y sus ambientes Develop, Testing y Production | Bearer |
| POST | `/application/functionality` | Crear funcionalidad | Bearer |
| POST | `/messages` | Crear mensaje en el ambiente autorizado | Bearer |
| GET | `/messages` | Buscar por código o listar con paginación | Bearer |
| POST | `/messages/{messageCode}/translations` | Traducir sin guardar la traducción | Bearer |
| GET | `/me/contexts` | Listar contextos disponibles para la identidad | Bearer |
| GET | `/me/context` | Consultar contexto activo | Bearer |
| PUT | `/me/context` | Seleccionar contexto activo | Bearer |
| GET | `/catalog/applications` | Listar aplicaciones | Bearer |
| GET | `/catalog/applications/{applicationId}/environments` | Listar ambientes | Bearer |
| GET | `/catalog/applications/{applicationId}/functionalities` | Listar funcionalidades | Bearer |
| GET | `/catalog/message-types` | Listar tipos de mensaje | Pública |
| GET | `/catalog/message-categories` | Listar categorías | Pública |
| GET | `/catalog/message-states` | Listar estados | Pública |
| GET | `/catalog/message-environment-states` | Listar estados por ambiente | Pública |

## Headers y contexto

Para seguir los ejemplos JSON:

```http
Accept: application/json
Content-Type: application/json
```

`Content-Type` se necesita en las solicitudes con cuerpo.

Las operaciones protegidas del contrato usan autenticación Bearer:

```http
Authorization: Bearer <credencial-de-identidad>
```

Con Bearer, el servidor utiliza la identidad, sus permisos y su contexto activo.
En Swagger UI, pulsar **Authorize** e introducir únicamente el valor de la credencial,
sin el prefijo `Bearer`; Swagger agrega ese prefijo al header `Authorization`.

Las validaciones y filtros de autorización incluyen:

- Crear una aplicación requiere que la organización coincida con la del contexto activo
  y el permiso `APPLICATION_CREATE`.
- Crear una funcionalidad requiere que la aplicación coincida con la del contexto activo
  y el permiso `FUNCTIONALITY_CREATE`.
- El catálogo de aplicaciones se filtra por `CONTEXT_SELECT`.
- Los catálogos de ambientes y funcionalidades verifican la aplicación del contexto activo.

Un Bearer inválido puede producir `401` incluso cuando la operación admite acceso público.
Los UUID de los ejemplos son ilustrativos y deben sustituirse por identificadores reales.

## Entradas de creación

| Operación | Campos obligatorios |
|---|---|
| Crear aplicación | `name`, `organizationId`, `languageId`, `stateId` |
| Crear funcionalidad | `name`, `applicationId`, `stateId` |
| Crear mensaje | `code`, `title`, `content`, `typeId`, `categoryId`, `statusId`, `functionalityId` |
| Seleccionar contexto | `organizationId`, `applicationId`, `environmentId` |
| Traducir mensaje | `targetLanguage` (`sourceLanguage` es opcional y su valor predeterminado es `auto`) |

El nombre de aplicación o funcionalidad admite hasta 50 caracteres. Para crear mensajes:

- `code` es un sufijo de hasta 10 caracteres alfanuméricos o espacios. Ejemplo: `WELCOME`.
  El servidor construye `MSG_WELCOME` y normaliza espacios a guiones bajos.
- `title` tiene entre 10 y 50 caracteres.
- `content` tiene entre 10 y 100 caracteres.
- Los identificadores referenciados deben existir; la funcionalidad debe pertenecer a la aplicación autorizada.

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

`data` contiene directamente el mensaje. Cuando se envía `code`, los parámetros de paginación no se usan.

### Paginada

```http
GET /messageucolab/v1/messages?page=1&size=50&sort=ASC&columnSort=id
```

- `page`: comienza en 1; predeterminado 1.
- `size`: entre 1 y 100; predeterminado 50.
- `sort`: `ASC` o `DESC`; predeterminado `ASC`.
- `columnSort`: campo de ordenamiento; predeterminado `id`.

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
