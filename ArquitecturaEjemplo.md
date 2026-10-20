# Esquema de arquitectura

Este proyecto (MessageUcoLab) es una API de mensajes multientorno con catálogo jerárquico
(Organización → Aplicación → Módulo → Funcionalidad → Parámetro), soporte de tokens, traducción
asistida por IA, caché Redis y eventos por broker. Está organizado como un **monorepo Maven
multi-módulo** con **arquitectura hexagonal / clean architecture** orientada a features.

La idea central: la infraestructura depende de la aplicación, pero la aplicación no depende de
detalles externos como REST, SurrealDB, Redis, Pulsar, Docker, Azure Key Vault, Doppler o Prometheus.

## Stack real del proyecto

| Tecnología | Versión | Rol |
|---|---|---|
| Java | 25 | Lenguaje (release 25 en compilación, sin preview) |
| Spring Boot | 4.1.1 | Framework base (Java 25; Spring Framework 7.0.9) |
| SurrealDB | 2.1.1 (driver) / v3.1.4 (imagen) | BD principal (única; cubre relacional y no relacional) |
| Spring Data Redis | Boot 4.1.1 | Caché de lectura de mensajes y catálogo de mensajes |
| Caffeine | Gestionada por Spring Boot | Caché local de secretos Doppler |
| Apache Pulsar | 3.2.2 | Broker de mensajes (eventos de dominio) |
| LangChain4j | 1.15.1 | Traducción asistida por IA (Ollama local por defecto / OpenAI) |
| loki-logback-appender | 1.6.0 | Envío de logs a Loki |
| Spring Boot Actuator + Micrometer | Boot 4.1.1 | Health + métricas Prometheus |
| Azure Key Vault | spring-cloud-azure 7.4.0 | Secretos |
| Doppler | okhttp 4.12.0 | Secretos en runtime + creación de tokens |
| springdoc-openapi | 3.1.1 | Swagger UI / OpenAPI |
| Starter PEP | 0.1.3 | Enforcement de Security Baseline antes de los controladores |
| Lombok / ModelMapper | — | Reducción de boilerplate / mapeo de objetos |

## Vista general

```text
Cliente HTTP / Frontend (JSON, YAML, XML, TXT, HTML por header Accept)
        |
        v
infraestructure.primaryadapters.controller   (Controllers + Interceptors)
        |
        v
core.application.primaryports.facade         (UseCaseFacade + impl)
        |
        v
core.application.usecase.handling            (Handling*Port)
        |
        v
core.application.usecase                     (UseCase)
        |
        +--> core.application.usecase.validator       (CompositeValidator y reglas)
        |
        +--> core.application.secondaryports          (repositorios, catálogo, cache...)
                          |
                          v
              infraestructure.secondaryadapters       (Adapters)
                          |
        +-- SurrealDB  +-- Redis  +-- Pulsar  +-- Doppler/Key Vault  +-- LangChain4j
```

## Módulos del monorepo

| Módulo | Artefacto | Contenido | ¿Genera jar ejecutable? |
|---|---|---|---|
| `core` | `co.edu.uco:core` | Casos de uso, puertos (interfaces), dominios, validadores, entidades de salida, facades | No |
| `infrastructure` | `co.edu.uco:infrastructure` | Adaptadores primarios (controllers REST, interceptors) y secundarios (SurrealDB, Redis, Pulsar, LLM, Azure Key Vault, Doppler, Loki, serializers) | Sí (`CrossWordApplication`) |
| `utils` | `co.edu.uco:utils` | Helpers transversales (UUID, texto, fechas, JSON, paginación), excepciones base y catálogo de códigos de mensaje | No |

Solo hay un `mvnw` por módulo. El build se lanza desde la raíz:

```bash
./mvnw clean verify      # build completo + tests + reporte JaCoCo
```

### Regla de dependencias entre módulos

```text
infrastructure --> core --> utils
```

Nunca en sentido inverso: `core` no importa clases de `infrastructure` ni librerías de
infraestructura (Spring solo como DI, no como dependencia de negocio; drivers de BD, Redis,
Pulsar, etc. quedan en `infrastructure`).

## Capas principales (paquetes reales)

```text
utils/src/main/java/co/edu/uco/crosscutting/
|-- catalog/MessageCatalogCodeEnum.java        (códigos TCH_/FUN_ del catálogo de mensajes)
|-- exceptions/                                (CrossWordsException, BusinessException, BusinessRuleException)
|   `-- enumeration/                           (ExceptionType, ExceptionLocation)
`-- helpers/                                   (UtilUUID, UtilText, UtilNumeric, UtilDate, UtilObject,
                                               UtilPagination, UtilPairKey, PropertiesHelper, EnumConstants)
    |-- json/                                  (MapperJsonObject, UtilMapperJson)
    `-- config/                                (LocalDateTimeDeserializer)

core/src/main/java/co/edu/uco/application/
|-- primaryports/                              (puertos de entrada)
|   |-- dto/                                   (message, token, page, catalog, keypair)
|   `-- facade/                                (UseCaseFacade + impl; message, token, page, catalog)
|-- secondaryports/                            (puertos de salida)
|   |-- entity/                                (MessageData, TokenData, EnvironmentData, ... *Data)
|   |-- repository/                            (MessageRepository, *CatalogRepository, token, cache...)
|   |-- catalog/                               (CatalogPort: catálogo de mensajes i18n)
|   |-- secret/                                (SecretProviderPort, CreateTokenSecretPort, EncryptTokenPort)
|   |-- vault/                                 (VaultPort)
|   |-- translation/                           (MessageTranslationPort)
|   |-- broker/                                (SendMessage)
|   |-- logging/                               (LoggingPort, LoggingPortFactory)
|   |-- presenter/                             (PresenterPort<T>)
|   |-- parameter/                             (CatalogParameterPort)
|   `-- Response.java
|-- usecase/                                   (casos de uso + implementation)
|   |-- handling/                              (Handling*Port: contratos internos que implementan los use cases)
|   |-- domain/                                (MessageDomain, TokenDomain, ... + aggregate/)
|   `-- validator/                             (CompositeValidator, reglas por feature)
|-- common/
|   |-- catalog/                               (MessageCatalog, CatalogPortStaticRef, strategy/)
|   `-- mapper/                                (DTOMapper, DataMapper, SimplePageMapper, impl/)
`-- crosscutting/
    `-- exceptions/                            (exceptiones específicas del dominio de mensajes)

