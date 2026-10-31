# Contrato de la API de MessageUcoLab

> **Última actualización:** 31 de octubre de 2026
> **Estado:** Documento oficial vigente
> **Fuente de verdad:** `infrastructure/src/main/resources/static/openapi.yaml`

## Alcance

La especificación OpenAPI 3.0.3 define las operaciones públicas y protegidas, parámetros, cuerpos, respuestas y esquemas de MessageUcoLab. En la fecha de actualización contiene 28 operaciones HTTP.

Con la aplicación en ejecución:

- Swagger UI: `http://localhost:8085/swagger-ui.html`
- OpenAPI YAML: `http://localhost:8085/openapi.yaml`
- API directa: `http://localhost:8085`
- API mediante Kong: `http://localhost:8000`

Este documento resume el contrato. Cuando exista una diferencia, prevalece el archivo OpenAPI hasta que contrato e implementación vuelvan a alinearse.

## Seguridad y contexto

La seguridad predeterminada es la cookie BFF:

```http
Cookie: SECURITY_BASELINE_SESSION=<sesion>
```

Las operaciones administrativas requieren autorización PEP y propiedad de negocio. Las operaciones de mensajes usan `X-Application-Id`; las que resuelven disponibilidad por ambiente usan también `X-Environment-Id` o reciben `environmentIds` según su contrato.

```http
X-Application-Id: <uuid-aplicacion>
X-Environment-Id: <uuid-ambiente>
```

Las dos rutas bajo `/messageucolab/v1/public` declaran `security: []` y no pasan actualmente por el PEP.

## Operaciones oficiales

Todas las rutas usan el prefijo `/messageucolab/v1`.

### Espacios personales

| Método | Ruta | Operación |
|---|---|---|
| `GET` | `/session/user` | Consultar usuario autenticado |
| `GET` | `/organizations` | Listar organizaciones propias |
| `POST` | `/organizations` | Crear organización personal |
| `GET` | `/organizations/{organizationId}/applications` | Listar aplicaciones propias |
| `GET` | `/session/applications` | Listar aplicaciones de la sesión |

### Administración

| Método | Ruta | Operación |
|---|---|---|
| `POST` | `/application` | Crear aplicación y ambientes |
| `POST` | `/application/functionality` | Crear funcionalidad |
| `GET` | `/messages` | Consultar por código o listar mensajes |
| `POST` | `/messages` | Crear mensaje |
| `PUT` | `/messages/{messageCode}` | Editar mensaje o solo sus ambientes |
| `POST` | `/messages/{messageCode}/translations` | Traducir un mensaje administrativo |

### Consumo

| Método | Ruta | Operación |
|---|---|---|
| `GET` | `/messages/consumer` | Consultar un mensaje disponible con sesión |
| `POST` | `/messages/consumer/translations` | Traducir un mensaje disponible con sesión |
| `GET` | `/public/messages` | Listar mensajes públicos disponibles |
| `GET` | `/public/messages/{code}` | Consultar o traducir un mensaje público |

### Importación

| Método | Ruta | Operación |
|---|---|---|
| `GET` | `/messages/imports/template` | Descargar plantilla Excel |
| `POST` | `/messages/imports/validate` | Validar archivo sin crear mensajes |
| `GET` | `/messages/imports/{importId}` | Consultar borrador |
| `GET` | `/messages/imports/{importId}/errors` | Descargar reporte de errores |
| `GET` | `/messages/imports/{importId}/result` | Consultar recibo confirmado |
| `POST` | `/messages/imports/{importId}/confirm` | Confirmar lote atómico e idempotente |

### Catálogos

| Método | Ruta |
|---|---|
| `GET` | `/catalog/applications` |
| `GET` | `/catalog/applications/{applicationId}/environments` |
| `GET` | `/catalog/applications/{applicationId}/functionalities` |
| `GET` | `/catalog/message-types` |
| `GET` | `/catalog/message-categories` |
| `GET` | `/catalog/message-states` |
| `GET` | `/catalog/message-environment-states` |

## Creación y edición de mensajes

La creación requiere:

```json
{
  "code": "WELCOME",
  "title": "Mensaje de bienvenida",
  "content": "Bienvenido a nuestra aplicación",
  "typeId": "<uuid>",
  "categoryId": "<uuid>",
  "statusId": "<uuid>",
  "functionalityId": "<uuid>",
  "environmentIds": ["<uuid>"]
}
```

Reglas principales:

- `code` es el sufijo; el servidor genera el prefijo `MSG_`.
- El sufijo admite hasta 10 caracteres alfanuméricos o espacios.
- `title` admite entre 10 y 50 caracteres.
- `content` admite entre 10 y 100 caracteres.
- La funcionalidad debe estar activa y pertenecer a la aplicación.
- Los ambientes no se repiten y deben pertenecer a la aplicación.

Para reemplazar exclusivamente los ambientes:

```http
PUT /messageucolab/v1/messages/{messageCode}
X-Application-Id: <uuid-aplicacion>
Content-Type: application/json
```

```json
{
  "environmentsOnly": true,
  "environmentIds": ["<uuid-ambiente>"]
}
```

Esta modalidad no modifica código, título, contenido, clasificación, estado ni funcionalidad. La operación masiva del frontend envía una solicitud por mensaje y puede producir resultados parciales.

## Consulta administrativa

`GET /messages` admite consulta exacta por `code` o listado paginado. Los filtros del listado se combinan con AND antes de contar y paginar.

| Parámetro | Uso |
|---|---|
| `page`, `size` | Página desde 1 y tamaño de 1 a 100 |
| `sort` | `ASC` o `DESC` |
| `columnSort` | Orden por código o actualización |
| `filterCode` | Coincidencia parcial por código |
| `filterTitle` | Coincidencia parcial por título |
| `filterStatusId` | Estado exacto por UUID |
| `filterFunctionalityId` | Funcionalidad exacta por UUID |
| `filterEnvironments` | UUID de ambientes separados por coma |

Las fechas de salida están en UTC y proceden del mensaje original. No representan el momento de la consulta.

## Formatos y errores

Las operaciones administrativas pueden negociar JSON, YAML, XML, texto plano y HTML donde el OpenAPI lo declara. Las integraciones deben preferir JSON.

Los errores gestionados por la aplicación usan normalmente:

```json
{
  "data": [],
  "errors": [
    {
      "code": "<codigo>",
      "message": "<mensaje>"
    }
  ],
  "timestamp": "2026-10-31T00:00:00Z",
  "path": "/messageucolab/v1/messages"
}
```

Los rechazos generados directamente por Spring, Kong o el PEP pueden usar otra estructura. El cliente debe comprobar primero el estado HTTP.

## Desviaciones conocidas del contrato

La implementación contiene operaciones de eliminación de mensajes y SSE que no aparecen actualmente entre las 28 operaciones del OpenAPI. Hasta que la especificación se actualice y valide, no deben presentarse como parte estable del contrato externo oficial.

## Documentos relacionados

- [Consumo de mensajes](consumo-mensajes.md)
- [Importación de mensajes](importacion-mensajes.md)
- [Espacios personales](espacios-personales.md)
