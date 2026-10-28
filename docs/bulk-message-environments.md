# Edición masiva de ambientes

En el listado del minifront, seleccionar uno o varios mensajes y usar **Editar
ambientes** en el pie de la tabla. La selección también admite otras páginas y
todos los resultados filtrados. No se añade un banner encima del listado.

El diálogo permite elegir uno, varios o todos los ambientes de la aplicación.
No precarga los filtros del listado como destinos ni elige un ambiente por
defecto. **Aplicar ambientes** reemplaza los ambientes de cada mensaje por la
selección común. Los ambientes no seleccionados se retiran. No permite editar
código, título, contenido, tipo, categoría, estado ni funcionalidad.

## Contrato

Se amplía el cuerpo de la ruta existente, sin añadir endpoints ni permisos:

```http
PUT /messageucolab/v1/messages/{messageCode}
Content-Type: application/json
X-Application-Id: <aplicación propia>
```

```json
{
  "environmentsOnly": true,
  "environmentIds": ["7b3e5d91-a2c8-46f0-9d14-5e7a1b6c3f82"]
}
```

Con `environmentsOnly: true`, el backend valida propiedad del mensaje y ambientes
de la aplicación mediante la sesión BFF y las reglas Composite existentes. Los
otros campos se ignoran: el adaptador solo reemplaza relaciones de ambientes y
actualiza `updated_at`, sin reenviar ni reescribir contenido o clasificación.
Conserva los identificadores y estados de las relaciones que siguen elegidas,
crea las nuevas con el estado activo del catálogo e invalida la caché del mensaje.
No cambian los permisos de ruta del PEP ni la integración con seguridad.

Omitir la bandera o enviar `false` mantiene la edición completa anterior. En
esa modalidad, omitir `environmentIds` conserva los ambientes actuales.

## Resultados parciales y despliegue

El frontend captura códigos, aplicación y destinos al confirmar, y envía una
solicitud secuencial por mensaje. No existe atomicidad entre distintos mensajes.
Los éxitos dejan de estar seleccionados; los pendientes o fallidos se conservan,
se muestra el resultado y se vuelve a cargar la primera página con sus totales.
Los mensajes pueden dejar de aparecer si ya no cumplen el filtro de ambientes.

No se reintentan mutaciones automáticamente. Errores de red, sesión, permisos,
rate limit o servidor detienen los siguientes envíos. Cambiar contexto/filtros o
desmontar el listado también detiene nuevos envíos; una petición ya enviada
puede completar su escritura en la aplicación original.

**Desplegar primero el backend actualizado y después el frontend.** La modalidad
limitada necesita este cambio en backend; no hay migración de datos, nuevos
secretos ni cambios de despliegue de seguridad. Este cambio no ejecuta ninguna
edición de mensajes reales por sí mismo.
