# Consulta pública de mensajes

Rutas para que las aplicaciones de los otros equipos lean sus mensajes sin sesión BFF.
Son **públicas** mientras seguridad no resuelva la autorización entre aplicaciones: no piden
cookie ni token y el PEP no las intercepta (`security.pep.enforcement.public-paths`).
La administración (crear, editar, importar) sigue protegida con sesión y PEP.

| Método | Ruta | Para qué |
|---|---|---|
| GET | `/messageucolab/v1/public/messages` | Todos los mensajes disponibles de la aplicación en el ambiente |
| GET | `/messageucolab/v1/public/messages/{code}?language=en` | Un mensaje en un idioma (`language` es opcional) |

Ambas exigen dos encabezados:

```http
X-Application-Id: <UUID de la aplicación en MessageUcoLab>
X-Environment-Id: <UUID de un ambiente de esa aplicación>
```

## Listado

```bash
curl -H "X-Application-Id: ba7ddc8e-e159-4025-9d6f-7e49ec3e98b1" \
     -H "X-Environment-Id: 0f145a20-ac27-4f69-b440-8a29d112cc63" \
     http://localhost:8000/messageucolab/v1/public/messages
```

```json
{
  "language": "es",
  "total": 1,
  "messages": [
    {
      "code": "MSG_PRODSAVED",
      "title": "Producto guardado",
      "content": "El producto {nombre} se guardo correctamente.",
      "type": "Funcional",
      "category": "Confirmación",
      "functionality": "Gestion de inventario",
      "created_at": "2026-10-27T20:27:52Z",
      "updated_at": "2026-10-27T20:27:52Z"
    }
  ]
}
```

Solo aparecen los mensajes en estado Activo y habilitados en ese ambiente, ordenados por
código y en el idioma base de la aplicación (`language`). Si no hay ninguno, `messages` llega vacío.

## Un mensaje

Sin `language` se entrega el texto original. Con otro idioma se traduce con el traductor
configurado (Ollama) y la traducción queda en Redis; si el mensaje se edita, se traduce de nuevo.
La primera traducción puede tardar varios segundos; las siguientes salen de la caché.

```json
{
  "code": "MSG_PRODSAVED",
  "sourceLanguage": "es",
  "targetLanguage": "en",
  "title": "...",
  "content": "...",
  "type": "Functional",
  "category": "Confirmation",
  "functionality": "Inventory Management",
  "created_at": "2026-10-27T20:27:52Z",
  "updated_at": "2026-10-27T20:27:52Z"
}
```

## Cliente Maven para Java

Los consumidores que usan **Java 25 y Spring Boot 4.1.1** pueden incorporar el
starter publicado en GitHub Packages. El starter mantiene separados el contrato
público, el adaptador HTTP y los DTO internos de esta API.

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

GitHub Packages requiere que el consumidor configure el servidor Maven `github`
con credenciales que tengan permiso `read:packages`. Esas credenciales no forman
parte del POM ni del artefacto.

```yaml
messageucolab:
  url: ${MESSAGEUCOLAB_URL:http://localhost:8000}
  application-id: ${MESSAGEUCOLAB_APPLICATION_ID}
  environment-id: ${MESSAGEUCOLAB_ENVIRONMENT_ID}
  timeout: 60s
```

Spring registra un `MessageReader` con estas operaciones:

```java
Mono<MessageDetails> details = messages.getDetails("MSG_PRODSAVED");
Mono<MessageDetails> translated = messages.getDetails("MSG_PRODSAVED", "en");
Mono<MessageCollection> all = messages.getAll();
```

El cliente aplica el respaldo opcional del consumidor ante `404`, respuestas
`5xx` y errores de transporte. Para definirlo, la aplicación registra un bean
`MessageFallbackPort`; si no lo hace, se conserva el error original. El listado
completo no usa respaldo porque no existe un mensaje individual que resolver.

## Errores

| HTTP | Cuándo |
|---|---|
| 422 | Falta un encabezado, el UUID no es válido o el idioma no tiene un formato válido |
| 404 | La aplicación o el ambiente no existen, o el mensaje no está disponible en ese ambiente |
| 409 | El ambiente no pertenece a la aplicación, o el mensaje cambió mientras se traducía |
| 403 | El consumidor no está autorizado (solo cuando el modo deje de ser `public`) |
| 405 | Se usó un método distinto de `GET`; el encabezado `Allow` indica los permitidos |

Los errores usan el cuerpo habitual `{"data": [], "errors": [{"code", "message"}], ...}`.

## Límites y CORS

- Kong limita la ruta `/messageucolab/v1/public` a **120 peticiones por minuto por IP**
  (encabezados `RateLimit-*` en la respuesta).
- CORS solo afecta a los navegadores: los backends consumen la ruta sin restricción. Desde el navegador
  solo la leen los orígenes de `messageucolab.cors.public-origins` (variable
  `MESSAGEUCOLAB_PUBLIC_CORS_ORIGINS`, separados por coma), con `GET` y sin credenciales.

## Paso a ruta asegurada

La decisión de acceso está en el puerto `ConsumerAuthorizationPort`. Hoy lo implementa
`OpenConsumerAuthorizationAdapter`, activo con `messageucolab.consumer.authorization.mode=public`
(variable `MESSAGEUCOLAB_CONSUMER_AUTHORIZATION_MODE`). Cuando seguridad publique la autorización
entre aplicaciones se agrega un adaptador que la consulte y se cambia el modo; las rutas y las
respuestas para los consumidores no cambian.
