# Plan de implementación: WebFlux, SurrealDB y actualizaciones en tiempo real en MessageUcoLab

Mi recomendación es abordar esta migración como un cambio arquitectónico controlado, no como una sustitución de Spring MVC por WebFlux. El objetivo es conseguir respuestas HTTP reactivas, permitir actualizaciones de mensajes en tiempo real y preservar Clean Architecture, la integridad del negocio y las pruebas existentes.

Hay una decisión fundamental: no necesitamos convertir todo el proyecto a programación reactiva para conseguir una API HTTP reactiva. Sin embargo, debemos distinguir claramente entre una API reactiva que utiliza operaciones bloqueantes aisladas y una solución completamente no bloqueante desde HTTP hasta la base de datos.

El plan que sigue prioriza la compatibilidad con la arquitectura actual, el puerto `Publisher`, la tecnología existente y la posibilidad de verificar cada fase antes de avanzar.

## 1. Objetivos y criterios de éxito

### API HTTP reactiva
WebFlux gestiona las solicitudes y respuestas. Los controladores GET devuelven `Mono` o `Flux` cuando corresponde.

### Clean Architecture intacta
`core` mantiene sus contratos y no depende de Spring WebFlux, Reactor, Netty ni `Sinks`.

### Mensajes actualizados en tiempo real
Los clientes conectados pueden recibir eventos de creación o actualización sin tener que consultar repetidamente la API.

### Operaciones bloqueantes controladas
Ninguna consulta síncrona a SurrealDB o Redis se ejecuta directamente en el event loop de Netty.

### Sin regresiones
Se preservan autenticación, serialización, manejo de excepciones, trazabilidad, contratos HTTP y reglas de negocio.

### Decisión arquitectónica inicial

Para la primera versión, recomiendo conservar el driver actual de SurrealDB y Redis, aislar las operaciones bloqueantes y mantener los contratos síncronos del núcleo. Esta opción permite reducir el alcance del cambio.

No obstante, no se debe presentar esa solución como acceso a datos totalmente reactivo. Si posteriormente se exige no bloquear ningún hilo de extremo a extremo, habrá que evaluar un driver realmente asíncrono o reactivo compatible con SurrealDB y una estrategia de acceso a Redis que también sea no bloqueante.

## 2. Decisiones técnicas que deben quedar cerradas antes de programar

Estas decisiones deben documentarse en el proyecto antes de modificar clases. Evitan que una implementación aparentemente correcta termine rompiendo la arquitectura o generando una falsa sensación de reactividad.

| Decisión | Recomendación |
|---|---|
| Java | JDK 25 (actualizado por la migración del monorepo) |
| Framework HTTP | Spring WebFlux |
| GET existentes | Exponerlos de forma reactiva |
| POST, PUT y DELETE | Conservar el comportamiento actual en la primera fase |
| SurrealDB | Mantener el SDK actual hasta verificar alternativas compatibles |
| Redis | Mantener la implementación actual inicialmente |
| Operaciones bloqueantes | Aislarlas en un scheduler dedicado o adecuadamente dimensionado |
| Core | Sin dependencias de Spring, Reactor ni infraestructura |
| Eventos | Puerto `Publisher` y un contrato separado para suscripción |
| Tiempo real | SSE como primera alternativa si solo se necesita enviar actualizaciones del servidor al cliente |
| Pruebas | Sin cambiar la lógica de negocio ni relajar la cobertura para facilitar la migración |

La versión exacta de Spring Boot y Springdoc debe verificarse en los POM antes de seleccionar dependencias. No conviene actualizar versiones de manera oportunista durante esta migración.

### 2.1. Distinción entre los puertos

Recomiendo separar las operaciones de consulta de las operaciones que modifican el estado:

- `MessageQueryPort`: consulta mensajes.
- Los puertos de comandos existentes: crean, actualizan o eliminan mensajes.
- `Publisher`: publica eventos de negocio.
- `MessageEventStreamPort`: permite consumir el flujo de eventos para SSE.

