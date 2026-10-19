# Guía de integración: consumir el componente de mensajes (MessageUcoLab)

Esta guía es para los equipos que van a **usar** el componente de mensajes desde su aplicación
(Asistencia, Parámetros, Seguridad u otra). Explica cómo reemplazar los textos escritos en el
código por mensajes administrados en MessageUcoLab, cómo autenticarse y qué hacer cuando algo
falla.

Todo lo que aparece aquí se verificó contra el backend en ejecución (rama `develop`, Java 25).
Si algo no se comporta como se describe, avísenle al equipo de mensajes.

---

## 1. Idea general

Hoy su aplicación tiene textos escritos en el código:

```java
return "Los cambios se guardaron correctamente.";
```

Con MessageUcoLab, ese texto vive en el componente y su aplicación lo pide por un **código**:

```java
return mensajes.get("MSG_PRODSAVED");   // el texto lo administra MessageUcoLab
```

Así se puede cambiar el texto, corregirlo o traducirlo **sin volver a desplegar su aplicación**.

La consulta se hace **de backend a backend**. El navegador de sus usuarios nunca llama a
MessageUcoLab directamente, y el token de integración nunca se envía al frontend.

---

## 2. Conceptos que deben conocer

```
Organización
 └─ Aplicación          (por ejemplo "Asistencia")
     ├─ Ambientes       Develop, Testing y Production, que se crean solos con la aplicación
     ├─ Funcionalidades agrupan los mensajes (por ejemplo "Registro de asistencia")
     └─ Mensajes        código + título + contenido + tipo + categoría + estado
```

- **Código:** identifica el mensaje. Siempre empieza por `MSG_` y está en mayúsculas,
  por ejemplo `MSG_PRODSAVED`. Al crear el mensaje solo se escribe el sufijo (`PRODSAVED`):
  máximo 10 caracteres, solo letras, números y espacios. El componente le agrega `MSG_`.
- **El código es único por aplicación.** Dos aplicaciones distintas pueden tener cada una su
  `MSG_PRODSAVED` sin chocar.
- **Un mensaje se crea en un ambiente.** Si se crea en Develop, solo está disponible en Develop
  (ver la sección 9).
- **Estado:** solo se entregan mensajes en estado **Active**. Un mensaje Inactive responde como
  si no existiera (404).

---

## 3. Qué necesitan antes de empezar

| Dato | Ejemplo | Quién se lo da |
|---|---|---|
| URL base del componente | `http://localhost:8000` (gateway Kong) o `http://localhost:8085` (directo) | Equipo de mensajes |
| Su aplicación registrada en MessageUcoLab, con al menos una funcionalidad | `Asistencia` | Equipo de mensajes (o ustedes desde el widget de administración) |
| Un **token de integración** por cada ambiente que vayan a usar | Develop, Testing, Production | Equipo de mensajes |

> **Mientras se integra el componente de seguridad**, el registro de aplicaciones y la generación
> de tokens los hace el equipo de mensajes. Pídanlos indicando el nombre exacto de su aplicación,
> los ambientes que necesitan y hasta qué fecha debe valer cada token.

**Ustedes no necesitan Doppler ni ninguna variable de MessageUcoLab para consumirlo.** Doppler
solo hace falta si van a levantar el componente en su máquina (sección 8).

---

## 4. Paso a paso: reemplazar un texto escrito en el código

### 4.1 Crear el mensaje

Desde el widget o el minifront de administración, en **su aplicación y su ambiente**:

| Campo | Regla |
|---|---|
| Código | Solo el sufijo, máximo 10 caracteres, letras, números y espacios. `PRODSAVED` se guarda como `MSG_PRODSAVED` |
| Título | Entre 10 y 50 caracteres |
| Contenido | Entre 10 y 100 caracteres. Puede tener marcadores como `{nombre}` (ver 4.4) |
| Tipo, categoría, funcionalidad | Obligatorios |
| Estado | **Active**, o no se entregará |

### 4.2 Guardar el token como secreto

El token es una cadena larga (unos 344 caracteres). Trátenlo como una contraseña:

- Guárdenlo en el gestor de secretos de su equipo (Doppler, Azure Key Vault, etc.) y léanlo
  como variable de entorno, por ejemplo `MESSAGEUCOLAB_TOKEN`.
- **No** lo escriban en el código, en `application.properties` ni en `application.yml` con un
  valor por defecto, ni lo suban a Git.
- **No** lo envíen al navegador.
- Un token sirve para **una aplicación y un ambiente**. Para Develop y Production necesitan dos
  tokens distintos.

### 4.3 Pedir el mensaje

