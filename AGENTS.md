# AGENTS.md

## TL;DR
- Monorepo Maven multi-módulo: `core` (dominio/casos de uso) → `utils` (helpers) → `infrastructure` (adaptadores + app ejecutable).
- Arquitectura hexagonal por feature. Dependencias solo hacia abajo: `infrastructure → core → utils`. Nunca al revés.
- Usar `./mvnw`, no `mvn`, para garantizar Maven 3.9.9.
- Antes de correr la app: levantar infraestructura con `docker compose up -d` en `deployment/docker`.
- No hay linter/formatter configurado — no asumas reglas de estilo automáticas.
- Todo cambio debe respetar Clean/Hexagonal Architecture y pasar el Quality Gate de SonarCloud (ver sección "Clean Architecture y SonarCloud").
- Tests: no crear, modificar ni ejecutar pruebas automáticamente después de un cambio. Preguntar primero si el usuario autoriza invocar `unit-test-agent`; una petición explícita de crear, modificar o ejecutar pruebas ya cuenta como autorización.
- Crear/modificar pruebas SIEMPRE mediante `unit-test-agent` y auditarlas allí con `unit-test-validator`. No escribir tests en la conversación principal (quema tokens de la cuota principal).

## Qué es este proyecto
API de mensajes multientorno con catálogo jerárquico (Organización → Aplicación → Módulo → Funcionalidad → Parámetro), soporte de tokens, traducción asistida por IA, caché Redis y eventos SSE en memoria. Diseñada para desplegarse en Azure con secretos en Key Vault/Doppler y observabilidad vía Grafana/Prometheus/Loki.

### Propósito del producto: Building Block
- Este proyecto es un **Building Block de gestión y distribución de mensajes**, consumido por otras aplicaciones mediante su API para obtener y mostrar mensajes y sus traducciones.
- Tendrá un **minifront administrativo** para gestionar aplicaciones, catálogos y crear o mantener mensajes y traducciones.
- Distinguir siempre el minifront administrativo de las aplicaciones consumidoras: la navegación, las pantallas y la selección de idioma de los usuarios finales pertenecen a estas últimas.
- El idioma base registrado para una aplicación corresponde a esa aplicación consumidora. La traducción dinámica debe diseñarse como una capacidad reutilizable de la API, respetando el contexto de aplicación y ambiente y la arquitectura hexagonal.
- Las propuestas de consulta o preparación de traducciones no deben asumir conocimiento de las pantallas del consumidor; pueden definir contratos por aplicación, módulo, funcionalidad o conjunto de códigos de mensajes.

## Estructura del monorepo
| Módulo | Contenido | Genera jar ejecutable |
|---|---|---|
| `core` | Casos de uso, puertos (interfaces), dominios, validadores, entidades de salida | No |
| `infrastructure` | Adaptadores primarios (controllers REST) y secundarios (SurrealDB, Redis, Pulsar, LangChain4j, Azure Key Vault, Doppler, Loki). Contiene `CrossWordApplication` | Sí |
| `utils` | Helpers transversales: UUID, texto, fechas, JSON, excepciones base, catálogos de mensajes | No |

Flujo de una petición: `Controller → Interactor → UseCase → Repository port → Repository adapter → SurrealDB`.

## Stack y dependencias clave (infrastructure)
Java 17, Spring Boot 3.2.5, SurrealDB 2.1.1 (BD principal, no SQL tradicional), Apache Pulsar 3.2.2, Spring Data Redis, Caffeine 3.1.8, LangChain4j 1.15.1 (Ollama local por defecto / OpenAI), loki-logback-appender 1.6.0, Micrometer Prometheus, SpringDoc OpenAPI, Azure Key Vault, Doppler client.

## Setup y build
Requiere JDK 17 y Maven 3.9.x. Usar el wrapper `./mvnw` en cada módulo en vez de `mvn` instalado globalmente.

```bash
git clone <repo>
cd MessageUcoLab

# Build completo + tests + cobertura JaCoCo
./mvnw clean verify

# Infraestructura de soporte (SurrealDB, Redis, etc.)
cd deployment/docker
docker compose up -d
cd ../..

# Ejecutar la app (requiere infraestructura corriendo)
./mvnw spring-boot:run -pl infrastructure
```

- Puerto por defecto: `8085`
- Swagger UI: `http://localhost:8085/swagger-ui.html`
- Actuator health: `http://localhost:8085/actuator/health`
- Prometheus: `http://localhost:8085/actuator/prometheus`

## Testing

### Autorización obligatoria para invocar el agente de pruebas

- Después de realizar un cambio de producción, no crear ni ejecutar pruebas automáticamente. Preguntar al usuario si autoriza invocar `unit-test-agent` y esperar su respuesta.
- Si el usuario pide explícitamente crear, modificar, auditar o ejecutar pruebas, esa petición constituye autorización para invocar `unit-test-agent`; no es necesario volver a preguntar.
- La autorización corresponde a la ejecución completa de `unit-test-agent`. Una vez concedida, el subagente puede crear o modificar pruebas, auditarlas y ejecutar los comandos Maven necesarios sin solicitar permisos adicionales por cada comando.
- Si una verificación autorizada falla y se corrige la causa dentro de la misma tarea, `unit-test-agent` tiene autorización permanente para volver a ejecutar `./mvnw clean verify` sin preguntar de nuevo. Aplica también al reanudar el subagente para cerrar una validación pendiente. No dar por validada una corrección hasta confirmar una verificación exitosa; si existe un bloqueo que no se puede resolver dentro del alcance, reportarlo.
- El agente principal no debe ejecutar pruebas ni comandos que las incluyan indirectamente, como `./mvnw clean verify`, como consecuencia automática de una tarea de producción.
- Si el usuario no autoriza `unit-test-agent`, finalizar la tarea de producción e indicar que no se crearon ni ejecutaron pruebas.