No es obligatorio introducir cuatro interfaces nuevas si los contratos existentes ya cubren alguna de estas responsabilidades. Primero se inspeccionan los contratos actuales y se amplían solo donde haga falta.

Para mantener `core` libre de Reactor, las interfaces de consulta pueden seguir siendo síncronas durante la primera etapa. El controlador WebFlux envuelve la llamada bloqueante. Si necesitas que los puertos de consulta también sean asíncronos, esa será una decisión arquitectónica distinta que habrá que resolver antes de implementarla; no debe introducirse accidentalmente durante la migración.

## 3. Fase 0 — Auditoría y línea base

**Puerta de control 0**

**Objetivo:** conocer el estado real del código antes de cambiarlo.

### Tareas

1. Revisar `AGENTS.md` y las instrucciones de arquitectura del repositorio.
2. Revisar el POM raíz y los POM de `core`, `infrastructure` y `utils`.
3. Confirmar las versiones de Spring Boot, Java, Reactor, Springdoc y el SDK de SurrealDB.
4. Identificar todos los controladores, sus implementaciones, facades, presenters, filtros, interceptores y manejadores de excepciones.
5. Localizar las llamadas bloqueantes a SurrealDB y Redis.
6. Identificar el sistema actual de autenticación, serialización, logging, OpenTelemetry, MDC y transacciones.
7. Ejecutar las pruebas existentes y registrar los resultados por módulo.
8. Guardar una referencia del estado inicial de las respuestas HTTP y de los endpoints actuales.

### Entregable

Un inventario de dependencias y un mapa de llamadas desde el controlador hasta la base de datos.

No se modifica código de negocio durante esta fase.

**Criterio para avanzar:** la aplicación compila, se conoce el resultado de las pruebas y están identificadas todas las rutas GET y las dependencias bloqueantes relevantes.

## 4. Fase 1 — Migración de dependencias y arranque

**Puerta de control 1**

**Objetivo:** conseguir que la aplicación arranque con WebFlux antes de migrar los controladores.

### Cambios

- Sustituir `spring-boot-starter-web` por `spring-boot-starter-webflux` en el módulo que expone HTTP.
- Sustituir el artefacto de Springdoc para Spring MVC por su equivalente compatible con WebFlux, verificando la versión correspondiente.
- Inspeccionar el árbol de dependencias Maven para detectar componentes Servlet que sigan siendo necesarios o que entren en conflicto.
- Verificar el tipo de aplicación efectivo y el servidor HTTP utilizado.
- Revisar la configuración de seguridad, serialización, Actuator y documentación OpenAPI.
- Mantener las versiones y la configuración de SurrealDB, Redis y demás servicios sin cambios funcionales.

No se debe eliminar indiscriminadamente cualquier dependencia relacionada con Servlet: algunas bibliotecas pueden utilizar clases Servlet sin impedir que la aplicación principal funcione sobre WebFlux. Cada dependencia debe evaluarse por su uso real.

### Validaciones

- Compilación Maven correcta.
- Arranque con Netty, si esa es la configuración prevista.
- Health check funcional.
- Documentación OpenAPI accesible.
- Conexiones con SurrealDB y Redis operativas.
- Sin cambios inesperados en la configuración de los servicios.

**Criterio para avanzar:** la aplicación arranca y se conecta a sus dependencias sin haber modificado todavía la lógica de los casos de uso.

## 5. Fase 2 — Prueba de concepto de un único GET

**Puerta de control 2**

Esta es la fase más importante: antes de migrar todos los endpoints, se demuestra que la arquitectura elegida funciona en el proyecto real.

Escogería un GET de catálogo sencillo, por ejemplo el de tipos de mensajes, siempre que la auditoría confirme que es representativo.

### Flujo propuesto

