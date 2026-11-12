# Consumo de mensajes

> **Última actualización:** 31 de octubre de 2026
> **Estado:** Documento oficial vigente
> **Fuente de verdad:** contrato OpenAPI y módulos `clients/java`

## Modelo de consumo

Las aplicaciones consumidoras consultan mensajes por código y deciden cómo mostrarlos. MessageUcoLab no conoce sus pantallas ni reemplaza los marcadores incluidos en el contenido.

Un mensaje solo se entrega cuando:

- La aplicación existe.
- El ambiente existe y pertenece a la aplicación.
- El mensaje está activo.
- La asociación del mensaje con el ambiente está activa.

Las respuestas de consumo usan `Cache-Control: no-store` para evitar reutilización HTTP entre contextos.

## API de consumo con identidad de servicio

Las aplicaciones consumidoras llaman a las rutas de consumo con el token de su **identidad de servicio** (`client_credentials`), nunca con el token de un usuario:

```http
GET  /messageucolab/v1/messages/consumer
GET  /messageucolab/v1/messages/consumer?code=MSG_PRODSAVED
POST /messageucolab/v1/messages/consumer/translations?code=MSG_PRODSAVED
Authorization: Bearer <token-de-la-identidad-de-servicio>
```

El consumidor no envía la aplicación ni el ambiente: MessageUcoLab los deduce del `client_id` del token (claim `azp`). Para que una identidad pueda usar las rutas:

1. La aplicación consumidora crea una identidad de servicio por ambiente en el componente de seguridad.
2. Solicita acceso a los recursos de consumo de MessageUcoLab, o al rol técnico que los agrupa.
3. MessageUcoLab aprueba la solicitud.
4. El propietario de la aplicación en MessageUcoLab asocia el `client_id` de esa identidad al ambiente que consume, en **Identidades de servicio** de la pantalla de aplicaciones. Cada `client_id` consume un solo ambiente.

Sin token válido la respuesta es `401` (PEP); con una identidad sin permiso para la ruta, `403` (PEP); con una identidad que no está asociada a ningún ambiente, `403` (`FUN_232`). Una identidad solo lee los mensajes del ambiente al que fue asociada, aunque tenga permiso sobre la ruta. Kong limita estas rutas a 120 solicitudes por minuto y por IP.

### Registro de identidades por el propietario

Con la sesión BFF del propietario:

```http
GET    /messageucolab/v1/application/{applicationId}/service-identities
POST   /messageucolab/v1/application/{applicationId}/service-identities
DELETE /messageucolab/v1/application/{applicationId}/service-identities/{clientId}
```

El cuerpo del `POST` es `{"environmentId": "<uuid-ambiente>", "clientId": "<client_id>"}`. Si el `client_id` ya está asociado a otro ambiente la respuesta es `409` (`FUN_235`). Al eliminar una aplicación se eliminan sus asociaciones.

Sin `code`, `GET /messages/consumer` devuelve todos los mensajes disponibles del ambiente en el idioma base:

```json
{
  "language": "es",
  "total": 1,
  "messages": [
    {
      "code": "MSG_PRODSAVED",
      "title": "Producto guardado",
      "content": "El producto {nombre} se guardó correctamente.",
      "type": "Funcional",
      "category": "Confirmación",
      "functionality": "Gestión de inventario",
      "created_at": "2026-10-27T20:27:52Z",
      "updated_at": "2026-10-27T20:27:52Z"
    }
  ]
}
```

La traducción (`POST /consumer/translations` con `{"targetLanguage": "en"}`) guarda temporalmente el resultado en Redis. Una edición cambia la huella y obliga a generar una traducción nueva.

## API de consumo con sesión

Las mismas rutas aceptan la sesión BFF del propietario de la aplicación, por ejemplo desde la pantalla de administración:

```http
GET /messageucolab/v1/messages/consumer?code=MSG_WELCOME
X-Application-Id: <uuid-aplicacion-propia>
X-Environment-Id: <uuid-ambiente>
```

```http
POST /messageucolab/v1/messages/consumer/translations?code=MSG_WELCOME
X-Application-Id: <uuid-aplicacion-propia>
X-Environment-Id: <uuid-ambiente>
Content-Type: application/json

{ "targetLanguage": "en" }
```

Estas operaciones requieren sesión BFF y permiso PEP. No son un mecanismo máquina-a-máquina y no se debe compartir la sesión de un propietario con un servicio consumidor.

La respuesta es un objeto directo, sin envelope administrativo ni identificadores internos:

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

El idioma origen proviene de `application.language_id`. No se acepta desde el body ni se autodetecta. Si origen y destino coinciden, se devuelven los textos originales sin invocar IA.

## Consistencia y caché

La resolución usa Redis como caché y SurrealDB como autoridad:

1. Valida aplicación y ambiente.
2. Busca por código y ambiente en Redis.
3. Comprueba disponibilidad y versión en SurrealDB incluso ante un acierto.
4. Recarga desde SurrealDB cuando no existe una entrada válida.
5. No sirve caché si SurrealDB no puede confirmar disponibilidad.

La traducción se separa por aplicación, ambiente, idiomas y huella del mensaje. Después de traducir, la disponibilidad y la versión se verifican otra vez. Un cambio concurrente devuelve `409`.

## Avisos de cambios

Si la aplicación consumidora guarda mensajes en su propia caché, puede enterarse de los cambios sin consultar el catálogo de forma repetitiva suscribiéndose al topic de Pulsar de su ambiente. El detalle del topic y del mensaje está en el [contrato de la API](contrato-api.md#avisos-de-cambios-para-aplicaciones-consumidoras).

Cómo reaccionar:

1. Suscribirse solo al topic del ambiente que se consume (`<prefijo>-<environmentId>`). Los avisos de otros ambientes no llegan a ese topic.
2. Ante `MessageCreated` o `MessageUpdated`, invalidar la entrada de caché del `messageCode` (en cada idioma) y consultar de nuevo el mensaje. Un `404` indica que ya no está disponible: retirarlo de la caché.
3. Ante `MessageDeleted`, retirar el mensaje de la caché.
4. Ante `MessagesChanged` (código vacío), refrescar todo el ambiente.
5. Ignorar avisos repetidos por `eventId` y no suponer orden entre mensajes distintos.
6. Al arrancar, al cambiar de ambiente y de forma periódica y ligera, refrescar la caché: el aviso es de mejor esfuerzo y puede perderse.

El aviso no reemplaza la verificación de disponibilidad de las consultas de consumo ni lleva contenido.

## Cliente Java

El starter requiere Java 25 y Spring Boot 4.1.1:

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/CamiloGarcia09/MessageUcolab--Backend</url>
    </repository>
</repositories>

<dependency>
    <groupId>co.edu.uco</groupId>
    <artifactId>messageucolab-spring-boot-starter</artifactId>
    <version>0.1.0</version>
</dependency>
```

Las credenciales de GitHub Packages pertenecen al `settings.xml` o gestor de secretos del consumidor, nunca al POM.

El consumidor también necesita el starter de seguridad con su identidad de servicio:

```properties
# Identidad de servicio de la aplicación consumidora (los valores llegan desde el gestor de secretos)
security.pep.service-identity.enabled=true
security.pep.service-identity.token-uri=${KEYCLOAK_TOKEN_URI}
security.pep.service-identity.client-id=${MESSAGEUCOLAB_CLIENT_ID}
security.pep.service-identity.client-secret=${MESSAGEUCOLAB_CLIENT_SECRET}
```

El starter de MessageUcoLab obtiene el token con el `ServiceAccessTokenProvider` del starter de seguridad, que lo guarda en caché y lo renueva antes de vencer. Si la aplicación obtiene el token de otra forma, puede registrar su propio bean `MessageUcoLabAccessTokenProvider`.

```yaml
messageucolab:
  url: ${MESSAGEUCOLAB_URL:http://localhost:8000}
  timeout: 60s
```

El consumidor no configura identificadores de aplicación ni de ambiente: MessageUcoLab los deduce del `client_id` de su identidad.

Uso:

```java
Mono<MessageDetails> message = messageReader.getDetails("MSG_PRODSAVED");
Mono<MessageDetails> translated = messageReader.getDetails("MSG_PRODSAVED", "en");
Mono<MessageCollection> catalog = messageReader.getAll();
```

Un bean opcional `MessageFallbackPort` puede resolver un respaldo ante `404`, respuestas `5xx` o errores de transporte. El listado completo no usa respaldo.

## Errores

| Estado | Significado | Acción |
|---|---|---|
| `400` / `422` | Entrada, código o idioma inválido | Corregir solicitud |
| `401` | Token o sesión ausente o inválido | Revisar la identidad de servicio |
| `403` | Falta permiso o autorización del consumidor | Revisar integración |
| `404` | Mensaje no disponible | Revisar código y ambiente |
| `409` | Ambiente ajeno o cambio concurrente | Recargar y reintentar |
| `429` | Límite de Kong | Aplicar espera y backoff |
| `5xx` / timeout | Fallo técnico | Usar respaldo acotado |

## Recomendaciones

- Definir timeout y respaldo en el consumidor.
- Separar caché por aplicación, ambiente e idioma.
- Sustituir marcadores como `{nombre}` en la aplicación consumidora.
- No almacenar credenciales en JavaScript, URLs o logs.
- No exponer directamente el puerto 8085 en entornos públicos.
- Guardar el secreto de la identidad de servicio solo en el gestor de secretos; nunca en el front.

## Documentos relacionados

- [Contrato de la API](contrato-api.md)
- [Integración de seguridad](integracion-seguridad.md)
- [Espacios personales](espacios-personales.md)
