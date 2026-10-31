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

## API pública

La opción recomendada para el cliente Java actual usa rutas públicas temporales:

```http
GET /messageucolab/v1/public/messages
GET /messageucolab/v1/public/messages/{code}?language=en
X-Application-Id: <uuid-aplicacion>
X-Environment-Id: <uuid-ambiente>
```

No requieren cookie BFF ni pasan por el PEP. Esta excepción existe mientras seguridad no ofrezca autorización aplicación-a-aplicación. Kong limita la ruta a 120 solicitudes por minuto y por IP.

Ejemplo de listado:

```bash
curl -H "X-Application-Id: <uuid-aplicacion>" \
     -H "X-Environment-Id: <uuid-ambiente>" \
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

Sin `language`, la consulta individual devuelve el idioma base. Con otro idioma, traduce el mensaje y guarda temporalmente el resultado en Redis. Una edición cambia la huella y obliga a generar una traducción nueva.

## API de consumo con sesión

El contrato protegido permite consultar y traducir con sesión BFF:

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

```yaml
messageucolab:
  url: ${MESSAGEUCOLAB_URL:http://localhost:8000}
  application-id: ${MESSAGEUCOLAB_APPLICATION_ID}
  environment-id: ${MESSAGEUCOLAB_ENVIRONMENT_ID}
  timeout: 60s
```

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
| `401` | Sesión requerida inválida | Revisar autenticación |
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
- Acordar con seguridad un contrato máquina-a-máquina antes de retirar la excepción pública.

## Documentos relacionados

- [Contrato de la API](contrato-api.md)
- [Integración de seguridad](integracion-seguridad.md)
- [Espacios personales](espacios-personales.md)