1. **Controller WebFlux:** devuelve `Mono<ResponseEntity<T>>`.
2. **Scheduler:** ejecuta la operación bloqueante fuera del event loop.
3. **Caso de uso y puertos existentes:** permanecen sin cambios en la lógica de negocio.
4. **Adaptador de SurrealDB:** ejecuta la consulta síncrona, aislada del event loop.

El controlador podría utilizar este patrón, adaptando los tipos a los contratos reales:

```java
@GetMapping("/message-types")
public Mono<ResponseEntity<MessageTypesResponse>> getMessageTypes() {
    return Mono.fromCallable(
            () -> findCatalogUseCaseFacade.findMessageTypes()
        )
        .subscribeOn(Schedulers.boundedElastic())
        .map(result -> ResponseEntity.ok(toResponse(result)));
}
```

Este código es ilustrativo: `MessageTypesResponse`, la firma de la facade y `toResponse` deben corresponder con las clases reales del repositorio.

### Pruebas obligatorias de la prueba de concepto

- Respuesta correcta cuando hay datos.
- Comportamiento cuando el catálogo está vacío.
- Errores de SurrealDB y sus códigos HTTP correspondientes.
- Formatos de respuesta y cabeceras `Accept` admitidos por ese endpoint.
- Verificación de que la consulta no se ejecuta en el event loop.
- Comportamiento ante varias solicitudes simultáneas.
- Cancelación y propagación de errores.
- Ausencia de cambios en los contratos del núcleo.

**Criterio para avanzar:** el endpoint conserva su contrato externo y funciona con WebFlux bajo pruebas automatizadas.

## 6. Fase 3 — Migración del resto de los GET

**Objetivo:** migrar los endpoints de lectura sin modificar las reglas de negocio ni cambiar innecesariamente las rutas públicas.

Los controladores que deben inspeccionarse incluyen:

- `CatalogController` y `CatalogControllerImpl`.
- `MessagesController` y `MessagesControllerImpl`.
- `ResolveMessageController` y `ResolveMessageControllerImpl`.

La lista definitiva se confirma durante la auditoría.

### Estrategia

1. Clasificar cada GET según su fuente de datos, formato de respuesta, seguridad y complejidad.
2. Migrar primero los endpoints de lectura sencillos.
3. Migrar después los que dependen de Redis, SurrealDB, autenticación o serialización personalizada.
4. Conservar rutas, parámetros, códigos HTTP, contratos de respuesta y permisos.
5. No convertir operaciones de escritura en reactivas durante esta fase, salvo que exista una dependencia técnica inevitable y aprobada.
6. Ejecutar las pruebas de regresión después de cada grupo de endpoints.

### Gestión de los schedulers

No conviene repartir llamadas a `boundedElastic()` por todas las clases sin una estrategia común. Se debe definir una política para las operaciones bloqueantes:

- Centralizar el patrón de ejecución en un adaptador o componente de infraestructura reutilizable.
- Considerar un scheduler dedicado para las operaciones de base de datos si las pruebas de carga justifican aislarlas.
- Establecer límites de concurrencia y observar las colas de trabajo.
- Definir tiempos de espera y cómo se propagan los errores.
- Evitar que un pool saturado degrade todas las solicitudes.

`boundedElastic()` evita bloquear el event loop, pero no elimina los límites de concurrencia, los tiempos de respuesta del driver ni la saturación de recursos.

**Criterio para avanzar:** todos los GET definidos en el alcance funcionan de forma reactiva en la capa HTTP y conservan sus contratos públicos.

## 7. Fase 4 — Migración de interceptores y filtros

**Objetivo:** adaptar el procesamiento transversal al modelo de WebFlux.

Los interceptores Servlet no deben trasladarse mecánicamente a un `WebFilter`. Hay que reconstruir el flujo y conservar sus responsabilidades.