infrastructure/src/main/java/co/edu/uco/
|-- init/CrossWordApplication.java             (Spring Boot main)
|-- infraestructure/
    |-- primaryadapters/                       (interfaces de controllers, interceptors, controllers)
    |   |-- MessagesController.java / TokenController.java / CreateMessageController.java / CatalogController.java
    |   |-- controller/                        (*Impl con @RestController)
    |   `-- interceptors/                      (TokenHeaderInterceptor, AcceptHeaderInterceptor)
    |-- secondaryadapters/
    |   |-- repository/
    |   |   |-- surreal/                       (model/, data/ mappers, impl/ adapters SurrealDB)
    |   |   `-- redis/                         (MessageRedis, RedisRepositoryAdapter, impl/MessageRedisAdapter)
    |   |-- catalog/RedisCatalogMessageAdapter.java
    |   |-- broker/SendBrokerMessage.java      (Pulsar)
    |   |-- encryption/JavaSecurityEncryptTokenAdapter.java
    |   |-- secrets/doppler/                   (DopplerProvider, DopplerSecretCacheService, DopplerCreateToken)
    |   |-- vault/azure/AzureKeyVaultAdapter.java
    |   |-- translation/LangChain4jMessageTranslationAdapter.java
    |   |-- presenter/
    |   |   |-- rest/HttpPresenterAdapter.java
    |   |   `-- serializer/                    (Registry, AbstractSerializer + json/xml/yaml/html/text)
    |   |-- logging/                           (Slf4jLoggingAdapter, Slf4jLoggingPortFactory)
    |   |-- parameter/dummy/CatalogParameterAdapter.java
    |   `-- ...
    `-- config/                                (SurrealDBConfig, RedisConfig, BrokerConfig, WebConfig,
                                               SerializerConfig, LoggingConfig, DopplerProperties,
                                               PulsarProperties, TranslationAiProperties, InfrastructureConstant)
```

> Nota: el paquete de infraestructura es `co.edu.uco.infraestructure` (con una "e" extra), nombre
> histórico que se mantiene tal cual en el código.

## Responsabilidad de cada capa

### core (`co.edu.uco.application`)

Núcleo de negocio, sin dependencias de frameworks de infraestructura (Spring solo como `@Component`
para DI; no hay drivers de BD, HTTP, Redis ni Pulsar):

- Puertos primarios: facades (`UseCaseFacade`) y DTOs.
- Puertos secundarios: interfaces de salida (persistencia, catálogo, secretos, traducción, broker...).
- Casos de uso y contratos `Handling*Port` que implementan.
- Dominios por operación y agregados.
- Validadores con patrón Composite y reglas individuales.
- Entidades de salida (`*Data`) hacia repositorios.
- Estrategia de catálogo con caché + BD (`MessageCatalogStrategy`).

### infrastructure (`co.edu.uco.infraestructure`)

Implementa los detalles técnicos. Sí depende de Spring Boot, SurrealDB, Redis, Pulsar, LangChain4j,
Azure Key Vault, Doppler, Loki, etc.:

- Controllers REST (interfaz + `*Impl`), que solo delegan en facades y presentan la respuesta.
- Interceptores de token y negociación de contenido.
- Adaptadores de repositorio (SurrealDB), caché (Redis), catálogo (Redis), broker (Pulsar),
  encriptación, secretos (Doppler), vault (Azure Key Vault), traducción (LangChain4j), logging
  (SLF4J/Loki), presentación (PresenterPort HTTP con serializadores).
- Configuración de beans (`config/`).
- Manejador global de errores HTTP integrado en `HttpPresenterAdapter` (`@RestControllerAdvice`).

### utils (`co.edu.uco.crosscutting`)

Código transversal compartido: helpers, jerarquía base de excepciones y el enumerador de códigos
de mensaje. Puede ser usado por core e infrastructure.

## Flujo de una petición (patrón real)

```
1. Cliente HTTP llega al Controller (interfaz con anotaciones Spring MVC + *Impl)
2. Los interceptores se ejecutan según la ruta:
     - LoggingConfig           -> crea/usurpa X-Correlation-ID, MDC, headers de respuesta
     - AcceptHeaderInterceptor -> valida el header Accept contra SerializerRegistry (406 si no soporta)
     - TokenHeaderInterceptor  -> valida el header Token (401 si falta o es invalido) y resuelve environmentId (atributo de request)
3. Controller(Impl) invoca la facade (puerto primario):  facade.execute(dto)
4. FacadeImpl delega en el contrato Handling*Port
5. UseCase (que implementa Handling*Port) ejecuta:
     - validadores (CompositeValidator -> reglas individuales)
     - lógica de dominio
     - puertos secundarios (repositorio/catálogo/caché/secretos/traducción/broker)
6. El adaptador de infrastructure implementa el puerto y toca la tecnología concreta (SurrealDB, Redis...)
7. El controller(Impl) llama al puerto PresenterPort<T> (HttpPresenterAdapter) con el resultado
8. HttpPresenterAdapter serializa el payload según el header Accept y escribe la respuesta HTTP
```

## Puertos primarios (`core/.../primaryports`)

### Facades (`facade/`)

Contrato de entrada que usan los controllers. Patrón **interfaz + `impl/`**:

```text
UseCaseFacade                                            (marcador/raíz)
facade/message/  FindMessageByCodeAndEnvironmentUseCaseFacade
                 FindMessagesByEnvironmentUsecaseFacade
                 CreateMessageUseCaseFacade
                 TranslateMessageByCodeAndEnvironmentUseCaseFacade
facade/token/    CreateTokenUseCaseFacade
                 VerifyAccessUseCaseFacade
                 FindEnvironmentIdTokenUseCaseFacade
facade/page/     SimplePageFacade
facade/catalog/  FindCatalogUseCaseFacade
```

Ejemplo real (`CreateMessageUseCaseFacadeImpl`): inyecta el `HandlingCreateMessagePort` y delega en él.
El controller nunca conoce el use case directamente.

### DTOs (`dto/`)

```text
dto/message/   MessageDTO, MessageCodeDTO, CreateMessageDTO, MessageEnvironmentDTO, TranslatedMessageDTO
dto/token/     TokenDTO, CreateTokenDTO
dto/page/      PageRequestDTO
dto/catalog/   CatalogItemDTO
dto/keypair/   KeyPairDTO
```

## Puertos secundarios (`core/.../secondaryports`) y sus adaptadores

| Puerto (core) | Implementación (infrastructure) |
|---|---|
| `CatalogPort` | `RedisCatalogMessageAdapter` (catálogo i18n Redis: code→texto) |
| `CacheMessageRepository` | `MessageRedisAdapter` (caché de lectura Redis, cache-aside) |
| `DataBaseMessageRepository` | `MessageSurrealRepositoryAdapterImpl` (SurrealDB) |
| `CreateMessageRepository` | `CreateMessageSurrealAdapter` (SurrealDB) |
| `FindTokenRepository` + `TokenRepository` | `TokenSurrealAdapter` |
| `TokenStateRepository` | `TokenStateSurrealAdapter` |
| `EnvironmentRepository` | `EnvironmentSurrealRepositoryAdapterImpl` |
| `ApplicationCatalogRepository` | `ApplicationCatalogSurrealAdapter` |
| `EnvironmentCatalogRepository` | `EnvironmentCatalogSurrealAdapter` |
| `FunctionalityCatalogRepository` | `FunctionalityCatalogSurrealAdapter` |
| `MessageTypeCatalogRepository` | `MessageTypeCatalogSurrealAdapter` |
| `MessageCategoryCatalogRepository` | `MessageCategoryCatalogSurrealAdapter` |
| `MessageStateCatalogRepository` | `MessageStateCatalogSurrealAdapter` |
| `MessageEnvironmentStateCatalogRepository` | `MessageEnvironmentStateCatalogSurrealAdapter` |
| `FindTokenCachePort` | `DopplerSecretCacheService` (Caffeine, TTL 15 min) |
| `SecretProviderPort` | `DopplerProvider` (OkHttp GET Doppler API) |
| `CreateTokenSecretPort` | `DopplerCreateToken` (OkHttp POST Doppler API) |
| `VaultPort` | `AzureKeyVaultAdapter` (SecretClient + managed identity) |
| `EncryptTokenPort` | `JavaSecurityEncryptTokenAdapter` (RSA 2048, OAEP SHA-256) |
| `MessageTranslationPort` | `LangChain4jMessageTranslationAdapter` (Ollama / OpenAI) |
| `SendMessage` (broker) | `SendBrokerMessage` (Pulsar producer) |
| `PresenterPort<T>` | `HttpPresenterAdapter` (REST + `@RestControllerAdvice`) |
| `LoggingPortFactory` / `LoggingPort` | `Slf4jLoggingPortFactory` / `Slf4jLoggingAdapter` |
| `CatalogParameterPort` | `CatalogParameterAdapter` (dummy, lee `parameter.properties`) |

### Entidades de salida (`secondaryports/entity`)

```text
MessageData, MessageEnvironmentData, MessageTranslationRequestData, MessageTranslationResponseData,
MessageTypeData, MessageCategoryData, StatusMessageData, MessageEnvironmentStateData,
EnvironmentData, EnvironmentType, ApplicationData, FunctionalityData, TokenData, StatusTokenData
```

Son los modelos que viajan entre core y los adaptadores (no son entidades JPA ni Spring Data).

### Otros puertos de salida

```text
secondaryports/Response.java                          (respuesta base: datos + mensajes)
secondaryports/GenericPort.java
secondaryports/repository/SimplePage, SimplePageRequest (paginación desacoplada de Spring Data)
secondaryports/repository/token/PageBuilder            (construye SimplePage)
secondaryports/repository/token/FindTokenCachePort
```

## Casos de uso (`core/.../usecase`) y contratos Handling

Los casos de uso implementan contratos `Handling*Port` (paquete `usecase/handling/`), que las
facades consumen. Lista real:

```text
usecase/
|-- CreateMessageUseCase.java                       implements HandlingCreateMessagePort
|-- FindMessageByEnvironmentUseCase.java            implements HandlingFindMessageEnvironmentPort
|-- FindMessageByCodeAndEnvironmentUseCase.java     implements HandlingFindMessageByCodeAndEnvironmentPort
|-- TranslateMessageByCodeAndEnvironmentUseCase.java implements HandlingTranslateMessageByCodeAndEnvironmentPort
|-- CreateTokenUseCase.java                         implements HandlingCreateTokenPort
|-- VerifyAccessUseCase.java                        implements HandlingVerifyAccessPort
|-- FindEnvironmentIdTokenUseCase.java              implements HandlingFindEnvironmentIdTokenPort
|-- RevokeTokenUseCase.java                         implements HandlingRevokeTokenPort
|-- FindCatalogUseCase.java                         implements HandlingFindCatalogPort
```

Ejemplo estructural (`CreateMessageUseCase`): valida con `CreateMessageCompositeValidator`,
construye un `MessageData` con `UtilUUID.getNewUUID()`, persiste vía `CreateMessageRepository`
(SurrealDB) y loguea. Errores `CrossWordsException` se re-lanzan; cualquier otro se envuelve.

