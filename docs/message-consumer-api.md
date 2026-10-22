# Consulta de mensajes para aplicaciones consumidoras

## Petición

```http
GET /messageucolab/v1/messages/resolve?code=MSG_WELCOME
Accept: application/json
X-Application-Id: <UUID de una aplicación de una organización propia>
X-Environment-Id: <UUID de un ambiente de esa aplicación>
```

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
  "title": "Welcome",
  "content": "Welcome to MessageUcoLab"
}
```

Este endpoint devuelve directamente el objeto JSON, sin el envoltorio
`data`/`messages` de las consultas administrativas. Cada objeto contiene
únicamente `code`, `title` y `content`; no incluye `created_at` ni metadatos
administrativos. Los endpoints existentes mantienen sus contratos.

El contenido es el original registrado. Se asume que está escrito en el idioma
base de la aplicación (`application.language_id`); no se detecta ni se traduce
el idioma y no se admite selección de idioma en esta versión. La aplicación
consumidora debe tratar los textos como texto plano y decidir cómo mostrarlos.
Los tokens dentro del contenido se entregan sin sustitución.

## Disponibilidad y aislamiento

Solo se entrega un mensaje si está activo y tiene una asociación activa con el
ambiente autorizado. Además, la aplicación del mensaje debe coincidir con la
del ambiente. El código se resuelve dentro de ese contexto, no globalmente.

Se consulta directamente `message_environment` y sus referencias en SurrealDB,
sin utilizar las proyecciones ni la caché Redis de las consultas completas.
La respuesta tiene `Cache-Control: no-store` para evitar cachés compartidas entre
consumidores y conservar la comprobación de disponibilidad en cada petición.

## Errores

- `400`: falta el parámetro `code`.
- Código vacío: se aplica el validador de códigos existente y su respuesta de dominio.
- `401`: credencial ausente o inválida.
- `403`: sesión o identidad sin permiso para consultar mensajes.
- `404`: mensaje inexistente, inactivo, deshabilitado para el ambiente o de otra aplicación.
- `500`: fallo técnico; no se presenta como mensaje inexistente.

Los errores conservan el formato centralizado existente de la API. El adaptador
HTTP del consumidor debe comprobar el estado antes de deserializar el mensaje
y definir timeout y comportamiento de respaldo ante indisponibilidad.

La primera versión consulta un solo código. Las consultas por conjuntos y la
traducción dinámica quedan fuera de este contrato inicial.