```http
GET {URL_BASE}/messageucolab/v1/messages/resolve?code=MSG_PRODSAVED
Accept: application/json
Token: <su-token-de-integracion>
```

No envíen el nombre de la aplicación ni el ambiente: **el token ya los define**.

Respuesta exitosa (HTTP 200):

```json
{
  "data": [
    {
      "code": "MSG_PRODSAVED",
      "title": "Producto guardado",
      "content": "El producto {nombre} se guardó correctamente."
    }
  ],
  "errors": []
}
```

El mensaje viene en `data[0]`. Prueba rápida desde la terminal:

```bash
curl -s "http://localhost:8000/messageucolab/v1/messages/resolve?code=MSG_PRODSAVED" -H "Token: $MESSAGEUCOLAB_TOKEN"
```

### 4.4 Marcadores dentro del contenido

MessageUcoLab entrega el texto **tal cual**, con los marcadores sin reemplazar. Su aplicación
los reemplaza:

```
Contenido guardado:  "Quedan {cantidad} unidades de {nombre}."
Su aplicación:       "Quedan 3 unidades de Teclado."
```

Usen nombres simples entre llaves y documenten qué marcadores espera cada código.

### 4.5 Ejemplo completo en Java (Spring WebFlux)

Este cliente pide el mensaje, lo guarda en caché y, si MessageUcoLab falla, usa el texto que
antes estaba escrito en el código. **Nunca lanza una excepción** hacia la lógica de negocio.
Está basado en el cliente de referencia de InventarioWebFlux.

```java
@Service
public class MensajesClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(4);
    private static final long TTL_ENCONTRADO_MS = 5 * 60 * 1000L;   // 5 minutos
    private static final long TTL_NO_ENCONTRADO_MS = 30 * 1000L;    // 30 segundos

    // Textos de respaldo: los mismos que antes estaban escritos en el código
    private static final Map<String, String[]> RESPALDO = Map.of(
        "MSG_PRODSAVED", new String[]{"Operación completada", "Los cambios se guardaron correctamente."}
    );

    private final WebClient webClient;
    private final String token;
    private final Map<String, Entrada> cache = new ConcurrentHashMap<>();

    public MensajesClient(WebClient.Builder builder,
                          @Value("${messageucolab.url}") String url,
                          @Value("${MESSAGEUCOLAB_TOKEN}") String token) {   // viene del gestor de secretos
        this.webClient = builder.baseUrl(url).build();
        this.token = token;
    }

    public Mono<Mensaje> get(String codigo, Map<String, String> variables) {
        Entrada guardada = cache.get(codigo);
        if (guardada != null && !guardada.vencida()) {
            return Mono.just(guardada.mensaje().conVariables(variables));
        }
        return webClient.get()
            .uri("/messageucolab/v1/messages/resolve?code={code}", codigo)
            .header("Token", token)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .timeout(TIMEOUT)
            .map(json -> {
                JsonNode item = json.path("data").path(0);
                Mensaje m = new Mensaje(codigo, item.path("title").asText(), item.path("content").asText());
                cache.put(codigo, Entrada.por(m, TTL_ENCONTRADO_MS));
                return m.conVariables(variables);
            })
            .onErrorResume(error -> {
                String[] r = RESPALDO.getOrDefault(codigo, new String[]{codigo, codigo});
                Mensaje m = new Mensaje(codigo, r[0], r[1]);
                cache.put(codigo, Entrada.por(m, TTL_NO_ENCONTRADO_MS));
                return Mono.just(m.conVariables(variables));
            });
    }

    public record Mensaje(String codigo, String titulo, String contenido) {
        Mensaje conVariables(Map<String, String> vars) {
            String t = titulo, c = contenido;
            for (var e : vars.entrySet()) {
                t = t.replace("{" + e.getKey() + "}", e.getValue());
                c = c.replace("{" + e.getKey() + "}", e.getValue());
            }
            return new Mensaje(codigo, t, c);
        }
    }

    private record Entrada(Mensaje mensaje, long expira) {
        static Entrada por(Mensaje mensaje, long ttlMs) {
            return new Entrada(mensaje, System.currentTimeMillis() + ttlMs);
        }
        boolean vencida() { return System.currentTimeMillis() > expira; }
    }
}
```

Uso:

```java
return mensajes.get("MSG_PRODSAVED", Map.of("nombre", producto.getNombre()));
```

Este ejemplo se compiló y ejecutó con Java 25 contra el componente real, en cinco escenarios:
mensaje encontrado (con el marcador reemplazado), segunda consulta desde la caché, token
inválido (401), componente apagado y código inexistente (404). En los tres últimos devolvió el
texto de respaldo sin lanzar excepciones.