## Dominios y agregados (`usecase/domain`)

```text
usecase/domain/
|-- MessageDomain, MessageCodeDomain, MessageTypeDomain, MessageCategoryDomain,
|   MessageStatusDomain, FunctionalityDomain, TokenDomain
`-- aggregate/
    |-- AggregateRoot.java, Entity.java
    |-- entities/          MessageEntity, MessageTypeEntity, MessageCategoryEntity,
    |                       MessageStatusEntity, FunctionalityEntity
    `-- entities/valueobject/  TitleVO, ContentVO, ListMessageAggregateRoot
```

Patrón: clases `final` con `@Getter`, setters que normalizan con helpers de `utils`
(`UtilUUID.getDefaultUUID`, `UtilText.trim`, `getDefaultIsNullObject`). Los value objects
(TitleVO/ContentVO) encapsulan las reglas de longitud y vacío del título/contenido del mensaje.

## Validadores (`usecase/validator`) — patrón Composite

```text
Validator.java                     (interfaz raíz: trim/validate)
CompositeValidator.java            (agrupa reglas y las ejecuta en orden)
validator/page/                    PageRequestDTOValidator, PageRequestDTOCompositeValidator,
                                   SimplePageRequestValidator(+Composite), PageNumberValidator,
                                   PageSizeValidator, SortColumnValidator, SortDirectionValidator,
                                   PageRequestTypeValidator, PageRequestRangeValidator
validator/message/                 FindMessageCodeValidator, TargetLanguageValidator,
                                   ListMessageValidator, CreateMessageCompositeValidator
validator/token/                   DateValidValidator, CreateTokenCompositeValidator
validator/environment/             EnvironmentExistValidator, ApplicationBelongsEnvironmentValidator
validator/impl/                    UUIDValidator, ExpirationDateValidator
```

Las reglas individuales se componen en validadores compuestos (`*CompositeValidator`) que el use
case invoca con `validate(dto)`. El caso de uso no conoce el detalle de cada regla.

## Estrategia de catálogo de mensajes (`common/catalog`)

Patrón **Strategy cache + database** para resolver tanto el catálogo i18n (códigos TCH_/FUN_) como
los mensajes de negocio:

```text
common/catalog/
|-- MessageCatalog.java              (record: code, title, content, type, category)
|-- CatalogPortStaticRef.java        (holder estático del CatalogPort, poblado en @PostConstruct)
`-- strategy/
    |-- MessageCatalogStrategy.java  (orquesta cache + database)
    |-- MessageCatalog.java
    |-- cache/                       (CacheCatalog -> CacheMessageCatalog: mensajes por environment)
    `-- database/                    (DatabaseCatalog -> DatabaseMessageCatalog: mensajes por environment)
```

Comportamiento de `MessageCatalogStrategy`:

- `getMessageByCodeAndEnvironment`: busca en caché (Redis); si no hay, consulta BD (SurrealDB) y
  rellena la caché.
- `getMessagesWithEnvironment`: si no hay caché o el tamaño difiere de la BD, refresca la caché;
  si la BD no devuelve datos lanza `BusinessException`.
- `getSystemMessageContent`: resuelve textos del catálogo de sistema (`TCH_*` / `FUN_*`).

`CatalogPortStaticRef` permite que clases del dominio sin Spring (excepciones, VOs, serializadores)
resuelvan textos: `CatalogPortStaticRef.getTitle(FUN_022.getCode())`.

## Catálogo de códigos (`utils/.../catalog/MessageCatalogCodeEnum`)

Enumerador centralizado de códigos del catálogo de mensajes (type-safe, sin strings literales):

```text
TCH_001..TCH_071   -> mensajes técnicos (infraestructura, errores de sistema)
FUN_001..FUN_144   -> mensajes funcionales (negocio, validaciones, nombres de campos)
```

Cada constante expone `getCode()` y se resuelve contra `CatalogPort` / `CatalogPortStaticRef`.
El contenido textual se precarga en Redis (script `deployment/docker/scripts/redis/CatalogMessageInit.sh`)
y en SurrealDB (`surreal-init.surql`, `surreal-seed.dev.surql`).

## Persistencia — SurrealDB (`secondaryadapters/repository/surreal`)

- Driver síncrono oficial `com.surrealdb.Surreal` (bean en `SurrealDBConfig`, WebSocket RPC).
- No hay ORM ni JPA: las consultas son **SurrealQL** construidas con helpers seguros:
    - `SurrealCatalogSupport` (clase abstracta): `queryAll/query/queryOne`, `RowMapper<T>`,
      `extractIdAsUUID`, `cleanThingId`, `stringOf`.
    - `SurrealQLUtil`: `quote()`, `recordIdLiteral()`, `datetime()` (no se usa `queryBind` porque el
      binding nativo no está disponible en este SDK).
- **Modelos** `model/`: `MessageSurrealModel`, `EnvironmentSurrealModel`, `TokenSurrealModel`,
  `StatusTokenSurrealModel` (POJOs con normalization en setters).
- **Mappers** `data/`: interfaz `DataMapper<D,A>` (`mapperData`/`mapperModel`).
  `MessageSurrealMapper`, `EnvironmentSurrealMapper` (vía ModelMapper); `TokenSurrealMapper`,
  `TokenStateSurrealMapper` (manual); `MessageDataCacheMapper` (MessageData ↔ MessageRedis para caché).
- **CQRS + Event Sourcing de proyecciones**: `SurrealDomainEventProjectionConsumer`
  (`@Scheduled`, cada ~2s, lote de 100) consume la tabla `domain_events` y **proyecta read models**
  denormalizados:
    - `application_document`, `environment_document`,
    - `message_data_collection` (mensaje denormalizado con catálogos),
    - `message_environment_readmodel` (mensaje + ambiente).
    - Deletes detectados por sufijo `_DELETED`; marca `projected`/`failed` + `projection_error`.
