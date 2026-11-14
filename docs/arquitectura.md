# Arquitectura de MessageUcoLab

> **Última actualización:** 31 de octubre de 2026
> **Estado:** Documento oficial vigente
> **Fuente de verdad técnica:** código de producción y archivos Maven del repositorio

## Propósito

MessageUcoLab es un Building Block para administrar y distribuir mensajes de aplicaciones consumidoras. Organiza la información mediante la jerarquía Organización → Aplicación → Ambiente → Funcionalidad → Mensaje, incorpora traducción asistida por IA y mantiene el aislamiento por propietario, aplicación y ambiente.

El minifront de MessageUcoLab es administrativo. Las pantallas, la navegación y la selección de idioma de usuarios finales pertenecen a las aplicaciones consumidoras.

## Módulos

| Módulo | Responsabilidad | Ejecutable |
|---|---|---|
| `utils` | Utilidades, excepciones base y códigos del catálogo | No |
| `core` | Dominio, casos de uso, puertos, DTO y validadores | No |
| `infrastructure` | API WebFlux, configuración y adaptadores técnicos | Sí |
| `clients/java/messageucolab-client-api` | Contrato público del cliente Java | No |
| `clients/java/messageucolab-spring-boot-starter` | Autoconfiguración y cliente HTTP | No |

La dirección arquitectónica exigida es:

```text
infrastructure → core → utils
```

`core` no debe depender de adaptadores, drivers ni detalles técnicos de `infrastructure`. La composición de dependencias corresponde a `infrastructure`.

## Flujo de una operación

```text
Cliente
  → Kong
  → filtros de seguridad y contexto
  → controller
  → facade / puerto primario
  → caso de uso
  → validador Composite
  → puerto secundario
  → adaptador
  → SurrealDB, Redis, Pulsar, IA o servicio externo
```

Los controllers traducen HTTP y delegan. Las reglas de negocio, propiedad y jerarquía pertenecen a los casos de uso y validadores.

## Componentes principales

| Componente | Tecnología | Función |
|---|---|---|
| API | Spring Boot 4.1.1, Spring Framework 7 y WebFlux | Transporte HTTP reactivo |
| Persistencia | SurrealDB, imagen 3.1.4 y driver 2.1.1 | Fuente principal de datos |
| Caché | Redis | Mensajes, traducciones, borradores y catálogo de textos |
| Eventos | Pulsar 3.2.2 | Eventos de dominio, proyecciones y avisos de cambios a consumidores (un topic por ambiente) |
| Tiempo real | SSE en memoria | Notificaciones al minifront |
| Traducción | LangChain4j 1.15.1 | Integración con Ollama u OpenAI |
| Seguridad | Sesión BFF y starter PEP 0.1.3 | Autenticación externa y autorización por ruta |
| Gateway | Kong 3.5 | Entrada, enrutamiento y rate limiting |
| Observabilidad | Actuator, Prometheus, Loki y Grafana | Salud, métricas y logs |

## Persistencia y proyecciones

SurrealDB es la única base principal. No existe un motor SQL adicional. Los adaptadores usan SurrealQL y el SDK oficial.

Los agregados fuente generan eventos en `domain_events`. Un consumidor programado actualiza proyecciones como `application_document`, `environment_document`, `message_data_collection` y `message_environment_readmodel`.

Los ambientes de un mensaje se representan mediante relaciones `message_environment`. Un mensaje comparte contenido entre sus ambientes; habilitarlo en varios ambientes no crea copias independientes.

Las fechas se manejan en UTC. `created_at` representa la creación original y `updated_at` la última escritura conocida. Si un dato histórico no tiene una fecha confiable, la API devuelve `null`; no inventa valores.

## Caché y catálogo

Redis cumple funciones separadas:

- Caché de lectura de mensajes.
- Caché de traducciones.
- Borradores temporales de importación.
- Catálogo de textos técnicos, funcionales, prompts y plantillas.

Los textos propios no se escriben como literales en clases Java. Se registran en `MessageCatalogCodeEnum` y en `deployment/docker/scripts/redis/CatalogMessageInit.sh`.

Los identificadores técnicos, UUID, rutas, headers, claves JSON y marcadores no se traducen. La interfaz administrativa y los catálogos iniciales están en español; los ambientes conservan los nombres `Develop`, `Testing` y `Produccion`.

## Validación y errores

Las validaciones nuevas siguen el patrón:

```text
Specification<T>
  → RuleValidator<T>
  → CompositeValidator<T>
```

Las reglas de formato se ejecutan antes de consultas de existencia, pertenencia o duplicidad. Las excepciones usan la jerarquía de dominio y se traducen a HTTP de forma centralizada.

| Tipo | Estado HTTP habitual |
|---|---:|
| Entrada HTTP inválida | 400 |
| Sesión inválida | 401 |
| Falta de permiso o propiedad | 403 |
| Recurso no encontrado | 404 |
| Formato de respuesta no admitido | 406 |
| Conflicto de concurrencia o duplicidad | 409 |
| Regla de negocio incumplida | 422 |
| Fallo técnico | 500 |

## Reactividad y SSE

La capa HTTP usa WebFlux. El acceso actual a SurrealDB y parte del acceso a Redis pueden ser síncronos; esas operaciones deben mantenerse fuera del event loop.

SSE ofrece actualizaciones administrativas por aplicación mediante `GET /messageucolab/v1/messages/events`, con sesión BFF, permiso PEP y `X-Application-Id` autorizado. La suscripción admite notificaciones por aplicación además de las existentes por ambiente. Después de confirmar las escrituras de la proyección, el backend emite `MessageCreated`, `MessageUpdated` o `MessageDeleted` según el evento de origen, con aplicación y código de mensaje; la señal por aplicación no depende de que ya exista una proyección por ambiente. Añadir, cambiar o quitar una asociación de ambiente es `MessageUpdated` si el mensaje sigue existiendo, nunca una creación o eliminación del mensaje completo.

