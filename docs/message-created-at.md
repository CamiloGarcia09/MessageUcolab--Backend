# Fecha de creación en las consultas de mensajes

Las respuestas de `GET /messageucolab/v1/messages?code=MSG_WELCOME` y
`GET /messageucolab/v1/messages?page=1&size=50` incluyen `created_at` en cada
mensaje, en formato ISO-8601 UTC. Corresponde a la fecha de creación del mensaje
original, no a la de su asociación con un ambiente ni a la de su proyección.

## Bases de datos existentes

1. Antes de desplegar, ejecutar
   `deployment/docker/scripts/surreal/backfill-message-created-at.surql`
   en el namespace y database de la aplicación. El script es idempotente y
   únicamente completa las fechas de las proyecciones desde `message`.
2. Invalidar las entradas de la caché de mensajes en Redis que se hayan generado
   con la versión anterior, sin borrar catálogos, tokens ni otras cachés.
3. Desplegar/reiniciar la API con el código actualizado.

No volver a importar `surreal-init.surql` para migrar datos existentes: contiene
operaciones de inicialización de la semilla. Las instalaciones nuevas ya incluyen
la fecha en las proyecciones de `MSG_WELCOME`.

Si no existe la fecha original en una proyección, la respuesta incluye
`"created_at": null`; no se inventa una fecha de creación.