- Tablas/colecciones (referencia en `InfrastructureConstant`): `token`, `token_state`,
  `message_environment`, `environment`, `application`, `message`, `message_type`,
  `message_category`, `message_state`, `message_environment_state`, `functionality`,
  `environment_type`, `domain_events`, y los read models.
- No existen tablas, entidades de salida ni repositorios persistentes `parameter` o `represent_parameter`.
  El puerto `CatalogParameterPort` y `CatalogParameterAdapter` se conservan porque resuelven configuracion
  tecnica desde `parameter.properties`, no parametros persistidos del dominio.
- Los agregados fuente no persisten los antiguos campos auxiliares `version` ni `seed_key`. Las versiones
  tecnicas de `domain_events` y de las proyecciones se conservan para ordenar eventos y read models.
- Requiere SurrealDB montado con el esquema de `deployment/docker/scripts/surreal-init.surql`.
- El esquema actualizado solo se soporta en instalaciones nuevas. No hay migracion de volumenes existentes;
  para adoptarlo se debe crear un volumen de SurrealDB nuevo.

### Política temporal UTC

- La aplicación utiliza **UTC (`UTC+00:00`)** como zona horaria única para lógica de negocio,
  serialización, persistencia y logs. `CrossWordApplication` fija la zona por defecto antes de iniciar
  Spring; el `Dockerfile` refuerza esta política con `TZ=UTC` y `-Duser.timezone=UTC`, y los Compose
  local y Azure declaran `TZ: UTC` para el contenedor de la aplicación.
- Mientras el modelo mantenga campos `LocalDateTime`, su contrato interno es representar siempre una
  fecha y hora ya normalizada a UTC. `UtilDate.nowUtc()` reemplaza el antiguo valor estático de hora y
  `TimeConfig` expone un `Clock.systemUTC()` inyectable para lógica dependiente del tiempo y pruebas
  deterministas.
- Las entradas de API conservan compatibilidad: una fecha sin offset, como
  `2026-12-31T23:59:59`, se interpreta como UTC; una fecha con `Z` o con offset, como
  `2026-12-31T18:59:59-05:00`, se convierte al instante UTC equivalente. OpenAPI documenta estos
  campos como `date-time` RFC 3339.
- `SurrealQLUtil.datetime()` escribe fechas normalizadas como literales RFC 3339 terminados en `Z`.
  Los valores leídos mediante el SDK de SurrealDB y los timestamps copiados a proyecciones se convierten
  a UTC antes de usarse. Los campos generados por el esquema con `time::now()` continúan siendo la fuente
  de los timestamps de auditoría.
- En SurrealDB, `created_at` se asigna con la fecha UTC actual al crear el registro y permanece inmutable;
  `updated_at` se renueva con la fecha UTC actual en cada escritura. Los registros del seed comparten el
  instante UTC capturado al iniciar su recreación, sin fechas fijas de ejemplo.
- Este cambio aplica a escrituras nuevas. Los registros históricos no se desplazan automáticamente,
  porque un valor antiguo sin información confiable sobre su zona de origen no puede corregirse de forma
  segura mediante una migración global.
- `logback-spring.xml` y `logback-azure.xml` emiten timestamps ISO-8601 con offset UTC (`Z`). La rotación
  diaria de archivos también ocurre a medianoche UTC. El horario de apagado automático de la VM en Azure
  permanece en hora de Colombia porque es una programación operativa independiente de la aplicación.

## Caché — Redis (`secondaryadapters/repository/redis` y `catalog`)

- **Caché de lectura de mensajes** (`MessageRedisAdapter`, puerto `CacheMessageRepository`):
  entidades `@RedisHash("Message")` (`MessageRedis`) con `@Indexed environmentId`. Cache-aside:
  el caso de uso consulta caché antes de tocar SurrealDB y la escribe tras un miss.
  Habilitado con `@EnableRedisRepositories` (`RedisConfig`), `RedisTemplate` + `Jackson2JsonRedisSerializer`.
- **Catálogo i18n** (`RedisCatalogMessageAdapter`, puerto `CatalogPort`): cada código TCH_/FUN_ es un
  hash Redis (`opsForHash`) con campos code/title/content/type/category. Se auto-registra en
  `CatalogPortStaticRef` en `@PostConstruct`.

## Broker — Apache Pulsar (`secondaryadapters/broker`)

- `SendBrokerMessage` (puerto `SendMessage`): con el `PulsarClient` del bean `BrokerConfig`
  (`pulsar.service-url`) crea un `Producer<String>` (`Schema.STRING`), serializa un
  `MessageCodeDomain` a JSON (`UtilMapperJson`) y publica en `pulsar.topic-name`.
- Error → `CrossWordsException` técnica (TCH_038/TCH_002/TCH_039).

## Tokens: encriptación, secretos y verificación

- **Encriptación** (`JavaSecurityEncryptTokenAdapter`, puerto `EncryptTokenPort`):
    - `generateKeys()` → RSA 2048 bits.
    - `generateSignature(data, publicKey)` → cifra con `RSA/ECB/OAEPWithSHA-256AndMGF1Padding` (Base64).
    - `access(privateKey, signature, secretName)` → descifra con la clave privada y compara con secretName.
- **Secretos Doppler**:
    - `DopplerProvider` (puerto `SecretProviderPort`): GET a la API de Doppler (`Authorization: Bearer`).
    - `DopplerSecretCacheService` (puerto `FindTokenCachePort`): caché Caffeine 15 min / 500 entradas.
    - `DopplerCreateToken` (puerto `CreateTokenSecretPort`): POST del change request a Doppler + invalida caché.
- **Azure Key Vault** (`AzureKeyVaultAdapter`, puerto `VaultPort`): `SecretClient` con
  `DefaultAzureCredentialBuilder`; rechaza secretos deshabilitados; errores mapeados a TCH_047..TCH_050.

