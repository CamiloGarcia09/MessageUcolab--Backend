# Contrato de la API de MessageUcoLab

> **Última actualización:** 8 de noviembre de 2026
> **Estado:** Documento oficial vigente
> **Fuente de verdad:** `infrastructure/src/main/resources/static/openapi.yaml`

## Alcance

La especificación OpenAPI 3.0.3 define las operaciones públicas y protegidas, parámetros, cuerpos, respuestas y esquemas de MessageUcoLab. Incluye el cambio de estado de funcionalidades descrito en este documento.

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

Las rutas de consumo aceptan la sesión BFF o el Bearer de una identidad de servicio autorizada en el PEP; ninguna ruta de mensajes queda fuera del PEP. Con identidad de servicio no se envían `X-Application-Id` ni `X-Environment-Id`: la aplicación y el ambiente salen del `client_id` del token, asociado por el propietario a un ambiente.

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
| `DELETE` | `/application/{applicationId}` | Eliminar una aplicación propia y todos sus datos de gestión |
| `POST` | `/application/functionality` | Crear funcionalidad |
| `PATCH` | `/application/functionality/{functionalityId}/state` | Activar o inactivar funcionalidad y desactivar sus mensajes |
| `GET` | `/application/{applicationId}/service-identities` | Listar las identidades de servicio asociadas a los ambientes |
| `POST` | `/application/{applicationId}/service-identities` | Asociar el `client_id` de una identidad a un ambiente |
| `DELETE` | `/application/{applicationId}/service-identities/{clientId}` | Retirar una identidad de servicio |
| `GET` | `/messages` | Consultar por código o listar mensajes |
| `GET` | `/messages/events` | Recibir notificaciones SSE de la aplicación autorizada |
| `POST` | `/messages` | Crear mensaje |
| `PUT` | `/messages/{messageCode}` | Editar mensaje o solo sus ambientes |
| `POST` | `/messages/{messageCode}/translations` | Traducir un mensaje administrativo |

### Consumo

| Método | Ruta | Operación |
|---|---|---|
| `GET` | `/messages/consumer` | Listar los mensajes disponibles del ambiente (sin `code`) o consultar uno (`?code=`) |
| `POST` | `/messages/consumer/translations` | Traducir un mensaje disponible |

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

## Eliminación de aplicaciones

```http
DELETE /messageucolab/v1/application/{applicationId}
Cookie: SECURITY_BASELINE_SESSION=<sesion>
Accept: application/json
```

- Sin cuerpo ni cabeceras de selección de aplicación o ambiente: se elimina la aplicación identificada en la ruta, nunca una selección implícita del navegador.
- Exige permiso PEP y propiedad actual del usuario BFF. La comprobación se repite dentro de la transacción; una aplicación ajena o inexistente devuelve `403` sin distinguir ambos casos. Un UUID inválido devuelve `422`.
- Una transacción SurrealDB borra la aplicación, funcionalidades, mensajes, relaciones mensaje/ambiente, ambientes, recibos de importación y proyecciones documentales asociadas. No se tocan la organización, otras aplicaciones ni los catálogos compartidos. Los eventos y documentos de auditoría se conservan deliberadamente.
- Las traducciones actuales son temporales en Redis, no registros de traducción en SurrealDB. Tras el commit se limpian sus claves administrativas y de consumo, los mensajes cacheados por UUID y los borradores de importación de esa aplicación. Se usa SCAN acotado, sin vaciar Redis ni invalidar por códigos compartidos con otras aplicaciones.
- Redis no forma parte de la transacción. Una caída durante la limpieza se registra como `TCH_108`, sin presentar como fallido un borrado ya confirmado. Los restos temporales caducan según sus TTL y la propiedad/existencia se valida antes de acceder a caché. Una respuesta perdida tampoco prueba que el borrado haya fallado: volver a consultar el listado antes de reintentar, nunca repetir automáticamente.
- Respuesta `200`: envelope `data: ["La aplicación y sus funcionalidades, mensajes y ambientes se eliminaron correctamente."], errors: []`.
- `MessageUcolab--Frontend` incorpora el botón en «Mis aplicaciones», con nombre exacto y confirmación explícita del impacto en todos los ambientes. Tras confirmar la respuesta quita la aplicación de los listados y limpia el contexto activo si corresponde. El minifront de mensajes no administra organizaciones/aplicaciones y no añade otra operación de borrado.

### Activación del borrado

