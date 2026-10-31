# Importación de mensajes

> **Última actualización:** 31 de octubre de 2026
> **Estado:** Documento oficial vigente
> **Fuente de verdad:** contrato OpenAPI y flujo de importación del backend

## Propósito

La importación crea mensajes en lote desde un archivo Excel contextualizado para una aplicación propia. El proceso separa validación y escritura, conserva un borrador temporal y confirma el lote de forma atómica e idempotente.

No actualiza mensajes existentes, no crea funcionalidades y no acepta filas corregidas desde el navegador durante la confirmación.

## Flujo administrativo

1. **Descargar plantilla:** obtiene una plantilla vinculada a la aplicación seleccionada.
2. **Seleccionar archivo y ambientes:** todos los mensajes comparten aplicación y ambientes.
3. **Validar:** procesa el archivo sin crear mensajes y devuelve vista previa y errores.
4. **Corregir:** descarga un reporte, corrige el archivo y vuelve a validar.
5. **Confirmar:** crea el lote completo y persiste un recibo idempotente.
6. **Recuperar:** consulta el recibo si se pierde la respuesta o vence el borrador.

## Formato de la plantilla

La versión oficial es `MESSAGEUCOLAB_IMPORT_V2`. Incluye las hojas `Mensajes`, `Instrucciones` y `Referencias`.

Columnas de `Mensajes`:

| Orden | Columna |
|---:|---|
| 1 | Código |
| 2 | Funcionalidad |
| 3 | Tipo |
| 4 | Categoría |
| 5 | Estado |
| 6 | Título |
| 7 | Contenido |

La primera fila de `Instrucciones` está oculta y conserva versión y UUID de aplicación. Esos datos validan el contexto de la plantilla, pero no sustituyen la sesión ni autorizan la operación.

Límites:

- Archivo `.xlsx` de hasta 5 MiB.
- Máximo 500 mensajes en las filas 2 a 501.
- Máximo 20 MiB expandidos y 2.000 entradas ZIP.
- Sin macros, fórmulas, enlaces externos, objetos incrustados o fuentes externas.
- Sin columnas adicionales con información.

## Referencias

Los desplegables muestran nombres y la API exige coincidencia exacta y unívoca. No acepta UUID ni valores con formato `Nombre [UUID]`.

Las funcionalidades se resuelven exclusivamente dentro de la aplicación autorizada y deben estar activas. Los UUID resueltos se conservan internamente y se vuelven a validar al confirmar.

## Contrato HTTP

Todas las rutas usan `/messageucolab/v1/messages/imports` y requieren `X-Application-Id`, sesión BFF, permiso PEP y propiedad de la aplicación.

| Método | Ruta | Resultado |
|---|---|---|
| `GET` | `/template` | Plantilla `.xlsx` |
| `POST` | `/validate?environmentIds=id1,id2` | Vista previa sin escrituras |
| `GET` | `/{importId}` | Borrador y errores actuales |
| `GET` | `/{importId}/errors` | Reporte `.xlsx` |
| `GET` | `/{importId}/result` | Recibo o `data: null` |
| `POST` | `/{importId}/confirm` | Confirmación atómica |

Confirmación:

```json
{
  "productionConfirmed": false
}
```

La presencia del ambiente `Produccion` exige `productionConfirmed: true`. La confirmación no sustituye permisos ni propiedad.

## Validaciones

La importación reutiliza las reglas de creación individual:

- Código obligatorio, formato y longitud.
- Título de 10 a 50 caracteres.
- Contenido de 10 a 100 caracteres.
- Tipo, categoría y estado existentes.
- Funcionalidad activa y perteneciente a la aplicación.
- Ambientes existentes, sin repetidos y pertenecientes a la aplicación.
- Código único dentro de la aplicación y sin duplicados internos en el archivo.

Una validación puede responder `200` con errores de fila. La confirmación rechaza cualquier borrador con errores mediante `422`.

## Borrador, transacción e idempotencia

El borrador vive 30 minutos en Redis bajo un alcance de aplicación e importación. No contiene autorización independiente y se comprueba la propiedad en cada petición.

La confirmación ejecuta una única transacción SurrealDB que crea:

- Los mensajes.
- Sus relaciones con ambientes.
- El recibo `message_import_receipt:<importId>`.

Se usa `CREATE`, no `UPSERT`. Un conflicto aborta todo el lote. El mismo `importId` devuelve el recibo original si la transacción ya se confirmó.

Una respuesta `500` o una desconexión no demuestran que no hubo escritura. El cliente debe consultar `/{importId}/result` o repetir la confirmación con el mismo identificador; no debe crear un lote nuevo para recuperar un resultado incierto.

## Seguridad del Excel

- Los valores de salida se escriben como texto para evitar inyección de fórmulas.
- Los errores de infraestructura no se presentan como errores de una fila.
- El navegador no guarda filas, tokens ni credenciales en almacenamiento local.
- Las descargas usan `Cache-Control: no-store`.
- El frontend nunca repite una mutación automáticamente al recuperar sesión.

## Activación del esquema

Las instalaciones nuevas reciben la tabla de recibos desde `surreal-init.surql`.

Para una base existente, el único esquema autorizado para esta función es:

```text
deployment/docker/scripts/surreal/message-import-schema.surql
```

No se debe ejecutar `surreal-init.surql` como migración. El servicio Compose `surrealdb-import-schema` está bajo el perfil de mantenimiento y solo incorpora el esquema aditivo.

Los textos de importación se cargan desde `CatalogMessageInit.sh`. No se debe vaciar Redis ni reimportar todos los datos como atajo.

## Limitaciones verificables

- Las pruebas unitarias con SDK simulado no certifican atomicidad en una instancia real.
- Un build correcto no valida sesión BFF, PEP, Redis o SurrealDB reales.
- La actualización documental del 31 de octubre de 2026 no ejecutó pruebas ni migraciones.

## Documentos relacionados

- [Contrato de la API](contrato-api.md)
- [Arquitectura](arquitectura.md)
- [Despliegue](despliegue.md)
