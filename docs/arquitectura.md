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
| Eventos | Pulsar 3.2.2 | Eventos de dominio y proyecciones |
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

SSE ofrece actualizaciones administrativas por aplicación. Su implementación en memoria tiene límites explícitos:

- Los eventos no son duraderos.
- Una reconexión no recupera eventos perdidos.
- Varias réplicas no comparten automáticamente el flujo.
- No reemplaza Pulsar ni constituye una garantía de entrega.

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