```bash
./mvnw test            # Unitarios e integración (JUnit 5)
./mvnw clean verify    # Build + tests + reporte JaCoCo
```
Reporte de cobertura HTML en `infrastructure/target/site/jacoco/index.html`.

No hay linter/formatter configurado actualmente — no introducir reglas de estilo automáticas ni asumir que existen; seguir el estilo del código circundante.

### Delegación obligatoria de tests
Aplica en todas las sesiones, incluso nuevas:

1. **Antes de delegar** → solicitar autorización para invocar `unit-test-agent`, salvo que el usuario ya haya pedido explícitamente trabajar con pruebas.
2. **Crear o modificar pruebas unitarias** → delegar SIEMPRE al agente `unit-test-agent` (subagente con contexto aislado). No editar archivos de test en la conversación principal.
3. **Auditar pruebas ya escritas** → aplicar el skill `unit-test-validator` dentro de `unit-test-agent`, no en la conversación principal.
4. **Tareas de producción** → el agente principal implementa únicamente el cambio solicitado; no debe añadir pruebas por iniciativa propia ni invocar automáticamente el subagente.
5. **Verificación autorizada** → `unit-test-agent` ejecuta `./mvnw clean verify` al final de su trabajo. Si falla, puede corregir la causa y repetir la verificación completa sin solicitar una nueva autorización, hasta confirmar el resultado o identificar un bloqueo fuera del alcance. Evitar repeticiones sin cambios ni motivo. La conversación principal no duplica la verificación.
6. **Conversación principal** → coordina la autorización, lanza el subagente, revisa su resumen y reporta el resultado; no reescribe tests salvo un fallo puntual que el subagente no pueda resolver.

## Manejo de errores
- Jerarquía base en `core`: `BusinessException`, `ValidationException`, `ConflictException`, `NotFoundException`, `TechnicalException`.
- Excepciones específicas por feature (ej. `MessageNotFoundException`) extienden la jerarquía base.
- `GlobalExceptionHandler` en `infrastructure` traduce excepciones de dominio a respuestas HTTP.
- Validaciones con patrón Composite: `CompositeValidator` agrupa reglas individuales (`*Rule` interfaz → `*RuleImpl` implementación).

## Clean Architecture y SonarCloud
Todo código nuevo o modificado debe respetar la separación de capas y pasar el Quality Gate de SonarCloud del pipeline. Reglas concretas:

- **Dirección de dependencias**: `infrastructure → core → utils`, nunca al revés. `core` no puede importar clases de `infrastructure` (frameworks, Spring, drivers de SurrealDB/Redis/Pulsar, etc.) ni de librerías de infraestructura.
- **`core` libre de frameworks**: casos de uso, dominios y validadores en `core` deben ser Java plano (POJOs), sin anotaciones de Spring ni dependencias de persistencia concretas — solo puertos (interfaces).
- **Sin lógica de negocio en adaptadores**: los controllers REST y los adaptadores de `infrastructure` solo traducen entrada/salida y delegan a los casos de uso; no deben contener reglas de negocio.
- **Cobertura de tests**: mantener o mejorar el porcentaje de cobertura JaCoCo que exige el Quality Gate; toda lógica nueva en `core` debe llevar tests unitarios (no depender solo de tests de integración en `infrastructure`).
- **Complejidad cognitiva**: evitar métodos largos o muy anidados; extraer métodos/objetos cuando Sonar marque "Cognitive Complexity" alta.
- **Sin duplicación**: evitar copiar/pegar lógica entre features; extraer a `utils` o a una clase compartida en `core` si aplica.
- **Manejo de excepciones consistente**: usar siempre la jerarquía existente (`BusinessException`, `ValidationException`, `ConflictException`, `NotFoundException`, `TechnicalException`) en vez de excepciones genéricas (`RuntimeException`, `Exception`) — Sonar marca esto como code smell.
- **Sin secretos ni credenciales hardcodeadas**: usar Doppler/Azure Key Vault; nunca literales de tokens, passwords o connection strings en el código (bloqueante en el Quality Gate de seguridad).
- **Nombres e imports limpios**: sin imports no usados, variables sin uso, código comentado muerto ni `TODO` sin ticket asociado — todo esto dispara issues en SonarCloud.
- **Validadores**: nuevas reglas de validación deben seguir el patrón Composite existente (`*Rule` → `*RuleImpl`), no lógica de validación inline en el caso de uso o el controller.

## Convenciones de commits/PRs
No hay convención formal documentada en el repo. CI se dispara en `develop` y en PRs contra `develop` vía Azure Pipelines. Usar mensajes de commit convencionales por defecto (`feat:`, `fix:`, `chore:`, etc.) salvo indicación distinta.

## Herramientas no obvias a tener en cuenta
- **Doppler**: gestión de secretos en runtime, además de Azure Key Vault.
- **SurrealDB**: única base de datos del proyecto, cubre tanto necesidades relacionales como no relacionales (reemplaza el esquema anterior con PostgreSQL/Kafka/MongoDB). No tratarla como una BD SQL tradicional ni asumir que hay un motor relacional separado.
- **LangChain4j + Ollama local**: motor por defecto para traducción de mensajes (fallback/alternativa: OpenAI).
- **Maven Wrapper (`mvnw`)**: siempre preferirlo sobre un `mvn` global.

## Documentación de referencia
- `ArquitecturaEjemplo.md` — documento maestro de arquitectura hexagonal.
- `docs/azure-deployment.md` — despliegue en Azure.