## Traducción asistida por IA (`secondaryadapters/translation`)

`LangChain4jMessageTranslationAdapter` (puerto `MessageTranslationPort`):

- Instancia el modelo **lazy** según `translation.ai.provider`:
    - **ollama** (default): `OllamaChatModel` con `baseUrl` (`http://ollama:11434` en prod) y
      `modelName` (`llama3.2`).
    - **openai**: `OpenAiChatModel` con apiKey.
- Usa `ResponseFormat` tipo JSON schema (`translatedTitle`/`translatedContent`).
- Prompt armado con el catálogo (`FUN_054`). Devuelve `MessageTranslationResponseData` con
  proveedor, modelo y latencia. Si `translation.ai.enabled=false` → `FUN_046`.

## Presentación de respuestas (`secondaryadapters/presenter`)

Respuesta base `Response` (datos + lista de mensajes) serializada según el header `Accept`:

| Serializer (bean en `SerializerConfig`) | Content-Type | Detalle |
|---|---|---|
| `JsonSerializer` (default) | `application/json` | `ObjectMapper` + `JavaTimeModule` |
| `XMLSerializer` | `application/xml` | `XmlMapper` + `JavaTimeModule` |
| `YamlSerializer` | `application/yaml` | `YAMLMapper` + `JavaTimeModule` |
| `HTMLSerializer` | `text/html` | JSON pretty dentro de `<pre>` |
| `PlainTextSerializer` | `text/plain` | `data.toString()` |

- `SerializerRegistry` resuelve el serializer por media type; si ninguno soporta → 406
  (`AcceptHeaderInterceptor`) o excepción técnica TCH_017.
- `HttpPresenterAdapter<T>` implementa `PresenterPort<T>`: `presentRestSuccess(...)` escribe la
  respuesta (200) serializada. Además, la misma clase es `@RestControllerAdvice` (ver errores).

## Interceptores y seguridad (`primaryadapters/interceptors` + `config/WebConfig`)

- `LoggingConfig` (`HandlerInterceptor` global): X-Correlation-ID, MDC (URI, método, query, sesión),
  headers de respuesta `X-Correlation-ID`, `TS`, `THREAD`, `APP=MessageUcoLab`. Limpia el MDC al final.
- `AcceptHeaderInterceptor` (`/messageucolab/v1/**`, sin swagger): valida el header Accept.
- `TokenHeaderInterceptor` (rutas de mensajes/token): exige header `Token` (401 si falta/vacío o
  token inválido), ejecuta `VerifyAccessUseCaseFacade` y `FindEnvironmentIdTokenUseCaseFacade` para
  resolver el `environmentId` y dejarlo como atributo de request.

Los controllers de catálogo están exentos del token (solo pasan por Accept y logging).

## Manejo de errores

### Jerarquía base (`utils/.../exceptions`)

```text
CrossWordsException extends RuntimeException
  userMessage, technicalMessage, rootException, type (ExceptionType), location (ExceptionLocation)
  |-- BusinessException      (type BUSINESS, location APPLICATION)   + buildUserException / buildTechnicalException
  |-- BusinessRuleException  (type BUSINESS_RULE, location APPLICATION)
  |     |-- UnauthorizedException (HTTP 401)
  |     `-- ForbiddenException    (HTTP 403)
  |-- NotFoundException      (HTTP 404)
  `-- ConflictException      (HTTP 409)
```

Enums: `ExceptionType{ TECHNICAL, BUSINESS, BUSINESS_RULE, GENERAL }`,
`ExceptionLocation{ INFRASTRUCTURE, APPLICATION, GENERAL }`.

### Excepciones específicas del dominio (`core/.../application/crosscutting/exceptions`)

```text
MessageNotFoundException, MessageKeyCanNotBeNullException, MessageKeyCanNotBeEmptyException,
ContentCanNotBeEmptyException, TitleCanNotBeEmptyException, SizeTitleLessThanTenException,
SizeTitleMoreThanFiftyException, SizeContentLessThanTenException, SizeContentMoreThanOneHundred
```

### Traducción a HTTP

**No existe una clase `GlobalExceptionHandler`.** La traducción la hace `HttpPresenterAdapter`
(`@RestControllerAdvice` en `secondaryadapters/presenter/rest`):

- `@ExceptionHandler(CrossWordsException.class)` → `Response([], [mensaje])` con el estado HTTP
  definido por la excepción (usa el serializer según Accept; si no hay `userMessage` usa FUN_023).
- `@ExceptionHandler(Exception.class)` → mensaje genérico con **HTTP 500**.
- Cuerpo ilegible → **HTTP 400**; media type no aceptable → **HTTP 406**.

Además, los interceptores escriben sus propios errores (401 token, 406 Accept).

## Configuración y arranque

- `CrossWordApplication` (`co.edu.uco.init`): `@SpringBootApplication(exclude = DataSourceAutoConfiguration)`,
  `@ComponentScan("co.edu.uco")`, `@EnableScheduling` (proyecciones CQRS). Puerto **8085**.
- `InfrastructureConstant`: constantes de colecciones, campos, headers, algoritmos, caché,
  patrones de interceptor y prefijos de properties.
- Principales `@ConfigurationProperties`:
    - `surreal` (`SurrealDBProperties`): host, port, user, password, namespace, database, maxConnections.
    - `pulsar` (`PulsarProperties`): serviceUrl, topicName.
    - `doppler` (`DopplerProperties`): token, request, urlConfigSecretsPost/Get.
    - `translation.ai` (`TranslationAiProperties`): enabled, provider, apiKey, baseUrl, modelName,
      temperature, maxRetries, timeoutSeconds.