El responsable de seguridad debe registrar y autorizar **DELETE `/messageucolab/v1/application/{applicationId}`** en el PEP existente. Los permisos POST o PATCH no sustituyen este permiso. No se debe excluir la ruta del PEP ni modificar seguridad desde este repositorio.

Desplegar backend y frontend y cargar únicamente los cuatro textos nuevos desde `deployment/docker`:

```bash
doppler run --project messageucolab --config dev -- docker compose run --rm --no-deps redis-init --application-deletion-only
```

No requiere migración de esquema ni ejecución del seed completo de SurrealDB. Desplegar no elimina aplicaciones: el borrado solo ocurre al invocar expresamente el endpoint. El CORS administrativo ya admite DELETE.

## Cambio de estado de funcionalidades

```http
PATCH /messageucolab/v1/application/functionality/{functionalityId}/state
Content-Type: application/json
Cookie: SECURITY_BASELINE_SESSION=<sesion>
```

```json
{
  "applicationId": "<uuid-aplicacion-propia>",
  "stateId": "<uuid-estado-funcionalidad>"
}
```

- El backend valida propiedad mediante la sesión BFF, nunca mediante correo o propietario enviados por el navegador. La funcionalidad debe pertenecer a la aplicación indicada.
- `stateId` debe identificar el estado Activo o Inactivo de `functionality_state`; no se admite el UUID de un estado de mensaje.
- Inactivar cambia el estado global de **todos** los mensajes asociados a Inactivo en la misma transacción, sin borrar mensajes, traducciones ni relaciones de ambientes. Afecta su disponibilidad en todos los ambientes.
- Reactivar la funcionalidad **no reactiva mensajes**. Después pueden activarse individualmente; no se permite activar mensajes mientras su funcionalidad esté inactiva.
- La respuesta `200` conserva el envelope `data: ["Estado de funcionalidad actualizado correctamente."], errors: []`. Los UUID inválidos se rechazan con `422`, el acceso a una aplicación ajena con `403` y una funcionalidad inexistente o ajena a la aplicación indicada con `404`.
- Se invalida la caché de los mensajes modificados; los eventos normales de actualización alimentan proyecciones y SSE. El consumo comprueba también el estado actual de la funcionalidad, incluso si existe caché anterior.
- La pantalla de funcionalidades de `MessageUcolab--Frontend` y la sección Configuración de `Minifronts/mensajes-ui` incorporan Activar/Inactivar y confirmación explícita del impacto, usando la misma ruta PATCH y las mismas reglas del backend.

### Activación del contrato

El responsable de seguridad debe habilitar el permiso de **PATCH `/messageucolab/v1/application/functionality/{functionalityId}/state`** en el PEP existente. No basta con el permiso POST de creación. Sin ese permiso puede responder `403`; no se debe excluir la ruta de autenticación ni modificar seguridad desde este repositorio.

Actualizar solo los cinco textos nuevos del catálogo, sin vaciar Redis ni recargar entradas ajenas, desde `deployment/docker`:

```bash
doppler run --project messageucolab --config dev -- docker compose run --rm --no-deps redis-init --functionality-states-only
```

No se requiere migración de esquema. El cambio se aplica a una funcionalidad al ejecutar su inactivación; no modifica retrospectivamente datos por desplegar el backend. Desplegar también el frontend actualizado. El CORS de MessageUcoLab admite PATCH; no se modifica el CORS ni el despliegue de seguridad.

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
- El sufijo admite entre 1 y 500 caracteres alfanuméricos o espacios; el código final puede tener hasta 504 caracteres con `MSG_`.
- `title` admite entre 5 y 1000 caracteres, tanto al crear como al editar.
- `content` admite entre 5 y 2000 caracteres, tanto al crear como al editar.
- La funcionalidad debe estar activa y pertenecer a la aplicación.
- Los ambientes no se repiten y deben pertenecer a la aplicación.

### Activación de los nuevos límites

El aumento de longitudes no requiere migrar datos ni modificar el esquema de SurrealDB: los tres campos son de tipo `string` sin restricciones de longitud en la base. Los mensajes existentes siguen siendo válidos.

Desplegar el backend, el frontend y el minifront de mensajes actualizados. Para actualizar únicamente los cinco errores de longitud y las instrucciones de Excel del catálogo Redis existente, ejecutar desde `deployment/docker`:

```bash
doppler run --project messageucolab --config dev -- docker compose run --rm --no-deps redis-init --message-limits-only
```