Recomendaciones:

- **Siempre tengan un texto de respaldo.** Si MessageUcoLab no responde, su aplicación debe
  seguir funcionando.
- **Definan un timeout corto** (4 segundos en el ejemplo). La primera consulta después de que
  MessageUcoLab arranca puede tardar más que eso; en ese caso se muestra el respaldo y las
  siguientes consultas ya llegan normales.
- **Usen caché.** No pidan el mismo mensaje en cada petición de sus usuarios.

### 4.6 ¿Cuándo se ve el cambio?

- MessageUcoLab entrega el mensaje **apenas se crea o se edita**: `/resolve` lee directamente de
  la base de datos.
- Lo que demora es la caché **de su aplicación**. Con los tiempos del ejemplo:
  - Un mensaje que antes no existía aparece en máximo **30 segundos**.
  - Un mensaje editado se actualiza en máximo **5 minutos**.
- Si necesitan verlo de inmediato (por ejemplo, en una demostración), expongan un endpoint
  interno que limpie su caché, como hace InventarioWebFlux con `POST /api/mensajes/limpiar-cache`.

---

## 5. Traducción (opcional)

MessageUcoLab puede traducir un mensaje con IA:

```http
POST {URL_BASE}/messageucolab/v1/messages/MSG_PRODSAVED/translations
Content-Type: application/json
Token: <su-token-de-integracion>

{ "sourceLanguage": "auto", "targetLanguage": "en" }
```

Respuesta (HTTP 200, resumida):

```json
{
  "data": [{
    "code": "MSG_PRODSAVED",
    "translatedTitle": "Product Saved",
    "translatedContent": "The product {name} was saved.",
    "translationProvider": "langchain4j-ollama",
    "translationElapsedMs": 14713
  }],
  "errors": []
}
```

Ténganlo en cuenta:

- **Es lenta:** en local, con Ollama, tarda alrededor de 15 segundos. No la llamen en cada
  petición de un usuario; traduzcan una vez y guarden el resultado.
- **Puede traducir los marcadores** (`{nombre}` → `{name}`). Si usan marcadores, verifiquen la
  traducción antes de usarla. Es un problema conocido (sección 10).
- `targetLanguage` usa códigos de idioma: `en`, `es`, `fr` (los que ofrece la pantalla de
  administración). Se verificó la traducción de español a `en`.

---

## 6. Errores

| HTTP | Cuándo pasa | Qué debe hacer su aplicación |
|---|---|---|
| 200 | Mensaje encontrado | Usar `data[0]` |
| 401 | Token ausente, inválido, vencido o reemplazado por uno nuevo. **La respuesta llega sin cuerpo** | Usar el texto de respaldo y avisar al equipo: hay que renovar el token |
| 404 | El código no existe en su aplicación y ambiente, está Inactive, o es de otra aplicación | Usar el texto de respaldo. Revisar el código y el ambiente |
| 500 | Falla técnica. **Hoy también se recibe si falta el parámetro `code`** | Usar el texto de respaldo |
| Sin respuesta | MessageUcoLab caído o timeout | Usar el texto de respaldo |

Ejemplo de error 404:

```json
{
  "data": [],
  "errors": [{ "code": "NOT_FOUND", "message": "There is no message with the code MSG_X for the application ..." }],
  "timestamp": "2026-10-19T20:27:37.553Z",
  "path": "/messageucolab/v1/messages/resolve"
}
```

Revisen siempre el código HTTP **antes** de leer `data`.

---

## 7. Tokens de integración

### 7.1 Cómo se generan

Por ahora los genera el equipo de mensajes con este endpoint:

```http
POST {URL_BASE}/messageucolab/v1/application/{applicationId}/token
Content-Type: application/json
Authorization: Bearer <identidad-del-administrador>

{ "expirationDate": "2027-01-01T00:00:00", "environmentId": "<id-del-ambiente>" }
```

- La respuesta trae el token en `data[0]`. **Se muestra una sola vez**; si se pierde, se genera
  uno nuevo.
- `expirationDate` usa el formato `yyyy-MM-dd'T'HH:mm:ss` y debe ser una fecha futura. Si no,
  responde 422: *"The expiration date must be a date greater than today."*

### 7.2 Reglas que deben conocer

- **Un solo token activo por ambiente.** Generar uno nuevo **revoca el anterior de inmediato**:
  el viejo empieza a responder 401. Si comparten un ambiente entre varios servicios, todos deben
  actualizar el secreto al mismo tiempo.
- **El token vence** en la fecha indicada y desde ese momento responde 401. Anoten la fecha y
  pidan el nuevo con tiempo.