- Configuraciones: `SurrealDBConfig` (cliente `Surreal`), `RedisConfig` (`@EnableRedisRepositories` +
  `RedisTemplate`), `BrokerConfig` (PulsarClient), `SerializerConfig` (5 serializers),
  `WebConfig` (interceptores), `LoggingConfig` (correlación).
- Recursos: `application.properties` (todo por variables de entorno: `SURREALDB*`, `REDIS*`,
  `PULSARURL`, `DOPPLERTOKEN`, `AZURE_KEYVAULT_UCOLAB_ENDPOINT`, `TRANSLATION_AI_*`),
  `application-azure.properties` (perfil `azure`: managed identity, logback azure),
  `logback-spring.xml` (appenders CONSOLE JSON / ROLLING / LOKI), `static/openapi.yaml` (Swagger).

## Endpoints reales

Base de mensajes: `crosswords.api.path.messages=/messageucolab/v1/messages`. Todas las rutas
soportan respuesta en JSON, YAML, XML, text/plain y text/html según el header `Accept`.

| Método | Ruta | Controlador | Token header |
|---|---|---|---|
| POST | `/messageucolab/v1/application` | CreateApplicationControllerImpl (crea aplicación y ambientes por defecto) | Sí |
| POST | `/messageucolab/v1/application/{id}/token` | TokenControllerImpl (CrearToken) | No |
| GET | `/messageucolab/v1/messages?page&size&sort&columnSort` | MessagesControllerImpl (listar por ambiente) | Sí |
| GET | `/messageucolab/v1/messages?code={messageCode}` | MessagesControllerImpl (por código y ambiente) | Sí |
| POST | `/messageucolab/v1/messages/{messageCode}/translations` | MessagesControllerImpl (traducir) | Sí |
| POST | `/messageucolab/v1/messages` | CreateMessageControllerImpl | Sí |
| GET | `/messageucolab/v1/catalog/applications` | CatalogControllerImpl | No |
| GET | `/messageucolab/v1/catalog/applications/{applicationId}/environments` | CatalogControllerImpl | No |
| GET | `/messageucolab/v1/catalog/applications/{applicationId}/functionalities` | CatalogControllerImpl | No |
| GET | `/messageucolab/v1/catalog/message-types` | CatalogControllerImpl | No |
| GET | `/messageucolab/v1/catalog/message-categories` | CatalogControllerImpl | No |
| GET | `/messageucolab/v1/catalog/message-states` | CatalogControllerImpl | No |
| GET | `/messageucolab/v1/catalog/message-environment-states` | CatalogControllerImpl | No |

Swagger: `http://localhost:8085/swagger-ui.html` · Health: `http://localhost:8085/actuator/health` ·
Métricas: `http://localhost:8085/actuator/prometheus`.

## Flujo real de una consulta (lectura con caché)

```text
1. GET /messageucolab/v1/messages?code={messageCode} con header Token
2. TokenHeaderInterceptor valida el token y resuelve environmentId (atributo de request)
3. MessagesControllerImpl -> FindMessageByCodeAndEnvironmentUseCaseFacade.execute(code, environmentId)
4. Facade -> HandlingFindMessageByCodeAndEnvironmentPort -> FindMessageByCodeAndEnvironmentUseCase
5. UseCase usa MessageCatalogStrategy.getMessageByCodeAndEnvironment:
     a. cacheCatalog.getMessageByCodeAndEnvironment  -> MessageRedisAdapter (Redis)  [HIT -> responde]
     b. si miss -> databaseCatalog.getMessageByCodeAndEnvironment -> MessageSurrealRepositoryAdapterImpl
     c. si se recupera de BD -> rellena caché (cache-aside)
6. UseCase mapea MessageData -> MessageDTO (MessageDTOMapper) y retorna Optional/throw MessageNotFound
7. MessagesControllerImpl -> PresenterPort<MessageDTO> (HttpPresenterAdapter)
8. SerializerRegistry elige serializer según Accept y escribe la respuesta (200)
```

## Flujo real de una escritura (crear mensaje)

```text
1. POST /messageucolab/v1/messages (body CreateMessageDTO, header Token o Authorization Bearer)
2. CreateMessageControllerImpl -> CreateMessageUseCaseFacade.execute(dto)
3. Facade -> HandlingCreateMessagePort -> CreateMessageUseCase
4. UseCase resuelve el ambiente autenticado y deriva de este el `applicationId`
5. UseCase valida con CreateMessageCompositeValidator, incluida la pertenencia de la funcionalidad
6. UseCase construye MessageData (id = UtilUUID.getNewUUID(), compone type/category/status/functionality) e invoca
   CreateMessageRepository.createMessage(messageData, environmentId, activeMessageEnvironmentStateId)
7. CreateMessageSurrealAdapter hace UPSERT en SurrealDB (tablas message + message_environment
   con record IDs a message_type/message_category/message_state/application/functionality/environment)
8. El evento de dominio queda en domain_events; SurrealDomainEventProjectionConsumer (2s) lo
   proyecta a los read models denormalizados de lectura
9. CreateMessageControllerImpl presenta "Mensaje creado exitosamente"
```

## Flujo real de tokens

```text
1. POST /messageucolab/v1/application/{id}/token -> TokenControllerImpl
2. CreateTokenUseCaseFacade.execute(tokenDTO, id)
3. CreateTokenUseCase:
     - genera par de llaves RSA (JavaSecurityEncryptTokenAdapter.generateKeys)
     - crea el secreto del token en Doppler (DopplerCreateToken)
     - persiste el token en SurrealDB (TokenSurrealAdapter: tabla token + token_state)
     - construye la firma/signature con la clave pública y la devuelve
4. TokenControllerImpl presenta el token generado (String)
```

La verificación en cada request: `VerifyAccessUseCase` (estado y expiración) +
`FindEnvironmentIdTokenUseCase` (resuelve el `environmentId`) desde el interceptor.

## Observabilidad y despliegue

### Local (`deployment/docker`)

```bash
cd deployment/docker
doppler run -- docker compose up -d --build
```