Este modo actualiza solo `FUN_018`, `FUN_019`, `FUN_020`, `FUN_021`, `FUN_198` e `IMP_012`; no vacía Redis ni recarga otras entradas. Las nuevas plantillas Excel usarán las instrucciones actualizadas. Las plantillas V2 existentes conservan su estructura y sus filas se validan con los nuevos límites al importar y confirmar.

### Edición de ambientes

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

## Actualización administrativa en tiempo real

```http
GET /messageucolab/v1/messages/events
Accept: text/event-stream
X-Application-Id: <uuid-aplicacion-propia>
Cookie: SECURITY_BASELINE_SESSION=<sesion>
```

La ruta conserva autenticación BFF, permiso PEP y comprobación de propiedad. El permiso GET de esta ruta es independiente del PATCH de funcionalidades. No se excluye SSE del PEP ni se modifica seguridad.

- `connected`: confirma apertura del canal; el cliente consulta el listado actual también al reconectar.
- `MessageCreated`: se proyectó la creación de un mensaje; incluye `applicationId` y `messageCode` en la notificación por aplicación, aunque todavía no haya filas por ambiente.
- `MessageUpdated`: se proyectó una edición individual o un cambio de sus asociaciones de ambientes. Incluye aplicación y código en la señal por aplicación; quitar un ambiente no elimina el mensaje completo.
- `MessageDeleted`: se proyectó la eliminación del mensaje completo y de sus proyecciones por ambiente. El código y la aplicación se recuperan del evento de borrado, aunque la fila original ya no exista.
- `MessagesChanged`: notifica un cambio colectivo, actualmente la inactivación de los mensajes de una funcionalidad. Incluye `applicationId`; `environmentId` y `messageCode` están vacíos en la señal por aplicación. Ya no se emite como aviso general de toda edición individual.
- `MessageChanged`: señal individual genérica para un evento de origen no reconocido; no equivale a la invalidación colectiva `MessagesChanged`.
- `heartbeat`: mantiene el canal abierto aproximadamente cada 15 segundos, sin anunciar cambios de datos.
- Se conservan notificaciones por ambiente con el tipo correspondiente. Una acción puede producir varias señales por aplicación y ambientes; no hay garantía de un solo evento por acción. El frontend agrupa las ráfagas y vuelve a consultar el estado actual.
- El campo `applicationId` es aditivo; puede estar vacío en eventos anteriores por ambiente. Un evento con aplicación explícita solo se entrega a suscripciones de esa aplicación autorizada.
- El frontend principal agrupa notificaciones, vuelve a consultar datos y totales del servidor sin recargar la página y resincroniza al reconectar. No se infiere el nuevo estado solo por recibir un evento.
- Los cambios de funcionalidad se guardan primero en la transacción fuente. El listado y su notificación se actualizan mediante el procesador asíncrono de proyecciones: el intervalo configurado de 2 segundos no garantiza una latencia máxima, pues hay cola y tiempo de procesamiento.
- No hay replay duradero ni sincronización SSE entre réplicas. La administración de `Minifronts/mensajes-ui` también escucha este canal, agrupa avisos, resincroniza al conectar o reconectar y conserva la búsqueda, el ambiente y la paginación válida sin reemplazar los formularios que se están llenando. Al cambiar de contexto o desmontarse cierra la conexión y descarta respuestas anteriores.
- El cliente público `createMessageClient` del minifront conserva su caché con TTL. No abre SSE administrativo ni adquiere con este cambio el futuro contrato de sincronización persistente de aplicaciones consumidoras.

La clasificación colectiva se registra en el campo flexible `metadata.notification_type` de los eventos nuevos dentro de la misma transacción de inactivación. No cambia el esquema ni requiere recargar seeds; los eventos antiguos sin esa marca se clasifican por su tipo de origen y no se reetiquetan retrospectivamente. Al desplegar el ajuste puede aparecer `MessageUpdated` al procesar una cascada antigua que aún estuviera pendiente.

Si se pierde la conexión, el frontend lo indica y reintenta con espera progresiva. Si la ruta devuelve `403`, muestra que falta autorización y permite reabrir el canal sin recargar cuando el responsable haya corregido el permiso. Una respuesta HTML `200` no se interpreta como un canal SSE válido.

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

La implementación contiene una operación de eliminación de mensajes que no aparece actualmente en el OpenAPI. Hasta que la especificación se actualice y valide, no debe presentarse como parte estable del contrato externo oficial. El canal SSE sí se documenta en la especificación actual.

## Documentos relacionados

- [Consumo de mensajes](consumo-mensajes.md)
- [Importación de mensajes](importacion-mensajes.md)
- [Espacios personales](espacios-personales.md)