| Componente actual | Tratamiento |
|---|---|
| `ActiveContextInterceptor` | `WebFilter` de contexto externo; credenciales propietarias retiradas |
| `AcceptHeaderInterceptor` | Migrar la negociación de formatos al contexto reactivo |
| `ExternalIdentityInterceptor` | Preservar la resolución y validación de identidad |
| `LoggingConfig` | Revisar filtros, contexto de logging y propagación de MDC |
| `HttpPresenterAdapter` | Adaptar el manejo global de excepciones al contexto reactivo |

### Requisitos técnicos

- Usar `ServerWebExchange` y `WebFilterChain`.
- Guardar los atributos de la solicitud en el contexto apropiado de WebFlux.
- Ejecutar la validación bloqueante de tokens fuera del event loop.
- Mantener el orden de autenticación, autorización y procesamiento.
- Evitar que una misma solicitud se valide dos veces por una cadena de filtros mal configurada.
- Conservar las cabeceras de correlación y el contexto de trazabilidad.
- No utilizar `ThreadLocal` como mecanismo general de propagación del contexto reactivo.

**Importante:** MDC y Reactor Context no son equivalentes. La propagación de trazas y del contexto de logging debe verificarse explícitamente, especialmente cuando el trabajo cambia de hilo.

### Pruebas de seguridad

Se debe comprobar, como mínimo, el comportamiento con tokens válidos, ausentes, vencidos e inválidos, así como con identidades externas incorrectas y solicitudes no autorizadas.

**Criterio para avanzar:** todas las rutas protegidas conservan sus reglas de acceso y el contexto de observabilidad llega correctamente a los componentes que lo necesitan.

## 8. Fase 5 — Manejo de excepciones y serialización

**Objetivo:** mantener una única estrategia de respuesta HTTP y evitar diferencias de comportamiento entre los endpoints migrados y los no migrados.

### Excepciones

El adaptador global de excepciones debe:

- Integrarse correctamente con WebFlux.
- Transformar los errores de negocio en las respuestas HTTP previstas.
- Distinguir errores de validación, autenticación, autorización, inexistencia de recursos y fallos de infraestructura.
- Evitar exponer trazas internas, consultas, credenciales o información sensible.
- Respetar el formato de error que espera el cliente.

Los manejadores pueden devolver respuestas reactivas cuando corresponda. No es obligatorio que cada método devuelva `Mono` si su contrato es compatible con el mecanismo de excepciones de WebFlux; se elegirá la firma que corresponda al diseño concreto.

### Serialización

Tu `PresenterPort` y el `SerializerRegistry` deben conservar su responsabilidad de presentación sin depender de `HttpServletResponse`.

La negociación debe verificarse para cada formato realmente soportado: JSON, YAML, XML, texto y HTML. No se debe anunciar un formato que el serializador no pueda producir ni devolver un cuerpo de un formato distinto al solicitado sin una política explícita.

**Criterio para avanzar:** las respuestas normales y los errores conservan un contrato uniforme y documentado.

## 9. Fase 6 — Diseño correcto del puerto `Publisher`

Esta fase merece un diseño independiente, porque la publicación de eventos y las consultas HTTP son responsabilidades diferentes.

### 9.1. Contrato de publicación

El puerto debe expresar la intención de publicar un evento, sin revelar qué tecnología lo transporta.

Por ejemplo:

```java
public interface Publisher<T> {
    void publish(T event);
}
```

Esta es una forma inicial posible si la publicación es local y síncrona. Si necesitas confirmación de entrega, persistencia, publicación remota o propagación asíncrona de errores, el contrato debe reflejarlo antes de implementarlo. No conviene prometer entrega garantizada con un método `void`.

El puerto debe ubicarse en la capa que define la necesidad de publicar eventos, dentro de `core` o del módulo de aplicación según la organización real del proyecto. No debe depender de Reactor ni de `Sinks.Many`.

### 9.2. Separar publicación de suscripción