El directorio debe tener configurado el proyecto y ambiente de Doppler correspondientes. Doppler aporta
las variables que Docker Compose necesita para crear los servicios y las credenciales de acceso a Azure;
la aplicación obtiene el resto de su configuración desde Azure Key Vault durante el arranque.

Servicios: SurrealDB (`surrealdb/surrealdb:v3.1.4`, con `surreal-init.surql` y seed),
Redis (con seed del catálogo), Pulsar 3.2.2, Kong Gateway 3.5 (declarativo), Ollama (con pull del
modelo), Grafana, Loki, OpenTelemetry Collector y Prometheus.

### Azure (`deployment/azure`, `infrastructure/azure`, `docs/azure-deployment.md`)

- Bicep (`infrastructure/azure/main.bicep`): ACR, VM Standard_B2s, Managed Identity, NSG, Key Vault.
- `deployment/azure/docker-compose.yml`: stack mínimo (Redis 7.4, SurrealDB, Pulsar, app con perfil
  `azure`, Kong 3.5 como entrada pública) sin observabilidad.
- `deployment/azure/deploy.sh`: login con Managed Identity, pull de la imagen desde ACR, `doppler run`,
  healthcheck por Kong y rollback automático si la app no responde.
- Pipelines Azure DevOps: CI (`azure-pipelines.yml`, trigger en `develop`, Maven Wrapper 3.9.9 + Temurin 25,
  `./mvnw clean verify`, publish de resultados de test, build de imagen) y CD manual
  (`azure-deploy-pipeline.yml`, stages DEPLOY / STOP).
- `Dockerfile` multi-etapa: build `eclipse-temurin:25-jdk-noble` con Maven Wrapper, runtime
  `eclipse-temurin:25-jre-noble`, healthcheck `/actuator/health`, EXPOSE 8085.

## Tests y cobertura

- JUnit Jupiter 6 + Mockito + AssertJ, gestionados por Boot. Los tests de core son unitarios; los de infrastructure también
  cubren adaptadores, serializers, mappers, interceptors, controllers y el projection consumer.
- Migración verificada con Temurin 25.0.4.1: **2049 tests en verde**, sin fallos, errores ni omitidos
  en el build Docker con `clean verify` — utils 214, core 1216, infrastructure 619.
- El build Docker incluye el seed de SurrealDB solo en la etapa de compilación para ejecutar
  también sus 15 pruebas de contrato estático; no importa el seed ni altera bases de datos.
- Cobertura JaCoCo del build local Java 25 (líneas / ramas): utils 96.68% / 96.97%,
  core 97.97% / 98.30%, infrastructure 89.50% / 81.83%. Todos cumplen el mínimo de 80% por módulo.
- JaCoCo 0.8.15 configurado en el `pom.xml` raíz: agente en `prepare-agent` y reporte en `verify`
  (`infrastructure/target/site/jacoco/index.html`).
- Comando: `./mvnw clean verify` (usa Maven 3.9.9 vía wrapper; el JDK de build debe ser **25**).

> Compilar y probar con Java 25 (`JAVA_HOME` apuntando a un JDK 25). Mockito/Byte Buddy se
> gestionan mediante Spring Boot; no utilizar el modo experimental de Byte Buddy.

- Arranque local Java 25 verificado con Doppler (`messageucolab` / `dev`), conexión a SurrealDB
  y respuestas HTTP 200 en health, OpenAPI, Swagger y Prometheus. Esto no sustituye la regresión
  completa de flujos de negocio ni el Quality Gate remoto de SonarCloud.

## Reglas que deben respetarse

```text
infrastructure --> core --> utils
```

Evitar:

```text
core --> infrastructure
core --> SurrealDB, Redis, Pulsar, HTTP, LangChain4j, Azure, Doppler (solo puertos)
domain --> Spring (solo DI, sin anotaciones de framework de negocio)
controllers --> lógica de negocio (los controllers solo delegan en facades y presentan)
```

Permitido:

```text
Controller(Impl) -> UseCaseFacade -> Handling*Port -> UseCase -> SecondaryPort -> Adapter
Adapter SurrealDB -> SurrealQL (SurrealQLUtil / SurrealCatalogSupport)
Adapter Redis -> RedisTemplate / Spring Data Redis
Adapter Pulsar -> PulsarClient
UseCase -> MessageCatalogStrategy (cache + database) -> CacheMessageRepository / DataBaseMessageRepository
HttpPresenterAdapter (@RestControllerAdvice) -> SerializerRegistry -> SerializerType
```

## Guía práctica para añadir una feature nueva

1. **DTO** en `core/.../primaryports/dto/<feature>/` (request/response).
2. **Puerto primario** en `core/.../primaryports/facade/<feature>/` (interfaz + `impl/`).
3. **Contrato interno** en `core/.../usecase/handling/` (`HandlingXxxPort`).
4. **Caso de uso** en `core/.../usecase/` que implementa el contrato.
5. **Validación** en `core/.../usecase/validator/<feature>/` siguiendo el patrón Composite.
6. **Puerto secundario** en `core/.../secondaryports/...` (repository/catalog/secret/...).
7. **Adaptador** en `infrastructure/.../secondaryadapters/...` que implementa el puerto.
8. **Controller** en `infrastructure/.../primaryadapters/` (interfaz + `controller/*Impl`)
   y registro de rutas/security en `config/WebConfig` si aplica.
9. **Serialización**: el formato de salida se resuelve solo con `PresenterPort` + `SerializerRegistry`
   (no hardcodear Content-Type por endpoint).
10. **Tests unitarios** en el módulo correspondiente (mismo paquete que la clase bajo prueba)
    y mantener cobertura JaCoCo.

La unidad de trabajo más sana es una feature completa en vertical:

```text
Controller -> Facade -> HandlingPort -> UseCase -> Validator -> Port -> Adapter -> Tecnología
```