- **Si el token se filtra**, pidan uno nuevo: al generarlo, el filtrado deja de servir.

### 7.3 ¿Por qué un token y no la identidad de usuario (`Authorization: Bearer`)?

El backend también acepta `Authorization: Bearer`, pero ese camino es para **personas** que
administran mensajes. Con Bearer, el ambiente sale del **contexto que la persona eligió por
última vez en la pantalla de administración**. Si alguien cambia de ambiente en el widget,
los mensajes que ven los usuarios de su aplicación también cambian.

Lo verificamos: después de cambiar el contexto del administrador a otra aplicación, la consulta
con Bearer respondió 404 y la consulta con `Token` siguió devolviendo el mensaje correcto.
**Para consumir mensajes desde su aplicación, usen siempre el encabezado `Token`.**

---

## 8. Levantar MessageUcoLab en su máquina (solo si lo necesitan)

Si solo consumen el componente desplegado por el equipo de mensajes, **salten esta sección**.

### 8.1 ¿Qué es Doppler?

Doppler es un gestor de secretos en la nube: guarda contraseñas y configuración fuera del
código. MessageUcoLab lo usa de dos formas:

1. **Para levantar el stack:** `doppler run -- docker compose up -d` inyecta las variables de
   entorno que leen Docker Compose y la aplicación.
2. **Para generar tokens de integración:** el backend guarda en Doppler la llave privada de cada
   token, usando la variable `DOPPLERTOKEN`. Sin ella no se pueden generar tokens.

El proyecto de Doppler es `messageucolab` con la configuración `dev`. Pídanle acceso al equipo
de mensajes. Si usan su propio proyecto de Doppler, configuren `DOPPLER_PROJECT` y
`DOPPLER_CONFIG`, y usen un `DOPPLERTOKEN` con permiso de escritura en esa configuración.

### 8.2 Requisitos

- Docker Desktop con Docker Compose v2
- Doppler CLI con sesión iniciada (`doppler login`)
- Puertos libres: 8085 (API), 8000 (gateway), 8005 (SurrealDB), 6379 (Redis), 8080 y 6800
  (Pulsar), 11434 (Ollama), además de 3000, 3100, 9090, 14317 y 14318 si usan observabilidad

No necesitan Java ni Maven: la imagen se compila dentro de Docker con Java 25.

### 8.3 Variables

| Grupo | Variables | Notas |
|---|---|---|
| Aplicación | `MESSAGEUCOLAB_PORT` | Normalmente `8085` |
| Base de datos (SurrealDB) | `SURREALDBPORT`, `SURREALDBUSER`, `SURREALDBPASSWORD`, `SURREALDBNAMESPACE`, `SURREALDBDATABASE` | Obligatorias |
| Caché (Redis) | `REDISHOST`, `REDISPORT`, `REDISPASSWORD`, `REDISDATABASE` | Obligatorias |
| Eventos (Pulsar) | `PULSARURL`, `PULSARTOPICNAME`, `PULSAR_PORT`, `PULSAR_PORTEXIT`, `PULSAR_GENERALPORT` | Obligatorias |
| Tokens de integración | `DOPPLERTOKEN`, `DOPPLER_PROJECT` (por defecto `messageucolab`), `DOPPLER_CONFIG` (por defecto `dev`) | `DOPPLERTOKEN` es obligatoria para generar tokens |
| Identidad simulada (temporal, hasta integrar el componente de seguridad) | `SIMULATED_SECURITY_ENABLED`, `SIMULATED_SECURITY_TOKEN`, `SIMULATED_SECURITY_ISSUER`, `SIMULATED_SECURITY_SUBJECT`, `SIMULATED_SECURITY_EMAIL`, `SIMULATED_SECURITY_EXPIRATION` | `ISSUER` y `SUBJECT` deben coincidir con el usuario del seed: `https://issuer.example` y `subject-1`. Si no coinciden, el administrador no ve ningún contexto |
| Azure Key Vault | `AZURE_KEYVAULT_ENABLED`, `AZURE_KEYVAULT_UCOLAB_ENDPOINT`, `AZURE_CLIENT_ID`, `AZURE_CLIENT_SECRET`, `AZURE_TENANT_ID` | En local usen `AZURE_KEYVAULT_ENABLED=false` |
| Traducción con IA | `TRANSLATION_AI_ENABLED`, `TRANSLATION_AI_PROVIDER`, `TRANSLATION_AI_BASE_URL`, `TRANSLATION_AI_MODEL_NAME`, `TRANSLATION_AI_API_KEY`, `TRANSLATION_AI_TEMPERATURE`, `TRANSLATION_AI_MAX_RETRIES`, `TRANSLATION_AI_TIMEOUT_SECONDS`, `OLLAMA_PORT` | Tienen valores por defecto para Ollama local |
| Gateway | `APIGATEWAY_PORT`, `KONGADMIN_PORT` | Normalmente `8000` y `8001` |
| Observabilidad | `GRAFANAPORT`, `GF_SECURITY_ADMIN_PASSWORD`, `LOKIPORT`, `LOKIPORTALERT`, `GRPC_OTELPORT`, `HTTP_OTELPORT`, `PROMETHEUSPORT` | Grafana, Loki, OpenTelemetry y Prometheus |