`MessagesChanged` se reserva para la inactivación colectiva de mensajes de una funcionalidad. La transacción de cascada marca `metadata.notification_type` en los eventos nuevos que genera, preservando los eventos pendientes anteriores; el proyector reconoce el origen sin deducirlo por el estado actual del mensaje. Se usa el objeto flexible de metadatos existente, sin migración de esquema ni cambio de triggers. El lote puede producir varias invalidaciones conforme se proyectan sus mensajes; el frontend las agrupa. SSE no garantiza un único evento por acción.

El frontend agrupa ráfagas de eventos y consulta nuevamente el listado, conservando filtros y orden y ajustando una página que haya dejado de existir. Cada conexión o reconexión provoca también esa resincronización. No se usan recargas de página ni esperas fijas para adivinar cuándo terminó una proyección. El procesador de proyecciones sigue siendo asíncrono (intervalo configurado de 2 segundos más el tiempo de procesamiento y la cola).

La implementación en memoria tiene límites explícitos:

- Los eventos no son duraderos.
- Una reconexión no reproduce eventos perdidos: el cliente recupera el estado actual consultando el listado tras conectarse.
- Varias réplicas no comparten automáticamente el flujo.
- No reemplaza Pulsar ni constituye una garantía de entrega.

## Avisos de cambios a componentes consumidores

Cuando un mensaje se crea, se edita o se elimina, MessageUcoLab publica un aviso en Pulsar para que los componentes que lo consumen actualicen su caché sin consultar el catálogo de forma repetitiva. El aviso es una señal sin contenido: el consumidor vuelve a consultar el mensaje por los endpoints de consumo.

```text
domain_events → proyector → Publisher<MessageEvent>
                                 └─ CompositeMessageEventPublisher
                                       ├─ InMemoryMessageEventAdapter (SSE administrativo)
                                       └─ PulsarMessageEventPublisher → topic <prefijo>-<clientId> de cada identidad
                                                                        asociada al ambiente (environment_service_identity)
                                                                        y topic <prefijo>-<environmentId> (compatibilidad)
```

- El proyector depende solo de `Publisher<MessageEvent>` (puerto en `core`). El adaptador de Pulsar y el compuesto viven en `infrastructure`, y `MessageEventPublisherConfig` registra el compuesto como bean primario.
- **Aislamiento por ambiente sin copiar ids:** el aviso de un ambiente se publica en el topic de cada identidad de servicio asociada a ese ambiente. Un consumidor se suscribe al topic de su propio `client_id`; si su identidad lee Develop, no recibe avisos de Produccion. Solo se publican los eventos por ambiente que emite el proyector; los eventos por aplicación mezclan ambientes y no se publican.
- Quitar la asociación de un mensaje a un ambiente genera un aviso solo para ese ambiente.
- Publicar en Pulsar es independiente del SSE: si Pulsar falla, el SSE y la proyección siguen funcionando. El publicador nunca lanza excepciones hacia el proyector y crea sus productores de forma perezosa, por lo que Pulsar no es requisito de arranque.
- `pulsar.events-enabled=false` desactiva solo estos avisos.

Límites actuales:

- La entrega es de mejor esfuerzo: si Pulsar no está disponible en ese instante, el aviso se registra como error y se pierde. Un patrón *outbox* con reintentos queda como mejora futura.
- Hay un retraso de hasta el intervalo del proyector (2 segundos) más el tiempo de procesamiento.
- El aislamiento por topic evita el cruce por error. Impedir que un consumidor se suscriba al topic de otro ambiente requiere permisos por topic en Pulsar, que se configuran en el broker.

El contrato del aviso y la guía para consumidores están en [contrato-api.md](contrato-api.md#avisos-de-cambios-para-aplicaciones-consumidoras) y [consumo-mensajes.md](consumo-mensajes.md#avisos-de-cambios).

## Seguridad y propiedad

Security Baseline autentica mediante sesión BFF y el PEP autoriza rutas. MessageUcoLab comprueba adicionalmente la propiedad de organizaciones, aplicaciones, funcionalidades y mensajes.

La identidad local estable se deriva de `identity_source + tenantId + subject`. El correo y el nombre son informativos. No se emiten tokens propios, no se acepta el header `Token` y no existen roles o membresías locales.

## Deuda arquitectónica conocida

El objetivo es mantener `core` libre de frameworks, pero el estado actual aún contiene anotaciones Spring, tipos de Spring Data y dependencias heredadas en ese módulo. Esta deuda no debe reproducirse en código nuevo y requiere una refactorización separada con pruebas autorizadas.

También existen archivos de datos locales de SurrealDB versionados bajo `deployment/docker/surreal-data`; deben tratarse como deuda de repositorio y no como parte del modelo de dominio.

## Incorporación de funcionalidades

Una funcionalidad vertical debe incluir, cuando corresponda:

1. DTO y puerto primario en `core`.
2. Caso de uso y contrato de manejo.
3. Validadores y excepciones de dominio.
4. Puerto secundario.
5. Adaptador en `infrastructure`.
6. Controller y presentación HTTP.
7. Códigos y textos del catálogo Redis.
8. Actualización del OpenAPI.
9. Pruebas mediante el agente autorizado del proyecto.

## Documentos relacionados

- [Contrato de la API](contrato-api.md)
- [Integración de seguridad](integracion-seguridad.md)
- [Espacios personales](espacios-personales.md)
- [Consumo de mensajes](consumo-mensajes.md)
- [Importación de mensajes](importacion-mensajes.md)
- [Despliegue](despliegue.md)