El puerto de publicación no resuelve por sí solo cómo un controlador SSE obtiene los eventos. Recomiendo un segundo contrato para consumir el flujo.

Hay dos alternativas:

- Un contrato de flujo independiente de Reactor, por ejemplo basado en `java.util.concurrent.Flow.Publisher`.
- Un contrato de aplicación basado en Reactor, únicamente si se acepta explícitamente que esa capa dependa de Reactor.

Para preservar el aislamiento actual del núcleo, prefiero la primera alternativa. El adaptador de infraestructura puede conectar ese contrato con Reactor mediante los adaptadores correspondientes.

La estructura conceptual sería:

1. **Caso de uso:** se produce un evento de negocio.
2. **Puerto `Publisher`:** publica el evento sin conocer el transporte.
3. **Adaptador de eventos:** implementación local con Reactor Sinks.
4. **Puerto de suscripción y controlador SSE:** el cliente recibe los eventos de forma continua.

### 9.3. Cuándo se publica un evento

Los eventos deben representar cambios de negocio, no simples consultas.

Ejemplos:

- `MessageCreated`.
- `MessageUpdated`.
- `MessageDeleted`, si corresponde al modelo de negocio.

La publicación debe producirse en el punto del flujo donde el cambio ya haya sido aceptado por el sistema. Si la operación modifica datos persistentes, publicar antes de confirmar la persistencia puede generar notificaciones de cambios que nunca llegaron a guardarse.

Hay que definir también qué ocurre si se guarda un mensaje, pero falla la publicación. Para una primera versión en memoria se puede aceptar una garantía limitada, siempre que esté documentada. Si la entrega no puede perderse, habrá que diseñar una solución duradera, como un patrón outbox o un mecanismo de mensajería persistente.

**Criterio para avanzar:** el contrato de publicación y el de suscripción están definidos, las responsabilidades son independientes y se conoce la garantía de entrega real.

## 10. Fase 7 — Implementación de SSE para actualizaciones en tiempo real

**Objetivo:** que los clientes puedan mantener una conexión HTTP abierta y recibir actualizaciones a medida que se publican eventos.

SSE es una buena primera opción si la comunicación es principalmente del servidor hacia el cliente. Si necesitas comunicación bidireccional continua, habrá que evaluar WebSocket por separado.

### Endpoint propuesto

```java
@GetMapping(
    value = "/events",
    produces = MediaType.TEXT_EVENT_STREAM_VALUE
)
public Flux<ServerSentEvent<MessageEvent>> events() {
    // Adaptar el flujo del puerto de suscripción a SSE.
}
```

Es un contrato ilustrativo, no una implementación lista para copiar. Debe integrarse con el puerto de suscripción definido en la fase anterior.

### Requisitos de la implementación

1. Emitir únicamente eventos que correspondan a la autorización del usuario o canal.
2. Definir un identificador de evento y una estructura estable del mensaje.
3. Gestionar la desconexión y cancelación del flujo cuando el cliente se retire.
4. Considerar eventos de heartbeat para conexiones prolongadas.
5. Aplicar límites de suscriptores, buffering y backpressure.
6. Definir el comportamiento de reconexión y el uso de `Last-Event-ID`, si se implementa recuperación de eventos.
7. Evitar enviar información confidencial a clientes que no tengan permiso para recibirla.
8. No mantener una conexión SSE abierta dentro de un método `boundedElastic()` que permanezca ocupado indefinidamente.

### Limitaciones de un `Sinks.Many` en memoria

Es una opción válida para comenzar, pero hay que tener presentes cuatro limitaciones:

- Los eventos no son duraderos.
- Una reconexión no recupera automáticamente los eventos perdidos.
- Varias instancias de la aplicación no comparten automáticamente el mismo flujo.
- El comportamiento ante saturación del buffer depende de la estrategia elegida.