Nunca suban estos valores a Git ni los pongan en capturas.

### 8.4 Comandos

```bash
cd deployment/docker
doppler run --project messageucolab --config dev -- docker compose up -d --build
```

La primera vez tarda varios minutos: descarga imágenes, compila y ejecuta todas las pruebas, y
descarga el modelo de Ollama. Para comprobar que quedó arriba:

```bash
curl http://localhost:8085/actuator/health        # debe responder {"status":"UP"}
docker compose ps                                  # messageucolab-app debe estar "healthy"
```

Si solo cambió el backend, reconstruyan únicamente ese servicio:

```bash
doppler run --project messageucolab --config dev -- docker compose up -d --no-deps --build messageucolab
```

### 8.5 Cuidado con los datos

Las aplicaciones, mensajes y tokens que creen se guardan en el volumen `docker_surrealdb-data`.
Reiniciar los contenedores o volver a ejecutar el seed **no** los borra (lo verificamos).
**`docker compose down -v` sí borra ese volumen** y, con él, todo lo que hayan registrado. Al
levantar de nuevo solo quedan los datos del seed (la aplicación de ejemplo Arquisoft y el mensaje
`MSG_WELCOME`). Usen `docker compose down` sin `-v` si quieren conservar los datos.

### 8.6 Si reinician SurrealDB, reinicien también la API

Después de reiniciar el contenedor `surrealdb`, la API sigue respondiendo `UP` en
`/actuator/health`, pero **todas las consultas a la base de datos se quedan colgadas**: no se
reconecta sola. Reinicien la API:

```bash
doppler run --project messageucolab --config dev -- docker compose restart messageucolab
```

---

## 9. Pasar mensajes de un ambiente a otro (Develop → Production)

**Hoy no está disponible.** Como el código es único por aplicación, tampoco se puede crear el
mismo código de nuevo en Production: el componente lo rechaza por duplicado.

El equipo de mensajes está trabajando en una operación para promover mensajes en bloque entre
ambientes. Mientras tanto, creen sus mensajes en el ambiente que realmente van a usar y
coordinen con el equipo de mensajes si necesitan tenerlos en más de uno.

---

## 10. Problemas conocidos

| Problema | Impacto | Cómo manejarlo mientras se corrige |
|---|---|---|
| Si falta el parámetro `code`, `/resolve` responde **500** en lugar de 400 | Bajo | Validen el código antes de llamar |
| El 401 llega **sin cuerpo** JSON | Bajo | Revisen el código HTTP antes de leer el cuerpo |
| La traducción puede traducir los marcadores (`{nombre}` → `{name}`) | Medio | Verifiquen los marcadores en el texto traducido |
| No se pueden promover mensajes entre ambientes | Alto | Sección 9 |
| Tras reiniciar SurrealDB, la API no se reconecta y sus consultas se cuelgan, aunque `/actuator/health` diga `UP` | Alto en ambientes compartidos | Reiniciar `messageucolab` (sección 8.6). Con timeout y respaldo, su aplicación no se cae |
| `docs/message-consumer-api.md` describe una respuesta sin el envoltorio `data` y un encabezado `Cache-Control: no-store` que hoy no se envían | Medio | Esta guía describe el comportamiento real |

---

## 11. Lista de verificación para su equipo

- [ ] Su aplicación está registrada en MessageUcoLab con al menos una funcionalidad
- [ ] Tienen un token por cada ambiente que usan, guardado en su gestor de secretos
- [ ] Anotaron la fecha de vencimiento de cada token
- [ ] Cada texto escrito en el código tiene su código `MSG_...` creado en estado Active
- [ ] Su cliente envía el encabezado `Token`, no `Authorization: Bearer`
- [ ] Su cliente tiene timeout, caché y texto de respaldo
- [ ] Su aplicación reemplaza los marcadores `{...}`
- [ ] Probaron el caso en que MessageUcoLab está apagado: su aplicación debe seguir funcionando
