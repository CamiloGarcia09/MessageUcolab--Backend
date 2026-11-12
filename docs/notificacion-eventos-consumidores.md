# Notificación de cambios a componentes consumidores: seguimiento de implementación

Fecha: 7 de noviembre de 2026

Documento de trabajo. El contrato y la arquitectura definitivos están en [contrato-api.md](contrato-api.md#avisos-de-cambios-para-aplicaciones-consumidoras), [arquitectura.md](arquitectura.md#avisos-de-cambios-a-componentes-consumidores) y [consumo-mensajes.md](consumo-mensajes.md#avisos-de-cambios). Este archivo solo registra el estado, las decisiones y lo pendiente.

## Objetivo

Cuando un mensaje se crea, se edita o se elimina, MessageUcoLab avisa a los componentes que lo consumen. El aviso es una señal sin contenido; el consumidor vuelve a consultar el mensaje. Un consumidor solo recibe los avisos del ambiente que consume.

## Estado

| Fase | Contenido | Estado |
|---|---|---|
| 1 | Publicar avisos a Pulsar (un topic por ambiente) | Implementada |
| 2 | Pruebas unitarias | Implementada (2.067 pruebas en verde en `utils`, `core` e `infrastructure`) |
| 2b | Prueba de integración contra un Pulsar real | **Omitida por decisión**; verificación manual (ver abajo) |
| 3 | Entrega confiable (outbox con `broker_status`) | Pendiente |
| 4 | Soporte para el consumidor (listener en el starter Java, webhook opcional) | Pendiente |
| 5 | Documentación oficial | Implementada |

No se han creado commits.

## Qué se implementó

| Archivo | Función |
|---|---|
| `infrastructure/.../secondaryadapters/event/MessageChangedEvent.java` | Contrato v1 del aviso, independiente de `MessageEvent` (que sigue alimentando el SSE). |
| `infrastructure/.../secondaryadapters/event/PulsarMessageEventPublisher.java` | Publica en `<prefijo>-<environmentId>`. Productor por topic, perezoso y reintentable. No lanza excepciones. |
| `infrastructure/.../secondaryadapters/event/CompositeMessageEventPublisher.java` | Entrega a SSE y a Pulsar de forma independiente. |
| `infrastructure/.../config/MessageEventPublisherConfig.java` | Registra el compuesto como bean `@Primary`. El proyector no cambió. |
| `PulsarProperties`, `application.properties` | `pulsar.events-topic-prefix` y `pulsar.events-enabled`, con valores por defecto. |
| `MessageCatalogCodeEnum`, `CatalogMessageInit.sh` | Textos de log `TCH_107` a `TCH_109`. |

## Decisiones tomadas

1. **Un topic por ambiente.** Requisito: quien consume Develop no debe enterarse de cambios en Produccion. Filtrar dentro de un topic compartido no aísla; el enrutamiento se hace en el broker.
2. **Se publican solo los eventos por ambiente** que ya emite el proyector. Los eventos por aplicación mezclan ambientes y se excluyen. No hizo falta modificar el proyector.
3. **`MessageEvent` no se modificó.** Es también el payload del SSE; el contrato del broker evoluciona por separado.
4. **El aviso no lleva contenido.** Evita exponer mensajes por el topic y obliga al consumidor a pasar por las consultas, que verifican disponibilidad.
5. **Mejor esfuerzo por ahora.** Un fallo de Pulsar se registra y el aviso se pierde; no interrumpe proyección ni SSE.
6. **Defecto encontrado por las pruebas y corregido:** un futuro de creación de productor ya fallido dejaba el productor cacheado y sin reintento, por una modificación recursiva del mapa dentro de `computeIfAbsent`.

7. **Defecto previo del proyector, encontrado en la prueba con un consumidor real y corregido:** `domain_events.aggregate_id` llega como ``message:`uuid` `` (con comillas invertidas) mientras `message_environment_readmodel.message_id` se guarda como `message:uuid`. Tres consultas del proyector (buscar los ambientes del mensaje, borrar sus filas de proyección y propagarle una edición) comparaban el valor crudo y no encontraban filas. Consecuencias: nunca se emitían los eventos por ambiente (de los que depende Pulsar) y la proyección por ambiente no recibía las ediciones de título y contenido. Se normaliza el id con `readModelMessageId(...)` y hay pruebas de regresión con ids entre comillas invertidas. Los tests anteriores usaban ids sin comillas, por eso no lo detectaban. Tras desplegar, la proyección por ambiente se corrige con la siguiente edición de cada mensaje.

8. **El borrado de un mensaje no avisaba a su ambiente, corregido tras la prueba con el consumidor.** Al borrar un mensaje se generan `MESSAGE_ENVIRONMENT_DELETED` y luego `MESSAGE_DELETED` (con una diferencia de un milisegundo). El proyector procesa primero la relación: retira la fila del ambiente y busca el mensaje en la fuente para avisar, pero el mensaje ya no existe y no emitía nada; después `MESSAGE_DELETED` buscaba los ambientes del mensaje y ya no encontraba ninguno. Ahora, cuando al eliminar la relación el mensaje ya no existe en la fuente, se toma su código de la proyección del mensaje (que aún existe) y se publica `MessageDeleted` para ese ambiente. Si solo se quita la asociación de un mensaje que sigue existiendo, el aviso sigue siendo `MessageUpdated`. No hay duplicados en ninguno de los dos órdenes de procesamiento.
9. **Reconexión del lado del consumidor.** Un proxy puede dejar abierta la conexión del navegador cuando el backend consumidor se reinicia, y el navegador no reintenta. El canal SSE de la pantalla de ejemplo envía un latido real cada 10 s y la pantalla reconecta si pasan 25 s sin señal. Cualquier consumidor que use SSE o conexiones de larga duración debe vigilar su propio silencio.

## Verificación manual contra Pulsar

Pasos sugeridos con el stack de `deployment/docker` levantado y los textos del catálogo cargados:

1. Obtener los identificadores de dos ambientes de una aplicación (por ejemplo Develop y Produccion).
2. Suscribirse al topic de Develop desde el contenedor de Pulsar:

   ```bash
   docker exec -it pulsar bin/pulsar-client consume -s prueba -n 0 \
     persistent://public/default/message-events-<idAmbienteDevelop>
   ```

   Si el contenedor o el prefijo son otros, ajustar el nombre y `pulsar.events-topic-prefix`.
3. En otra terminal, suscribirse igual al topic de Produccion.
4. Crear, editar y borrar un mensaje habilitado solo en Develop. Esperar unos segundos (intervalo del proyector).
5. Comprobar:
   - En el topic de Develop llegan `MessageCreated`, `MessageUpdated` y `MessageDeleted` con el `messageCode` correcto.
   - El topic de Produccion **no recibe nada**.
6. Habilitar el mensaje también en Produccion y comprobar `MessageUpdated` en el topic de Produccion. Quitarlo de Develop y comprobar que solo Develop recibe el aviso de esa asociación.
7. Inactivar una funcionalidad con mensajes y comprobar `MessagesChanged` con `messageCode` vacío.
8. Detener Pulsar, editar un mensaje y comprobar que la API y el SSE administrativo siguen funcionando y que el log registra `TCH_107`/`TCH_108`. Reiniciar Pulsar y comprobar que el siguiente cambio vuelve a publicarse.

## Pendiente y riesgos

- **Permisos por topic en Pulsar.** Sin ellos, un consumidor podría suscribirse al topic de otro ambiente. Es configuración del broker y debe coordinarla quien lo opere.
- **Recargar el catálogo de textos** (`CatalogMessageInit.sh`) en cada entorno para que existan `TCH_107` a `TCH_109`.
- **Fase 3 (outbox).** Marcar `broker_status` en `domain_events`, reintentar, dead-letter y métricas. Necesaria si algún consumidor no tolera avisos perdidos.
- **Fase 4.** Listener e invalidador de caché en el starter Java; webhook solo si algún consumidor no puede usar Pulsar.
- **Varias réplicas.** El procesador de proyecciones es un `@Scheduled`; confirmar que con varias instancias no publica el mismo aviso más de una vez, o asegurar que el consumidor deduplica por `eventId` (ya documentado).
- **Borrado en el contrato público.** La documentación indica que la eliminación de mensajes no figura en el OpenAPI oficial; confirmar si `MessageDeleted` forma parte del contrato estable.
- **Pruebas de integración automatizadas** con Testcontainers, si más adelante se acepta la dependencia.