Para una sola instancia y requisitos modestos, esta solución puede ser suficiente. Para múltiples réplicas o requisitos estrictos de entrega, será necesario un transporte compartido o una fuente persistente de eventos.

### Cómo comprobar que funciona

Abrir dos clientes SSE simultáneos, publicar un evento válido y verificar que ambos reciben el evento que tienen autorizado. Después, desconectar uno, publicar otro evento y comprobar el comportamiento esperado al reconectarse.

**Criterio para avanzar:** la conexión SSE funciona bajo concurrencia, respeta las autorizaciones y no bloquea los hilos de Netty.

## 11. Fase 8 — Pruebas y regresión

La migración no se considera terminada porque la aplicación compile. Debe demostrar que mantiene el comportamiento anterior y que las nuevas funcionalidades funcionan correctamente.

### Matriz de pruebas

| Nivel | Qué se verifica |
|---|---|
| Unitarias de `core` | Reglas de negocio, validaciones, contratos y casos de uso |
| Unitarias de adaptadores | Conversión de respuestas, errores y publicación de eventos |
| Controladores | Códigos HTTP, cuerpos, cabeceras y formatos |
| Seguridad | Tokens, identidad externa y autorización |
| Integración | SurrealDB, Redis y configuración real de la aplicación |
| Reactividad | Ninguna operación bloqueante en el event loop |
| SSE | Suscripción, emisión, cancelación y reconexión |
| Concurrencia | Múltiples GET, suscriptores y saturación controlada |
| Regresión | Todos los endpoints fuera del alcance mantienen su comportamiento |

### Reglas para modificar las pruebas

- No eliminar pruebas existentes para conseguir una compilación satisfactoria.
- No relajar umbrales de cobertura ni desactivar verificaciones de calidad.
- Reescribir únicamente las pruebas afectadas por los nuevos contratos HTTP.
- Preferir `WebTestClient` para las pruebas de integración de WebFlux.
- Mantener pruebas unitarias del núcleo sin necesidad de levantar Spring.
- Conservar las versiones de Java y dependencias que no sea necesario cambiar.
- Ejecutar las pruebas de cada módulo y después la suite completa.

El número real de pruebas y la cobertura inicial deben obtenerse de la ejecución de la fase 0; no se debe asumir que las cifras de una ejecución anterior siguen siendo la línea base vigente.

## 12. Fase 9 — Rendimiento, observabilidad y estabilidad

**Objetivo:** comprobar que la migración mejora el comportamiento bajo carga y no se limita a cambiar las firmas de los métodos.

Se deben medir al menos:

- Latencia p50, p95 y p99 de los GET.
- Tasa de solicitudes y porcentaje de errores.
- Tiempo de espera de las operaciones bloqueantes.
- Saturación de los schedulers y cantidad de tareas pendientes.
- Uso de CPU y memoria.
- Conexiones activas y desconexiones SSE.
- Cantidad de suscriptores y eventos descartados.
- Trazas y logs correlacionados entre petición, caso de uso y adaptador.

La prueba de carga debe incluir tanto GET sencillos como consultas lentas a SurrealDB, solicitudes que utilizan Redis y varios clientes SSE simultáneos.

También se deben verificar los timeouts, el cierre ordenado de conexiones y la recuperación ante indisponibilidad temporal de las dependencias.

**Criterio de aceptación:** no se bloquea el event loop, los errores y tiempos de espera están controlados y el rendimiento medido cumple los objetivos establecidos antes de la migración.

## 13. Fase 10 — Despliegue y cierre

La migración debe llegar al entorno Docker de MessageUcoLab sin cambiar de forma innecesaria la infraestructura de los servicios existentes.

### Secuencia

1. Ejecutar el build completo con Java 25 y las pruebas habilitadas.
2. Construir la imagen Docker.
3. Levantar la aplicación junto con las dependencias requeridas.
4. Verificar el health check, las consultas a SurrealDB, el acceso a Redis y las rutas HTTP.
5. Probar SSE con clientes reales o automatizados.
6. Ejecutar una prueba de regresión después del despliegue.
7. Comparar las métricas con la línea base.
8. Documentar las limitaciones de la implementación y el procedimiento de reversión.

La imagen y la configuración deben conservar las redes, variables de entorno, secretos, puertos y dependencias actuales, salvo los cambios estrictamente necesarios para esta migración.

El despliegue no debe considerarse exitoso si únicamente arranca el contenedor: debe verificarse el flujo completo desde HTTP hasta las dependencias y, en el caso de SSE, desde la publicación del evento hasta su recepción por el cliente.

## 14. Orden definitivo de ejecución

Este es el orden que seguiría en el repositorio. Cada fase debe cerrarse antes de iniciar la siguiente.

### Lista de ejecución

- [ ] **Fase 0. Auditoría y línea base:** revisar arquitectura, POM, controladores, dependencias bloqueantes y pruebas actuales.
- [ ] **Fase 1. Configuración WebFlux:** migrar dependencias HTTP y comprobar el arranque sin cambiar la lógica de negocio.
- [ ] **Fase 2. GET de catálogo piloto:** validar el primer controlador reactivo, los formatos de respuesta y el aislamiento de las consultas bloqueantes.
- [ ] **Fase 3. Resto de GET:** migrar controladores de lectura por grupos pequeños y ejecutar regresiones.
- [ ] **Fase 4. Filtros y excepciones:** adaptar autenticación, identidad, logging, MDC y manejo de errores.
- [ ] **Fase 5. Publisher y SSE:** definir contratos de publicación y suscripción, autorización, cancelación y reconexión.
- [ ] **Fase 6. Pruebas integrales:** validar los módulos, la suite completa, la concurrencia y la entrega de eventos.
- [ ] **Fase 7. Rendimiento y despliegue:** medir latencias, saturación, observabilidad y estabilidad en Docker.

Los números de la lista representan el orden de trabajo, no estimaciones de duración. El tiempo real dependerá del número de controladores, los contratos existentes y el estado de las pruebas.

## 15. Riesgos principales y cómo prevenirlos

| Riesgo | Medida preventiva |
|---|---|
| Bloqueo del event loop | Aislar toda llamada bloqueante y verificarlo mediante pruebas |
| Conflictos entre dependencias MVC y WebFlux | Revisar el árbol Maven y el servidor efectivo |
| Pérdida de comportamiento HTTP | Pruebas de contrato antes y después de cada migración |
| Dependencia de Reactor en `core` | Mantener los puertos independientes de Reactor |
| Eventos perdidos | Documentar las garantías de entrega y elegir una estrategia duradera si es necesario |
| Saturación por consultas lentas | Limitar concurrencia y observar los schedulers |
| Fuga de información por SSE | Autorización por usuario, canal o recurso |
| Fallos de trazabilidad | Validar propagación de Reactor Context, MDC y OpenTelemetry |
| Regresiones en producción | Despliegue controlado, smoke tests y procedimiento de reversión |

## 16. Qué no haría durante esta implementación

Para evitar ampliar el alcance y dificultar el diagnóstico, no haría lo siguiente:

- No reescribiría todos los casos de uso como reactivos.
- No introduciría `Mono` y `Flux` en `core` sin una decisión explícita.
- No cambiaría el SDK de SurrealDB sin demostrar que existe una alternativa compatible y que aporta el comportamiento requerido.
- No migraría todas las operaciones de escritura a la vez que los GET.
- No sustituiría Redis, la arquitectura de módulos ni el sistema de observabilidad.
- No publicaría eventos por cada GET cuando lo que se necesita son notificaciones de cambios de mensajes.
- No prometería entrega garantizada usando únicamente un flujo en memoria.
- No modificaría las reglas de negocio, los umbrales de cobertura ni las versiones de Java para facilitar el build.
