
# Plan de seguridad, autorizacion y contexto activo

## Proposito

Este documento registra la migracion del esquema de seguridad de MessageUcoLab. Debe actualizarse en cada sesion donde se tome una decision o se modifique codigo relacionado con autenticacion, autorizacion, tokens, organizaciones o contexto activo.

El objetivo final es separar claramente estas responsabilidades:

```text
Proveedor de identidad -> autentica: quien es el usuario o servicio
MessageUcoLab          -> autoriza: que puede hacer dentro del negocio
Contexto activo        -> determina: en que organizacion, aplicacion y ambiente trabaja
```

El token emitido por el proveedor se recibira y validara sin modificarlo, enriquecerlo ni reemplazarlo para transportar el contexto de MessageUcoLab.

## LEY DE ARQUITECTURA DEL PROYECTO

> **LEY INNEGOCIABLE:** Todo codigo nuevo debe seguir primero la arquitectura, estructura, nomenclatura, paquetes, jerarquias, patrones y convenciones ya existentes en este proyecto. Antes de crear una clase se debe localizar y revisar su equivalente funcional dentro del repositorio y reproducir ese patron. No se introduciran alternativas genericas, estructuras diferentes, `record`, nuevas abstracciones ni decisiones de estilo aunque sean tecnicamente validas si no corresponden con la arquitectura actual, salvo autorizacion explicita del responsable del proyecto.

Aplicacion obligatoria de esta ley:

- Las entidades de dominio deben conservar la convencion `*Entity`, ubicarse en `usecase.domain.aggregate.entities` y extender la jerarquia existente cuando corresponda, como `Entity<UUID>`.
- Los puertos, casos de uso, validadores, DTO, adaptadores, modelos de persistencia y pruebas deben respetar la ubicacion y forma de sus equivalentes actuales.
- Antes de implementar cada incremento se deben inspeccionar clases analogas del proyecto; la consistencia arquitectonica prevalece sobre preferencias externas o soluciones genericas.
- Cualquier excepcion a esta ley requiere aprobacion explicita antes de modificar codigo.

## Estado general

| Campo | Valor |
|---|---|
| Ultima actualizacion | 2026-10-20 |
| Estado global | Retiro del token propietario verificado; recuperacion 401 del frontend con popup y reconexion manual implementada, pruebas nuevas pendientes de autorizacion |
| Fase actual | Fase 7 - Codigo backend/frontend retirado y verificado; cierre operativo parcial |
| Proxima implementacion | Validar popup y reconexion manual del frontend tras autorizacion; completar el puente de identidad fuera de local-workspace |
| Bloqueo actual | El modo por usuario requiere el adaptador real hacia ExternalIdentity; no se cambia seguridad para suplirlo |

### Contrato vigente tras el retiro (2026-10-20)

Esta seccion prevalece sobre las decisiones de convivencia y evidencias historicas
que se conservan abajo para trazabilidad. El usuario confirma que el token legado
ya no tiene consumidores y solicita su retiro del backend y frontend MessageUcoLab.

- No se emiten ni validan tokens propietarios; no se interpreta el header `Token`.
- El componente externo de seguridad y el PEP conservan autenticacion y permisos.
  No se modifican sus repositorios, frontends, Docker, datos ni configuracion.
- El frontend conserva la sesion BFF mediante cookies y `credentials: 'include'`.
- `local-workspace` conserva su destino fijo y el enforcement del PEP. El campo
  `MessageAccessContext.workspaceEnvironmentId` representa ese destino confiable,
  no una credencial ni un ambiente enviado por el cliente.
- Fuera de ese perfil se exige identidad externa resuelta y contexto activo. El
  puente del proveedor real a `ExternalIdentity` sigue pendiente; retirar el token
  no lo implementa ni habilita un fallback sin autenticacion.
- Se retiran codigo, configuracion, dependencias y seeds exclusivos del mecanismo
  propietario. Doppler CLI sigue inyectando secretos del entorno; la app no necesita
  acceso de escritura a su API ni `DOPPLERTOKEN`.
- No se borran tablas de volumenes existentes, datos de Redis ni secretos remotos.
  El nuevo seed deja de crearlos; cualquier purga remota requiere alcance propio.
- Las evidencias de pruebas de fases anteriores son historicas, no verifican este
  retiro. El usuario autorizo `unit-test-agent` con GPT-6 Luna en primer plano.

### Convenciones de estado

| Estado | Significado |
|---|---|
| `PENDIENTE` | No se ha iniciado |
| `EN CURSO` | Se esta trabajando actualmente |
| `PARCIAL` | Tiene avances, pero no cumple todos sus criterios de salida |
| `BLOQUEADA` | Requiere una decision o dependencia externa |
| `COMPLETADA` | Implementada, probada y documentada |

## Decisiones confirmadas

- Se incluira la jerarquia `Organizacion -> Aplicacion -> Ambiente`.
- El token propietario actual convivira temporalmente con el flujo nuevo.
- El token externo se usara para identificar al usuario o servicio, no para transportar el ambiente activo.
- MessageUcoLab sera responsable de los permisos de negocio.
- El identificador estable de una identidad externa sera la combinacion `issuer + subject`.
- El correo sera un atributo informativo y no la llave principal de la identidad.
- El token original no se modificara, refirmara ni envolvera dentro de otro token.
- No se almacenaran tokens completos en SurrealDB o Redis.
- El contexto activo se administrara en el backend.
- SurrealDB sera la fuente persistente del ultimo contexto y Redis permitira su consulta rapida.
- La migracion sera gradual para no interrumpir las consultas que dependen del token actual.
- Se mantendra un unico contexto activo por identidad `issuer + subject`, compartido entre pestanas, dispositivos y tokens validos de esa identidad; la ultima actualizacion persistida prevalece.
- Durante la convivencia, la ausencia de `Authorization` permite continuar con el flujo legado; si el header se envia, debe ser un Bearer valido o la peticion recibe `401`.
- El simulador de identidad externa permanece deshabilitado por defecto y todos sus datos se suministran mediante configuracion externa.
- Toda Aplicacion nueva debe pertenecer a una Organizacion existente; no se asignara una organizacion artificial por defecto a datos historicos.
- El esquema final solo soporta instalaciones nuevas. No se ofrece backfill ni migracion para volumenes existentes; se debe crear un volumen nuevo.
- Las asignaciones de rol pertenecen exclusivamente a una Organizacion y sus permisos aplican a las Aplicaciones y Ambientes de esa Organizacion. No existen `scope_type`, `application_id` ni `environment_id` en una asignacion.
- Las asignaciones son aditivas y no se modelan denegaciones explicitas en la Fase 3.
- La identidad externa conserva `issuer + subject` como llave estable y correo informativo; no modela un tipo de principal.
- La creacion de Aplicacion y Funcionalidad no recibe fechas de inicio o fin.
- La creacion de Ambiente no recibe nombre: el tipo de ambiente es su etiqueta visible y solo puede existir un ambiente de cada tipo por Aplicacion.
- La creacion de Mensaje conserva `applicationId` y no recibe el nombre `application`; el servidor deriva ese nombre para las salidas.
- Los agregados fuente no conservan los campos auxiliares `version` ni `seed_key` definidos por el usuario. Las versiones tecnicas de eventos de dominio y proyecciones permanecen vigentes.
- Se eliminan las tablas y entidades persistentes `parameter` y `represent_parameter`; `CatalogParameterPort` y `parameter.properties` permanecen como mecanismo de configuracion tecnica.
- Durante la convivencia, los catalogos jerarquicos conservan el comportamiento legado sin identidad externa; con identidad externa filtran recursos por `CONTEXT_SELECT` y rechazan con `403` los accesos fuera del alcance autorizado.

## Convencion vigente de validacion

Desde la unificacion realizada el 2026-09-26, las validaciones nuevas o modificadas deben seguir este modelo:

```text
Specification<T> -> evalua una condicion y retorna boolean
RuleValidator<T> -> asocia Specification + codigo de catalogo + fabrica de excepcion
CompositeValidator<T> -> ejecuta reglas en orden y se detiene en el primer error
```

Reglas obligatorias para las siguientes fases:

- Una `Specification` no obtiene mensajes ni lanza excepciones; solo evalua una condicion reutilizable.
- Cada regla concreta extiende `RuleValidator<T>`, tiene una responsabilidad y selecciona su codigo de `MessageCatalogCodeEnum`.
- `Specifications.field(...)` aplica una especificacion reutilizable a un campo de un DTO o contexto.
- Las validaciones con varias entradas usan un contexto tipado, como `AuthorizationValidationContext` o `CreateMessageValidationContext`; no se deben recrear validadores imperativos `*RuleImpl`.
- Los composites declaran el orden con `List.of(...)`. Ese orden es parte del contrato y garantiza cortocircuito antes de consultar repositorios o convertir identificadores.
- Las reglas de requerido y UUID se ejecutan antes de existencia, pertenencia o duplicidad.
- Las consultas se hacen mediante puertos de `core` desde reglas concretas, nunca mediante adaptadores de infraestructura.
- La fabrica de excepcion de `RuleValidator` debe preservar la semantica HTTP: `UnauthorizedException` (`401`), `ForbiddenException` (`403`), `NotFoundException` (`404`), `ConflictException` (`409`) y `BusinessRuleException` (`422`).
- No se deben reintroducir interfaces con una unica implementacion `*RuleImpl` para reglas simples.
- Cada regla requiere pruebas de caso valido, limite y error; cada composite requiere pruebas de orden y cortocircuito.

Ordenes que forman parte del contrato actual:

- Aplicacion: nombre, Organizacion requerida, UUID de Organizacion, existencia de Organizacion, catalogos, estado y duplicidad.
- Contexto activo: identificadores requeridos, tres UUID, existencia de Organizacion/Aplicacion/Ambiente y las dos relaciones jerarquicas.
- Autorizacion: identidad requerida antes de consultar permisos.
- Mensaje: campos y catalogos antes del ambiente autenticado, existencia del ambiente y relaciones con Aplicacion y Funcionalidad.
- Organizacion: nombre requerido, longitud maxima y duplicidad.
- Paginacion: formato opcional del DTO, numero, tamano, direccion y columna de ordenamiento; el rango contra paginas disponibles se valida despues de consultar el repositorio.

La validacion transversal de paginacion sigue la misma separacion:

- `page/PageRequestDTOCompositeValidator` valida el formato textual recibido por la API.
- `page/SimplePageRequestCompositeValidator` valida el objeto normalizado antes de consultar datos.
- `page/PageRequestRangeValidator` valida la pagina solicitada contra el total obtenido.
- `page/rule/` contiene las reglas atomicas de numero, tamano, direccion, columna y formato de atributos.
- `SimplePageRequestValidationContext` transporta la peticion y el tipo de modelo requerido para validar columnas mediante reflexion.
- `PageRequestRangeValidationContext` agrupa pagina solicitada y total de paginas.
- Los antiguos wrappers `PageRequestDTOValidator` y `SimplePageRequestValidator` no deben reintroducirse; los consumidores dependen directamente de los composites.

## Estado actual del sistema

El flujo propietario continua activo para mantener compatibilidad y ahora convive con la resolucion opcional de identidad externa simulada. El mecanismo legado:

1. Genera tokens con RSA.
2. Persiste informacion del token en SurrealDB.
3. Guarda claves privadas mediante Doppler.
4. Recibe el token en el header `Token`.
5. Obtiene el `environmentId` asociado al token.
6. Coloca el ambiente como atributo de la peticion HTTP.

Riesgos conocidos que deben atenderse antes o durante la migracion:

- La expiracion persistida del token no se verifica al autorizar una peticion.
- El listado de mensajes puede quedar accesible sin la proteccion esperada.
- Algunas consultas pueden aceptar un ambiente enviado por el cliente.
- La creacion de mensajes puede usar aplicacion y ambiente recibidos en el body sin compararlos correctamente con el contexto autorizado.
- El modelo de autorizacion ya existe, pero su administracion mediante endpoints permanece fuera del alcance completado.
- Los logs que exponian tokens, cuerpos HTTP o material criptografico fueron saneados en la Fase 1.
- El proveedor real sigue pendiente; el simulador externo solo se habilita mediante la propiedad explicita de desarrollo.

## Flujo objetivo

```text
Cliente
  -> envia Authorization: Bearer <token original>
API Gateway o backend
  -> valida autenticidad, emisor, audiencia y expiracion
MessageUcoLab
  -> obtiene issuer + subject
  -> resuelve el contexto activo en Redis/SurrealDB
  -> verifica permisos de negocio
Caso de uso
  -> opera solamente dentro del contexto autorizado
```

Respuestas esperadas:

- `401 Unauthorized`: token ausente, invalido o expirado.
- `403 Forbidden`: identidad valida sin permiso sobre la operacion o el contexto.
- `404 Not Found`: recurso inexistente dentro del contexto permitido.
- `409 Conflict`: seleccion de contexto inconsistente con la jerarquia de negocio.

## Resumen de fases

| Fase | Nombre | Estado | Resultado principal |
|---|---|---|---|
| 0 | Definir contrato de seguridad | `PARCIAL` | Responsabilidades y contrato con el proveedor definidos |
| 1 | Asegurar el flujo actual | `COMPLETADA` | El token existente no permite cruzar ambientes ni exponer secretos |
| 2 | Introducir identidad externa simulada | `COMPLETADA` | Un bearer token identifica al principal sin contener contexto |
| 3 | Modelar organizaciones y autorizacion | `COMPLETADA` | MessageUcoLab decide a que recursos accede cada identidad |
| 4 | Implementar contexto activo | `COMPLETADA` | El backend recuerda y valida el contexto seleccionado |
| 5 | Migrar operaciones al contexto | `COMPLETADA` | Creacion, catalogos y administracion resuelven alcance desde el contexto autorizado |
| 6 | Integrar el proveedor real | `PARCIAL` | PEP real integrado; puente a ExternalIdentity pendiente fuera del perfil local |
| 7 | Retirar el mecanismo propietario | `PARCIAL` | Codigo retirado y regresion verificada; limpieza remota y validacion runtime fuera de alcance |

## Fase 0 - Definir el contrato de seguridad

**Estado:** `PARCIAL`

### Alcance

- [x] Separar autenticacion externa, autorizacion de negocio y contexto activo.
- [x] Elegir `issuer + subject` como identidad estable.
- [x] Decidir que el token no transportara el ambiente.
- [x] Decidir que se incluira la entidad Organizacion.
- [x] Elegir una migracion con convivencia temporal.
- [ ] Confirmar si el token del proveedor sera JWT u opaco.
- [ ] Obtener el `issuer` esperado.
- [ ] Obtener el `audience` esperado.
- [ ] Obtener la URL JWKS o el endpoint de introspeccion.
- [ ] Confirmar los claims disponibles.
- [ ] Definir como se autenticaran los servicios no humanos.
- [ ] Confirmar si el API Gateway validara el token.
- [ ] Definir como se protegera la comunicacion Gateway-backend.
- [ ] Definir si el navegador usara BFF y cookie `HttpOnly` o bearer directo.

### Criterio de salida

Existe un contrato documentado con el proveedor que permite validar tokens sin asumir detalles tecnicos ni incluir dependencias del proveedor en `core`.

## Fase 1 - Asegurar el flujo actual

**Estado:** `COMPLETADA`

### Alcance

- [x] Validar la fecha de expiracion del token actual.
- [x] Proteger el endpoint de listado de mensajes.
- [x] Impedir consultas globales cuando no exista un ambiente autorizado.
- [x] Usar el ambiente derivado del token en vez del enviado por query.
- [x] Impedir la creacion de mensajes en otro ambiente.
- [x] Validar la relacion entre aplicacion, funcionalidad y ambiente.
- [x] Eliminar tokens y claves privadas de los logs.
- [x] Homogeneizar las respuestas `401` del mecanismo actual y los rechazos de autorizacion como `403`.
- [x] Crear pruebas unitarias para token vigente, expirado y en el limite exacto de expiracion.
- [x] Crear o actualizar pruebas unitarias para estas reglas.
- [x] Ejecutar las pruebas de los modulos afectados.

### Criterios de salida

- [x] Un token del ambiente A no puede leer informacion del ambiente B.
- [x] Un token del ambiente A no puede escribir en el ambiente B.
- [x] Un token expirado recibe `401`.
- [x] Omitir el ambiente no produce una consulta global.
- [x] Los logs no contienen tokens ni claves privadas.
- [x] Las pruebas existentes y nuevas pasan correctamente.

### Fuera de alcance

- Redis para contexto activo.
- Modelo de organizaciones y permisos.
- Integracion con el proveedor real.
- Retiro del token propietario.

## Fase 2 - Introducir identidad externa simulada

**Estado:** `COMPLETADA`

### Alcance

- [x] Crear en `core` un modelo de identidad sin dependencias web o de Spring.
- [x] Crear un puerto para validar y resolver una identidad externa.
- [x] Hacer que el resultado incluya `issuer`, `subject`, correo y expiracion.
- [x] Reemplazar conceptualmente la generacion del adaptador simulado por validacion de identidad.
- [x] Activar el simulador solamente mediante perfil o propiedad explicita de desarrollo.
- [x] Recibir el token externo mediante `Authorization: Bearer`.
- [x] Evitar que el bearer token aparezca en logs o claves de cache.
- [x] Mantener temporalmente el header `Token` para consumidores del flujo legado.
- [x] Probar token simulado valido, invalido y expirado.

### Criterio de salida

Una peticion puede convertirse en una identidad estable mediante un token simulado, sin obtener organizacion, aplicacion o ambiente desde ese token.

## Fase 3 - Modelar organizaciones y autorizacion

**Estado:** `COMPLETADA`

### Modelo inicial esperado

```text
Organization
Application -> Organization
Environment -> Application
ExternalIdentity
Membership
Role
Permission
RoleAssignment de Organizacion
```

### Permisos iniciales propuestos

- `CONTEXT_SELECT`
- `MESSAGE_READ`
- `MESSAGE_CREATE`
- `MESSAGE_TRANSLATE`
- `APPLICATION_CREATE`
- `FUNCTIONALITY_CREATE`

### Alcance

#### Avance incremental

- [x] Crear `OrganizationEntity` dentro de la jerarquia de entidades de dominio en `core`.
- [x] Definir el puerto de persistencia de Organizacion en `core`.
- [x] Implementar el modelo, mapper y adaptador de Organizacion para SurrealDB.
- [x] Crear el caso de uso y las reglas de validacion para registrar organizaciones.

- [x] Crear la entidad y persistencia de Organizacion.
- [x] Asociar cada Aplicacion con una Organizacion.
- [x] Persistir identidades externas por `issuer + subject`.
- [x] Modelar membresias, roles, permisos y asignaciones por Organizacion.
- [x] Crear puertos de consulta de autorizaciones en `core`.
- [x] Implementar politicas de autorizacion como reglas de negocio.
- [x] Diferenciar consistentemente `401` y `403`.
- [x] Filtrar catalogos segun los recursos autorizados.
- [x] Probar acceso permitido y denegado entre organizaciones y aplicaciones.

### Criterio de salida

- [x] MessageUcoLab puede responder si una identidad tiene un permiso en una Organizacion y aplicarlo a sus Aplicaciones o Ambientes sin depender de roles internos del proveedor.

## Fase 4 - Implementar contexto activo

**Estado:** `COMPLETADA`

### Modelo esperado

```text
ActiveContext
- organizationId
- applicationId
- environmentId
- updatedAt
```

### Alcance

- [x] Decidir el comportamiento de varias pestanas o dispositivos para una misma identidad.
- [x] Crear el modelo y los puertos de contexto en `core`.
- [x] Persistir el ultimo contexto en SurrealDB.
- [x] Implementar cache distribuida del contexto en Redis.
- [x] No usar el token completo como clave de Redis.
- [x] Crear `GET /messageucolab/v1/me/contexts`.
- [x] Crear `GET /messageucolab/v1/me/context`.
- [x] Crear `PUT /messageucolab/v1/me/context`.
- [x] Verificar membresia, jerarquia y permiso `CONTEXT_SELECT` antes de cambiarlo.
- [x] Definir el comportamiento cuando no exista contexto activo.
- [x] Probar aislamiento, expiracion de cache, cambio y recuperacion desde persistencia.

### Criterio de salida

- [x] La identidad puede seleccionar un contexto autorizado, recuperarlo en peticiones posteriores y conservarlo aunque Redis pierda la entrada cacheada.

## Fase 5 - Migrar operaciones al contexto

**Estado:** `COMPLETADA`

### Orden de migracion

- [x] Consulta y listado de mensajes.
- [x] Traduccion de mensajes.
- [x] Creacion de mensajes.
- [x] Consulta de catalogos.
- [x] Administracion de aplicaciones y funcionalidades; los ambientes se aprovisionan automaticamente.

### Reglas

- [x] Los casos de uso reciben identidad y contexto tipados.
- [x] Los controllers no implementan reglas de autorizacion de negocio.
- [x] `core` no depende de `HttpServletRequest`, Spring Security o Redis.
- [x] Los IDs implicitos en el contexto se eliminan de queries y bodies cuando sea posible.
- [x] Si un ID debe permanecer por compatibilidad, se exige coincidencia con el contexto.
- [x] Cada operacion verifica el permiso correspondiente antes de acceder al repositorio.
- [x] Se agregan pruebas de aislamiento entre organizaciones, aplicaciones y ambientes.

### Criterio de salida

- [x] Ninguna operacion migrada puede seleccionar su alcance de negocio mediante IDs arbitrarios enviados por el cliente.

## Fase 6 - Integrar el proveedor real

**Estado:** `PENDIENTE`

### Alcance

- [ ] Implementar el adaptador con el contrato confirmado en la Fase 0.
- [ ] Validar firma o introspeccion, `issuer`, `audience` y expiracion.
- [ ] Configurar secretos y endpoints externamente.
- [ ] Reemplazar el simulador sin modificar los casos de uso.
- [ ] Configurar la confianza entre Gateway y backend.
- [ ] Actualizar OpenAPI para `Authorization: Bearer`.
- [ ] Validar el flujo de navegador o BFF definido en la Fase 0.
- [ ] Ejecutar pruebas de integracion del flujo completo.

### Criterio de salida

El proveedor real autentica las peticiones y MessageUcoLab conserva de forma independiente la autorizacion y el contexto.

## Fase 7 - Retirar el mecanismo propietario

**Estado:** `PARCIAL` — retiro del codigo completado y verificado; limpieza de
secretos remotos y validacion del proveedor en runtime fuera del alcance autorizado.

### Alcance

- [x] Confirmar que no quedan consumidores del header `Token` (confirmacion del usuario).
- [x] Retirar el endpoint de emision de tokens propietarios.
- [x] Retirar la generacion y validacion RSA exclusiva de esos tokens.
- [ ] Retirar secretos Doppler exclusivos del mecanismo anterior.
- [x] Retirar tablas y eventos del nuevo seed, repositorios y casos de uso obsoletos (sin purgar datos existentes).
- [x] Eliminar configuraciones y documentacion operativa del mecanismo anterior.
- [x] Ejecutar regresion automatizada y comprobar rechazos de autenticacion y permisos.
- [ ] Verificar el despliegue con el proveedor real (no se desplegaron cambios en esta tarea).

### Criterio de salida

Todas las peticiones protegidas usan la identidad del proveedor y no queda codigo activo del token propietario.

## Reglas para actualizar este documento

En cada sesion de trabajo relacionada con este plan se debe:

1. Actualizar la fecha de `Ultima actualizacion`.
2. Actualizar `Estado global`, `Fase actual`, `Proxima implementacion` y `Bloqueo actual`.
3. Cambiar el estado de la fase intervenida.
4. Marcar solamente las tareas realmente implementadas y verificadas.
5. Registrar archivos modificados y pruebas ejecutadas.
6. Registrar decisiones nuevas o cambios de alcance.
7. Registrar bloqueos y riesgos descubiertos.
8. Agregar una entrada al historial de cambios.

Una fase solo puede marcarse `COMPLETADA` cuando cumple sus criterios de salida y sus pruebas han sido ejecutadas exitosamente.

## Registro de la fase actual

### Archivos modificados

- `PLAN_SEGURIDAD_Y_CONTEXTO.md`: creacion del plan y mecanismo de seguimiento.
- `core/src/main/java/co/edu/uco/application/usecase/VerifyAccessUseCase.java`: validacion de expiracion con reloj UTC y traduccion de token inexistente, inactivo o vencido a `UnauthorizedException`.
- `core/src/test/java/co/edu/uco/application/usecase/VerifyAccessUseCaseTest.java`: cobertura de token vigente, inexistente, vencido y vencido exactamente en el instante actual.
- `infrastructure/src/main/java/co/edu/uco/infraestructure/config/WebConfig.java`: registro explicito del listado de mensajes en el interceptor de token.
- `infrastructure/src/main/java/co/edu/uco/infraestructure/primaryadapters/MessagesController.java`: retiro del parametro publico `environmentId` en el listado.
- `infrastructure/src/main/java/co/edu/uco/infraestructure/primaryadapters/controller/MessagesControllerImpl.java`: uso del ambiente autenticado almacenado en la peticion.
- `infrastructure/src/main/java/co/edu/uco/infraestructure/secondaryadapters/repository/surreal/impl/MessageSurrealRepositoryAdapterImpl.java`: eliminacion del filtro global `WHERE TRUE` cuando falta el ambiente.
- `infrastructure/src/test/java/co/edu/uco/infraestructure/config/WebConfigTest.java`: verificacion del registro de la ruta protegida.
- `infrastructure/src/test/java/co/edu/uco/infraestructure/primaryadapters/controller/MessagesControllerImplTest.java`: verificacion del ambiente autenticado en el listado.
- `infrastructure/src/test/java/co/edu/uco/infraestructure/secondaryadapters/repository/surreal/impl/MessageSurrealRepositoryAdapterImplTest.java`: verificacion de que un ambiente ausente nunca genera una consulta global.
- `core/src/main/java/co/edu/uco/application/primaryports/facade/message/CreateMessageUseCaseFacade.java`: propagacion explicita del ambiente autenticado.
- `core/src/main/java/co/edu/uco/application/primaryports/facade/message/impl/CreateMessageUseCaseFacadeImpl.java`: delegacion del DTO junto con el ambiente autenticado.
- `core/src/main/java/co/edu/uco/application/usecase/handling/HandlingCreateMessagePort.java`: contrato de creacion extendido con el ambiente autenticado.
- `core/src/main/java/co/edu/uco/application/usecase/CreateMessageUseCase.java`: persistencia exclusiva con el ambiente autenticado.
- `core/src/main/java/co/edu/uco/application/usecase/validator/message/CreateMessageCompositeValidator.java`: composicion unica de reglas del DTO, catalogos y contexto autenticado mediante `CreateMessageValidationContext`.
- `core/src/main/java/co/edu/uco/application/usecase/validator/message/rule/MessageAuthenticatedEnvironmentRule.java`: ambiente autenticado requerido y coincidencia con el body cuando este lo informa.
- `core/src/main/java/co/edu/uco/application/usecase/validator/message/rule/MessageEnvironmentExistsRule.java`, `MessageApplicationBelongsEnvironmentRule.java` y `MessageFunctionalityBelongsApplicationRule.java`: jerarquia ambiente-aplicacion-funcionalidad con rechazos `403`.
- `infrastructure/src/main/java/co/edu/uco/infraestructure/config/UseCaseRuleConfig.java`: composicion Spring de la regla de contexto para mantener `core` libre de frameworks.
- `infrastructure/src/main/java/co/edu/uco/infraestructure/primaryadapters/controller/CreateMessageControllerImpl.java`: extraccion del ambiente autenticado desde la peticion.
- `utils/src/main/java/co/edu/uco/crosscutting/catalog/MessageCatalogCodeEnum.java`: codigos `FUN_145` y `FUN_146` para las nuevas reglas y correccion de la descripcion de `FUN_036`.
- `deployment/docker/scripts/redis/CatalogMessageInit.sh`: mensajes funcionales para contexto y funcionalidad no autorizados, y mensajes tecnicos saneados para no solicitar valores sensibles.
- `infrastructure/src/main/java/co/edu/uco/infraestructure/secondaryadapters/security/SecurityAdapter.java`: validacion del token simulado sin registrar su valor.
- `infrastructure/src/main/java/co/edu/uco/infraestructure/secondaryadapters/encryption/JavaSecurityEncryptTokenAdapter.java`: errores criptograficos sin claves privadas, firmas ni nombres de secreto como argumentos del logger.
- `infrastructure/src/main/java/co/edu/uco/infraestructure/secondaryadapters/repository/surreal/impl/TokenSurrealRepositoryAdapterImpl.java`: logs genericos sin ID ni consulta SurrealQL del token.
- `infrastructure/src/main/java/co/edu/uco/infraestructure/secondaryadapters/presenter/rest/HttpPresenterAdapter.java`: respuestas HTTP escritas al cliente sin registrar el cuerpo serializado y respeto del estado `403` tipado.
- `utils/src/main/java/co/edu/uco/crosscutting/exceptions/UnauthorizedException.java`: excepcion de autenticacion con estado HTTP `401`.
- `utils/src/main/java/co/edu/uco/crosscutting/exceptions/ForbiddenException.java`: excepcion de autorizacion con estado HTTP `403`.
- `infrastructure/src/main/java/co/edu/uco/infraestructure/primaryadapters/interceptors/TokenHeaderInterceptor.java`: respuesta `401` para token ausente, invalido, vencido o revocado durante la resolucion del ambiente.
- `ArquitecturaEjemplo.md`: documentacion actualizada de estados de seguridad y traduccion de excepciones HTTP.
- `core/src/test/java/co/edu/uco/application/usecase/CreateMessageUseCaseTest.java`: propagacion y persistencia del ambiente autenticado.
- `core/src/test/java/co/edu/uco/application/usecase/validator/message/CreateMessageCompositeValidatorTest.java`: orden y cortocircuito de campos, catalogos y contexto autenticado.
- `core/src/test/java/co/edu/uco/application/usecase/validator/message/rule/`: cobertura individual de ambiente autenticado, jerarquia, escenarios `403`, listas nulas y UUID equivalentes con distinta capitalizacion.
- `core/src/test/java/co/edu/uco/application/primaryports/facade/message/impl/CreateMessageUseCaseFacadeImplTest.java`: propagacion de facade a caso de uso.
- `infrastructure/src/test/java/co/edu/uco/infraestructure/primaryadapters/controller/CreateMessageControllerImplTest.java`: uso del ambiente autenticado por el controller.
- `infrastructure/src/test/java/co/edu/uco/infraestructure/secondaryadapters/security/SecurityAdapterTest.java`: verifica validacion valida e invalida sin pasar el token al logger.
- `infrastructure/src/test/java/co/edu/uco/infraestructure/secondaryadapters/encryption/JavaSecurityEncryptTokenAdapterTest.java`: verifica errores sin material criptografico como argumentos del logger.
- `infrastructure/src/test/java/co/edu/uco/infraestructure/secondaryadapters/repository/surreal/impl/TokenSurrealRepositoryAdapterImplTest.java`: verifica mensajes genericos al persistir y consultar tokens.
- `infrastructure/src/test/java/co/edu/uco/infraestructure/secondaryadapters/presenter/rest/HttpPresenterAdapterTest.java`: verifica que los cuerpos de respuesta no se envian al logger y que `ForbiddenException` produce `403`.
- `utils/src/test/java/co/edu/uco/crosscutting/exceptions/UnauthorizedExceptionTest.java`: verifica mensaje, tipo, ubicacion y estado `401`.
- `utils/src/test/java/co/edu/uco/crosscutting/exceptions/ForbiddenExceptionTest.java`: verifica mensaje, tipo, ubicacion y estado `403`.
- `infrastructure/src/test/java/co/edu/uco/infraestructure/primaryadapters/interceptors/TokenHeaderInterceptorTest.java`: verifica `401` en todos los rechazos del token legado.
- `core/src/main/java/co/edu/uco/application/usecase/domain/security/ExternalIdentity.java`: identidad externa inmutable con emisor, sujeto, correo y expiracion, sin contexto de negocio ni tipo de principal.
- `core/src/main/java/co/edu/uco/application/secondaryports/security/ExternalIdentityResolverPort.java`: puerto independiente del proveedor para validar y resolver identidades.
- `core/src/main/java/co/edu/uco/application/secondaryports/security/SecurityPort.java`: retirado junto con el contrato obsoleto de generacion de tokens simulados.
- `infrastructure/src/main/java/co/edu/uco/infraestructure/secondaryadapters/security/SimulatedExternalIdentityAdapter.java`: resolucion simulada, validacion de token y expiracion UTC sin registrar ni cachear credenciales.
- `infrastructure/src/main/java/co/edu/uco/infraestructure/secondaryadapters/security/SecurityAdapter.java`: retirado junto con el token fijo y su activacion por defecto.
- `infrastructure/src/main/java/co/edu/uco/infraestructure/primaryadapters/interceptors/ExternalIdentityInterceptor.java`: lectura estricta de `Authorization: Bearer`, respuesta `401` y almacenamiento exclusivo de la identidad resuelta en la peticion.
- `infrastructure/src/main/java/co/edu/uco/infraestructure/config/ExternalIdentityWebConfig.java`: registro condicional del interceptor de identidad para la API v1.
- `infrastructure/src/main/java/co/edu/uco/infraestructure/config/InfrastructureConstant.java`: nombre estable del atributo HTTP de identidad externa.
- `infrastructure/src/main/resources/application.properties`: simulador deshabilitado por defecto y configuracion mediante variables externas.
- `core/src/test/java/co/edu/uco/application/usecase/domain/security/ExternalIdentityTest.java`: verifica todos los atributos del principal y la ausencia de contexto en el modelo.
- `infrastructure/src/test/java/co/edu/uco/infraestructure/secondaryadapters/security/SimulatedExternalIdentityAdapterTest.java`: verifica activacion explicita, token valido, invalido, vacio, expirado y limite exacto de expiracion.
- `infrastructure/src/test/java/co/edu/uco/infraestructure/primaryadapters/interceptors/ExternalIdentityInterceptorTest.java`: verifica Bearer valido y malformado, rechazo `401` y convivencia sin header externo.
- `infrastructure/src/test/java/co/edu/uco/infraestructure/config/ExternalIdentityWebConfigTest.java`: verifica el registro del interceptor para la API v1.
- `infrastructure/src/test/java/co/edu/uco/infraestructure/secondaryadapters/security/SecurityAdapterTest.java`: retirado con el adaptador obsoleto.
- `core/src/main/java/co/edu/uco/application/usecase/domain/aggregate/entities/ExternalIdentityEntity.java`: entidad persistente de identidad externa con llave compuesta `issuer + subject` y correo informativo.
- `core/src/main/java/co/edu/uco/application/secondaryports/repository/ExternalIdentityRepository.java`: puerto de consulta por `issuer + subject` y persistencia de identidades externas.
- `infrastructure/src/main/java/co/edu/uco/infraestructure/secondaryadapters/repository/surreal/model/ExternalIdentitySurrealModel.java`: representacion persistente de ExternalIdentity con campos `issuer`, `subject` y `email`.
- `infrastructure/src/main/java/co/edu/uco/infraestructure/secondaryadapters/repository/data/ExternalIdentitySurrealMapper.java`: conversion entre `ExternalIdentityEntity` y `ExternalIdentitySurrealModel`.
- `infrastructure/src/main/java/co/edu/uco/infraestructure/secondaryadapters/repository/surreal/impl/ExternalIdentitySurrealRepositoryAdapterImpl.java`: adaptador con consulta por `issuer + subject`, create, update y manejo de `NONE` para email nulo.
- `deployment/docker/scripts/surreal/surreal-init.surql`: tabla `external_identity` SCHEMAFULL, campos, indice unico compuesto `(issuer, subject)` y campos de auditoria.
- `core/src/test/java/co/edu/uco/application/usecase/domain/aggregate/entities/ExternalIdentityEntityTest.java`: cobertura de identificador, normalizacion de issuer/subject/email y aceptacion de null email.
- `infrastructure/src/test/java/co/edu/uco/infraestructure/secondaryadapters/repository/surreal/model/ExternalIdentitySurrealModelTest.java`: cobertura de construccion, normalizacion, null email y valores por defecto.
- `infrastructure/src/test/java/co/edu/uco/infraestructure/secondaryadapters/repository/data/ExternalIdentitySurrealMapperTest.java`: cobertura del mapeo bidireccional y roundtrip.
- `infrastructure/src/test/java/co/edu/uco/infraestructure/secondaryadapters/repository/surreal/impl/ExternalIdentitySurrealRepositoryAdapterImplTest.java`: cobertura de find por issuer+subject, create con escape de slash y comilla, update con campos exactos, logs saneados y excepcion tecnica.
- `core/src/main/java/co/edu/uco/application/usecase/domain/aggregate/entities/OrganizationEntity.java`: entidad de Organizacion integrada con la jerarquia `Entity<UUID>` del proyecto.
- `core/src/main/java/co/edu/uco/application/secondaryports/repository/OrganizationRepository.java`: puerto de creacion y consulta de organizaciones por identificador o nombre.
- `core/src/test/java/co/edu/uco/application/usecase/domain/aggregate/entities/OrganizationEntityTest.java`: cobertura de identificador, nombre normalizado y valores por defecto sin dependencias de frameworks.
- `infrastructure/src/main/java/co/edu/uco/infraestructure/secondaryadapters/repository/surreal/model/OrganizationSurrealModel.java`: representacion persistente de Organizacion siguiendo los modelos SurrealDB existentes.
- `infrastructure/src/main/java/co/edu/uco/infraestructure/secondaryadapters/repository/data/OrganizationSurrealMapper.java`: conversion explicita entre el modelo SurrealDB y `OrganizationEntity`.
- `infrastructure/src/main/java/co/edu/uco/infraestructure/secondaryadapters/repository/surreal/impl/OrganizationSurrealRepositoryAdapterImpl.java`: adaptador del puerto de Organizacion con consulta por identificador, consulta por nombre y persistencia.
- `deployment/docker/scripts/surreal/surreal-init.surql`: definicion `SCHEMAFULL` de `organization`, campos de auditoria e indice unico por nombre.
- `infrastructure/src/test/java/co/edu/uco/infraestructure/secondaryadapters/repository/surreal/model/OrganizationSurrealModelTest.java`: cobertura de construccion, normalizacion y valores por defecto del modelo persistente.
- `infrastructure/src/test/java/co/edu/uco/infraestructure/secondaryadapters/repository/data/OrganizationSurrealMapperTest.java`: cobertura del mapeo bidireccional de Organizacion.
- `infrastructure/src/test/java/co/edu/uco/infraestructure/secondaryadapters/repository/surreal/impl/OrganizationSurrealRepositoryAdapterImplTest.java`: cobertura de consultas, escape SurrealQL, persistencia, logs saneados y error tecnico.
- `core/src/main/java/co/edu/uco/application/primaryports/dto/organization/CreateOrganizationDTO.java`: entrada normalizada para registrar organizaciones.
- `core/src/main/java/co/edu/uco/application/primaryports/facade/organization/CreateOrganizationUseCaseFacade.java`: puerto primario del registro de Organizacion.
- `core/src/main/java/co/edu/uco/application/primaryports/facade/organization/impl/CreateOrganizationUseCaseFacadeImpl.java`: delegacion hacia el puerto interno de manejo.
- `core/src/main/java/co/edu/uco/application/usecase/handling/HandlingCreateOrganizationPort.java`: contrato interno del caso de uso.
- `core/src/main/java/co/edu/uco/application/usecase/CreateOrganizationUseCase.java`: validacion, construccion de `OrganizationEntity`, persistencia y traduccion de errores tecnicos.
- `core/src/main/java/co/edu/uco/application/usecase/validator/organization/CreateOrganizationCompositeValidator.java`: composicion ordenada de nombre requerido, longitud maxima y duplicidad.
- `core/src/main/java/co/edu/uco/application/usecase/validator/organization/rule/OrganizationNameRequiredRule.java`: obligatoriedad del nombre con `FUN_147`.
- `core/src/main/java/co/edu/uco/application/usecase/validator/organization/rule/OrganizationNameMaxLengthRule.java`: maximo de 50 caracteres con `FUN_148`.
- `core/src/main/java/co/edu/uco/application/usecase/validator/organization/rule/OrganizationNameDuplicatedRule.java`: rechazo de duplicados mediante `OrganizationRepository` y `FUN_149`.
- `infrastructure/src/main/java/co/edu/uco/infraestructure/config/UseCaseRuleConfig.java`: composicion Spring de reglas, composite, caso de uso y facade fuera de `core`.
- `utils/src/main/java/co/edu/uco/crosscutting/catalog/MessageCatalogCodeEnum.java`: codigos `FUN_147`, `FUN_148` y `FUN_149` para las reglas de Organizacion.
- `deployment/docker/scripts/redis/CatalogMessageInit.sh`: mensajes funcionales de nombre requerido, longitud maxima y duplicidad.
- `core/src/test/java/co/edu/uco/application/primaryports/dto/organization/CreateOrganizationDTOTest.java`: cobertura de valores por defecto, normalizacion y builder.
- `core/src/test/java/co/edu/uco/application/primaryports/facade/organization/impl/CreateOrganizationUseCaseFacadeImplTest.java`: delegacion y propagacion de errores de dominio.
- `core/src/test/java/co/edu/uco/application/usecase/CreateOrganizationUseCaseTest.java`: persistencia, UUID generado, limites, excepciones tipadas y logs saneados.
- `core/src/test/java/co/edu/uco/application/usecase/validator/organization/CreateOrganizationCompositeValidatorTest.java`: orden, entrada nula y cortocircuito de reglas.
- `core/src/test/java/co/edu/uco/application/usecase/validator/organization/rule/`: pruebas individuales de nombre requerido, longitud maxima y duplicidad.
- `infrastructure/src/test/java/co/edu/uco/infraestructure/config/UseCaseRuleConfigTest.java`: composicion de composites y casos de uso con puertos simulados, sin beans legacy.
- `utils/src/test/java/co/edu/uco/crosscutting/catalog/MessageCatalogCodeEnumTest.java`: verificacion del ultimo codigo funcional de Organizacion.
- `core/src/main/java/co/edu/uco/application/primaryports/dto/application/CreateApplicationDTO.java`: incorpora `organizationId` como parte obligatoria del contrato de creacion.
- `core/src/main/java/co/edu/uco/application/secondaryports/entity/ApplicationData.java`: referencia tipada a `OrganizationEntity` preservando constructores parciales existentes.
- `core/src/main/java/co/edu/uco/application/usecase/validator/application/rule/ApplicationOrganizationIdRequiredRule.java`: presencia de Organizacion con `FUN_150`.
- `core/src/main/java/co/edu/uco/application/usecase/validator/application/rule/ApplicationOrganizationIdUuidRule.java`: formato UUID mediante `ValidUuidSpecification` y `FUN_038`.
- `core/src/main/java/co/edu/uco/application/usecase/validator/application/rule/ApplicationOrganizationExistsRule.java`: existencia mediante `OrganizationRepository` y `FUN_151`.
- `core/src/main/java/co/edu/uco/application/usecase/validator/application/CreateApplicationCompositeValidator.java`: integra las tres reglas de Organizacion antes de catalogos y duplicidad.
- `core/src/main/java/co/edu/uco/application/usecase/CreateApplicationUseCase.java`: construye la Aplicacion con su `OrganizationEntity` validada.
- `core/src/main/java/co/edu/uco/application/primaryports/facade/application/impl/CreateApplicationUseCaseFacadeImpl.java`: retira la anotacion Spring para mantener la composicion en infraestructura.
- `infrastructure/src/main/java/co/edu/uco/infraestructure/config/UseCaseRuleConfig.java`: compone regla, composite, caso de uso y facade de Aplicacion mediante beans explicitos.
- `infrastructure/src/main/java/co/edu/uco/infraestructure/secondaryadapters/repository/surreal/impl/ApplicationSurrealRepositoryAdapterImpl.java`: persiste y recupera `organization_id` como referencia SurrealDB.
- `infrastructure/src/main/java/co/edu/uco/infraestructure/secondaryadapters/repository/surreal/impl/ApplicationCatalogSurrealAdapter.java`: conserva la referencia de Organizacion al consultar aplicaciones.
- `infrastructure/src/main/java/co/edu/uco/infraestructure/secondaryadapters/repository/surreal/impl/SurrealDomainEventProjectionConsumer.java`: proyecta identificador y datos basicos de Organizacion en `application_document`.
- `deployment/docker/scripts/surreal/surreal-init.surql`: relacion obligatoria `application.organization_id`, indices y campo de la proyeccion documental.
- `utils/src/main/java/co/edu/uco/crosscutting/catalog/MessageCatalogCodeEnum.java`: codigos `FUN_150` y `FUN_151` para Organizacion requerida e inexistente.
- `deployment/docker/scripts/redis/CatalogMessageInit.sh`: mensajes funcionales de la relacion Aplicacion-Organizacion.
- `core/src/test/java/co/edu/uco/application/secondaryports/entity/ApplicationDataTest.java`: asociacion tipada y compatibilidad de constructores parciales.
- `core/src/test/java/co/edu/uco/application/usecase/validator/application/rule/ApplicationOrganizationIdRequiredRuleTest.java`, `ApplicationOrganizationIdUuidRuleTest.java` y `ApplicationOrganizationExistsRuleTest.java`: presencia, UUID, existencia y cortocircuitos.
- `core/src/test/java/co/edu/uco/application/primaryports/dto/application/CreateApplicationDTOTest.java`: valores por defecto y normalizacion de `organizationId`.
- `core/src/test/java/co/edu/uco/application/usecase/validator/application/CreateApplicationCompositeValidatorTest.java`: orden y cortocircuito de la nueva regla.
- `core/src/test/java/co/edu/uco/application/usecase/CreateApplicationUseCaseTest.java`: propagacion del identificador de Organizacion a la entidad persistida.
- `infrastructure/src/test/java/co/edu/uco/infraestructure/secondaryadapters/repository/surreal/impl/ApplicationSurrealRepositoryAdapterImplTest.java`: mapeo y `UPSERT` exacto con `organization_id`.
- `infrastructure/src/test/java/co/edu/uco/infraestructure/secondaryadapters/repository/surreal/impl/ApplicationCatalogSurrealAdapterTest.java`: consulta de catalogo con referencia de Organizacion valida y malformada.
- `infrastructure/src/test/java/co/edu/uco/infraestructure/secondaryadapters/repository/surreal/impl/SurrealDomainEventProjectionConsumerTest.java`: proyeccion de `organization_id` y Organizacion embebida.
- `infrastructure/src/test/java/co/edu/uco/infraestructure/config/UseCaseRuleConfigTest.java`: composicion completa del flujo de creacion de Aplicacion.
- `core/src/main/java/co/edu/uco/application/usecase/domain/aggregate/entities/MembershipEntity.java`: relacion de una identidad externa con una Organizacion.
- `core/src/main/java/co/edu/uco/application/usecase/domain/aggregate/entities/RoleEntity.java`: rol de negocio independiente del proveedor de identidad.
- `core/src/main/java/co/edu/uco/application/usecase/domain/aggregate/entities/PermissionEntity.java`: permiso tipado de negocio.
- `core/src/main/java/co/edu/uco/application/usecase/domain/aggregate/entities/RoleAssignmentEntity.java`: asignacion de rol perteneciente exclusivamente a una Organizacion.
- `core/src/main/java/co/edu/uco/application/usecase/domain/security/PermissionCode.java`: catalogo tipado de los siete permisos iniciales.
- `core/src/main/java/co/edu/uco/application/secondaryports/security/AuthorizationQueryPort.java`: puerto para consultar permisos y recursos autorizados sin depender de SurrealDB.
- `core/src/main/java/co/edu/uco/application/usecase/validator/authorization/AuthorizationCompositeValidator.java`: compone identidad requerida y permiso mediante `AuthorizationValidationContext`, preservando ausencia de identidad (`401`) y falta de permiso (`403`).
- `core/src/main/java/co/edu/uco/application/usecase/validator/authorization/rule/ExternalIdentityRequiredRule.java` y `AuthorizationPermissionRule.java`: reglas concretas para `FUN_152` y `FUN_153`.
- `infrastructure/src/main/java/co/edu/uco/infraestructure/secondaryadapters/repository/surreal/impl/AuthorizationQuerySurrealAdapter.java`: resolucion de permisos por `issuer + subject`, membresia, rol, permiso y Organizacion del recurso.
- `core/src/main/java/co/edu/uco/application/usecase/FindCatalogUseCase.java`: filtrado de Aplicaciones y Ambientes y proteccion del catalogo de Funcionalidades para identidades externas.
- `core/src/main/java/co/edu/uco/application/usecase/handling/HandlingFindCatalogPort.java`, `core/src/main/java/co/edu/uco/application/primaryports/facade/catalog/FindCatalogUseCaseFacade.java` y su implementacion: propagacion tipada de identidad hacia los catalogos jerarquicos.
- `infrastructure/src/main/java/co/edu/uco/infraestructure/primaryadapters/controller/CatalogControllerImpl.java`: entrega la identidad resuelta al facade sin implementar autorizacion en el controller.
- `infrastructure/src/main/java/co/edu/uco/infraestructure/config/UseCaseRuleConfig.java`: composicion Spring de la regla, caso de uso y facade de catalogos fuera de `core`.
- `deployment/docker/scripts/surreal/surreal-init.surql`: tablas, relaciones e indices de membresias, roles, permisos, relaciones rol-permiso y asignaciones por Organizacion; carga de permisos iniciales.
- `utils/src/main/java/co/edu/uco/crosscutting/catalog/MessageCatalogCodeEnum.java` y `deployment/docker/scripts/redis/CatalogMessageInit.sh`: mensajes `FUN_152` y `FUN_153` para autenticacion requerida y permiso denegado.
- Pruebas de autorizacion: cobertura de entidades y enums, regla `401`/`403`, autorizacion por Organizacion, aislamiento entre organizaciones y aplicaciones, filtrado de catalogos, propagacion HTTP, composicion Spring y consultas SurrealQL.
- `core/src/main/java/co/edu/uco/application/usecase/domain/aggregate/entities/ActiveContextEntity.java`: contexto activo persistente asociado a una identidad externa, Organizacion, Aplicacion, Ambiente y fecha UTC de actualizacion.
- `core/src/main/java/co/edu/uco/application/secondaryports/repository/ActiveContextRepository.java`: puerto de persistencia del ultimo contexto por identidad externa.
- `core/src/main/java/co/edu/uco/application/secondaryports/cache/ActiveContextCachePort.java`: puerto de cache distribuida tipado por `ExternalIdentity`, sin aceptar tokens.
- `infrastructure/src/main/java/co/edu/uco/infraestructure/secondaryadapters/repository/surreal/model/ActiveContextSurrealModel.java`: representacion persistente del contexto activo.
- `infrastructure/src/main/java/co/edu/uco/infraestructure/secondaryadapters/repository/data/ActiveContextSurrealMapper.java`: conversion explicita entre entidad y modelo SurrealDB.
- `infrastructure/src/main/java/co/edu/uco/infraestructure/secondaryadapters/repository/surreal/impl/ActiveContextSurrealRepositoryAdapterImpl.java`: consulta y `UPSERT` determinista por identificador de identidad externa.
- `infrastructure/src/main/java/co/edu/uco/infraestructure/secondaryadapters/cache/ActiveContextRedisAdapter.java`: cache Redis de mejor esfuerzo, TTL configurable y claves SHA-256 sin identidad ni token visibles.
- `infrastructure/src/main/java/co/edu/uco/infraestructure/secondaryadapters/cache/ActiveContextCacheModel.java`: formato interno validado para serializar el contexto en Redis.
- `infrastructure/src/main/java/co/edu/uco/infraestructure/config/ActiveContextCacheProperties.java` y `application.properties`: configuracion externa del TTL mediante `ACTIVE_CONTEXT_CACHE_TTL_SECONDS`.
- `deployment/docker/scripts/surreal/surreal-init.surql`: tabla `active_context`, referencias jerarquicas e indice unico por identidad externa.
- Pruebas del primer incremento de contexto activo: entidad de dominio, modelo y mapper SurrealDB, consultas y errores tecnicos del repositorio, propiedades de cache, TTL, aislamiento de claves, colisiones, entradas malformadas y tolerancia a fallos Redis.
- `core/src/main/java/co/edu/uco/application/usecase/ActiveContextUseCase.java`: listado de contextos autorizados, recuperacion cache-aside con reautorizacion y seleccion persistente antes de invalidar cache.
- `core/src/main/java/co/edu/uco/application/usecase/validator/context/SelectActiveContextCompositeValidator.java` y `context/rule/`: reglas ordenadas de identificadores, UUID, existencia y jerarquia Organizacion-Aplicacion-Ambiente.
- `core/src/main/java/co/edu/uco/application/usecase/validator/authorization/rule/ExternalIdentityRequiredRule.java`: rechazo tipado `401` cuando no existe identidad externa.
- `infrastructure/src/main/java/co/edu/uco/infraestructure/primaryadapters/MeContextController.java` y su implementacion: endpoints de contextos disponibles, consulta y seleccion del contexto activo sin reglas de negocio en el controller.
- `infrastructure/src/main/java/co/edu/uco/infraestructure/config/UseCaseRuleConfig.java`: composicion Spring del caso de uso, facade y reglas fuera de `core`.
- `infrastructure/src/main/resources/static/openapi.yaml`: contrato de los tres endpoints, esquema Bearer neutral respecto a JWT u opaco y respuestas `401`, `403`, `404`, `409` y `422` aplicables.
- `utils/src/main/java/co/edu/uco/crosscutting/catalog/MessageCatalogCodeEnum.java` y `deployment/docker/scripts/redis/CatalogMessageInit.sh`: mensajes `FUN_154` a `FUN_160` para ausencia de contexto, identificadores, recursos y conflictos de jerarquia.
- Pruebas de cierre de contexto activo: casos de uso, facade, reglas individuales y composite, propagacion HTTP, composicion Spring y consulta SurrealDB de Aplicacion por identificador.
- `core/src/main/java/co/edu/uco/application/usecase/domain/security/MessageAccessContext.java`: contrato tipado de entrada con ambiente legado e identidad externa opcional.
- `core/src/main/java/co/edu/uco/application/usecase/security/MessageEnvironmentResolver.java` y `MessageEnvironmentResolverImpl.java`: resolucion de alcance legado o contexto activo con autorizacion `MESSAGE_READ`/`MESSAGE_TRANSLATE`, `401` `FUN_152` sin identidad ni ambiente y propagacion de `403`.
- `core/src/main/java/co/edu/uco/application/usecase/handling/HandlingFindMessageEnvironmentPort.java`, `HandlingFindMessageByCodeAndEnvironmentPort.java` y `HandlingTranslateMessageByCodeAndEnvironmentPort.java`: firmas migradas a `MessageAccessContext`.
- `core/src/main/java/co/edu/uco/application/primaryports/facade/message/` (interfaces e impl de listado, consulta y traduccion): propagacion tipada del contexto hacia los casos de uso.
- `core/src/main/java/co/edu/uco/application/usecase/FindMessageByEnvironmentUseCase.java`, `FindMessageByCodeAndEnvironmentUseCase.java` y `TranslateMessageByCodeAndEnvironmentUseCase.java`: resolucion de ambiente autorizado antes del repositorio; propagacion de `CrossWordsException` en consulta por codigo para no envolver autorizacion.
- `infrastructure/src/main/java/co/edu/uco/infraestructure/primaryadapters/controller/MessagesControllerImpl.java`: construccion de `MessageAccessContext` desde atributos HTTP sin reglas de negocio.
- `infrastructure/src/main/java/co/edu/uco/infraestructure/primaryadapters/interceptors/TokenHeaderInterceptor.java`: omision del token legado cuando ya existe identidad externa resuelta.
- `infrastructure/src/main/java/co/edu/uco/infraestructure/config/ExternalIdentityWebConfig.java` y `WebConfig.java`: orden de interceptores (`-100` identidad externa, `0` token legado) para convivencia.
- `infrastructure/src/main/java/co/edu/uco/infraestructure/config/UseCaseRuleConfig.java`: bean `messageEnvironmentResolver` fuera de `core`.
- `infrastructure/src/main/resources/static/openapi.yaml`: los tres endpoints de mensajes aceptan `tokenAuth` y `bearerAuth`, con respuesta `401` y descripciones de convivencia.
- `core/src/test/java/co/edu/uco/application/usecase/security/MessageEnvironmentResolverImplTest.java`: legado sin autorizacion, contexto activo con permiso, `401` `FUN_152` y `403` propagado.
- `core/src/test/java/co/edu/uco/application/usecase/FindMessageByEnvironmentUseCaseTest.java`, `FindMessageByCodeAndEnvironmentUseCaseTest.java` y `TranslateMessageByCodeAndEnvironmentUseCaseTest.java`: mock de `MessageEnvironmentResolver` con `MessageAccessContext`, excepciones de dominio y `lenient()` donde el resolver no se invoca.
- `core/src/test/java/co/edu/uco/application/primaryports/facade/message/impl/` (los tres tests de facade): delegacion del contexto al puerto interno.
- `infrastructure/src/test/java/co/edu/uco/infraestructure/primaryadapters/controller/MessagesControllerImplTest.java`: captors de `MessageAccessContext` con ambiente legado e identidad externa.
- `infrastructure/src/test/java/co/edu/uco/infraestructure/primaryadapters/interceptors/TokenHeaderInterceptorTest.java`: omision del token legado con identidad externa resuelta (`verifyNoInteractions`).
- `infrastructure/src/test/java/co/edu/uco/infraestructure/config/ExternalIdentityWebConfigTest.java` y `WebConfigTest.java`: orden de registro de interceptores.
- `infrastructure/src/test/java/co/edu/uco/infraestructure/config/UseCaseRuleConfigTest.java`: composicion del bean `messageEnvironmentResolver`.
- `core/src/main/java/co/edu/uco/application/primaryports/facade/message/CreateMessageUseCaseFacade.java` y `impl/CreateMessageUseCaseFacadeImpl.java`: propagacion de `MessageAccessContext` en la creacion.
- `core/src/main/java/co/edu/uco/application/usecase/handling/HandlingCreateMessagePort.java`: contrato de creacion tipado con `MessageAccessContext`.
- `core/src/main/java/co/edu/uco/application/usecase/CreateMessageUseCase.java`: resolucion de ambiente con `MessageEnvironmentResolver` y permiso `MESSAGE_CREATE` antes de validar y persistir.
- `core/src/main/java/co/edu/uco/application/usecase/validator/message/CreateMessageCompositeValidator.java`: `environmentId` del body permanece opcional y las reglas contextuales forman parte del mismo composite.
- `core/src/main/java/co/edu/uco/application/usecase/validator/message/rule/MessageAuthenticatedEnvironmentRule.java`: exige ambiente autenticado y compara el ambiente del body solo cuando este se informa.
- `core/src/main/java/co/edu/uco/application/usecase/FindCatalogUseCase.java`: exige que `applicationId` del catalogo coincida con `ActiveContext.applicationId` cuando hay identidad; `403` `FUN_153` en desalineacion.
- `core/src/main/java/co/edu/uco/application/primaryports/facade/application|environment|functionality/` (interfaces e impl de creacion): propagacion de `ExternalIdentity`.
- `core/src/main/java/co/edu/uco/application/usecase/handling/HandlingCreate{Application,Environment,Functionality}Port.java`: firmas con `ExternalIdentity`.
- `core/src/main/java/co/edu/uco/application/usecase/Create{Application,Functionality}UseCase.java`: autorizacion interna `authorizeAgainstActiveContext` con `APPLICATION_CREATE`/`ORGANIZATION` y `FUNCTIONALITY_CREATE`/`APPLICATION`; identity null conserva el flujo legado. La aplicacion aprovisiona sus tres ambientes por defecto.
- `infrastructure/src/main/java/co/edu/uco/infraestructure/primaryadapters/controller/Create{Message,Application,Environment,Functionality}ControllerImpl.java`: construccion de `MessageAccessContext` o `ExternalIdentity` desde atributos HTTP sin reglas de negocio.
- `infrastructure/src/main/java/co/edu/uco/infraestructure/config/UseCaseRuleConfig.java`: beans `handlingFindCatalogPort` y `handlingCreateApplicationPort` con `HandlingActiveContextPort`, `AuthorizationCompositeValidator` y `CatalogPort`.
- `core/src/test/java/co/edu/uco/application/primaryports/facade/message/impl/CreateMessageUseCaseFacadeImplTest.java` y los de facade de application/functionality: delegacion con contexto tipado e identity null legado.
- `core/src/test/java/co/edu/uco/application/usecase/Create{Message,Application,Environment,Functionality}UseCaseTest.java` y `FindCatalogUseCaseTest.java`: pruebas de aislamiento (legacy null, match con `ActiveContext`, `403` `FUN_153`, permisos correctos, composite de autorizacion denegando).
- `core/src/test/java/co/edu/uco/application/usecase/validator/message/CreateMessageCompositeValidatorTest.java` y pruebas de `validator/message/rule/`: environmentId opcional, match condicional, jerarquia y cortocircuito.
- `infrastructure/src/test/java/co/edu/uco/infraestructure/primaryadapters/controller/CreateMessageControllerImplTest.java`: captors de `MessageAccessContext` legado y autenticado, y propagacion de fallos del facade.
- `infrastructure/src/test/java/co/edu/uco/infraestructure/config/UseCaseRuleConfigTest.java`: wiring de catalogos y creacion de Aplicacion con los nuevos puertos.

### Pruebas ejecutadas

- `VerifyAccessUseCaseTest`: 7 pruebas, 0 fallos, 0 errores y 0 omitidas.
- Pruebas focalizadas del listado: 20 pruebas, 0 fallos, 0 errores y 0 omitidas.
- Pruebas focalizadas de creacion: 32 pruebas, 0 fallos, 0 errores y 0 omitidas.
- Pruebas focalizadas de saneamiento de logs: 39 pruebas, 0 fallos, 0 errores y 0 omitidas.
- Pruebas focalizadas de normalizacion HTTP: 41 pruebas, 0 fallos, 0 errores y 0 omitidas.
- Reactor completo con `clean verify`: `utils`, `core` e `infrastructure` finalizaron correctamente.
- Suite `utils`: 186 pruebas, 0 fallos, 0 errores y 0 omitidas.
- Suite `core`: 446 pruebas, 0 fallos, 0 errores y 0 omitidas.
- Suite `infrastructure`: 357 pruebas, 0 fallos, 0 errores y 0 omitidas.
- JaCoCo: los controles de cobertura de linea y rama, configurados con minimo de 80 %, se cumplieron en todos los modulos.
- Auditoria de `VerifyAccessUseCaseTest`: prueba un resultado real, usa excepcion de dominio, no usa infraestructura concreta y evita acceder a secretos o criptografia cuando el token esta vencido.
- Auditoria del listado: las pruebas verifican la ruta protegida, el origen autenticado del ambiente y la ausencia del fallback global; no contienen asserts triviales ni pruebas deshabilitadas.
- Auditoria de creacion: las pruebas usan puertos simulados, excepciones de dominio y cubren completamente las lineas y ramas de la nueva regla de contexto.
- Auditoria de logs: las pruebas verifican argumentos concretos del `LoggingPort`, mockean SurrealDB y no dependen de infraestructura real; no contienen asserts triviales ni pruebas deshabilitadas.
- Auditoria HTTP: las pruebas verifican estados reales, excepciones especificas de la jerarquia y puertos simulados; no usan infraestructura concreta ni asserts triviales.
- Pruebas focalizadas de identidad externa: 19 pruebas, 0 fallos, 0 errores y 0 omitidas.
- Reactor completo posterior a la Fase 2 con `clean verify`: `utils`, `core` e `infrastructure` finalizaron correctamente.
- Suite `utils`: 186 pruebas, 0 fallos, 0 errores y 0 omitidas.
- Suite `core`: 449 pruebas, 0 fallos, 0 errores y 0 omitidas.
- Suite `infrastructure`: 371 pruebas, 0 fallos, 0 errores y 0 omitidas.
- JaCoCo posterior a la Fase 2: los controles de cobertura de linea y rama, configurados con minimo de 80 %, se cumplieron en todos los modulos.
- Auditoria de identidad externa: pruebas sin asserts triviales ni deshabilitados, `core` libre de frameworks, excepciones `UnauthorizedException`, activacion condicional real, ramas Bearer y cuerpo HTTP `401` verificados.
- Pruebas focalizadas de Organizacion: 4 pruebas, 0 fallos, 0 errores y 0 omitidas.
- Reactor completo posterior al primer incremento de la Fase 3 con `clean verify`: `utils`, `core` e `infrastructure` finalizaron correctamente.
- Suite `utils`: 186 pruebas, 0 fallos, 0 errores y 0 omitidas.
- Suite `core`: 453 pruebas, 0 fallos, 0 errores y 0 omitidas.
- Suite `infrastructure`: 371 pruebas, 0 fallos, 0 errores y 0 omitidas.
- JaCoCo posterior al primer incremento de la Fase 3: los controles de cobertura de linea y rama, configurados con minimo de 80 %, se cumplieron en todos los modulos.
- Auditoria de `OrganizationEntityTest`: AAA, asserts no triviales, cobertura completa del modelo y ausencia de Spring o adaptadores concretos en `core`.
- Pruebas focalizadas de persistencia de Organizacion: 14 pruebas, 0 fallos, 0 errores y 0 omitidas.
- Reactor completo posterior al segundo incremento de la Fase 3 con `clean verify`: `utils`, `core` e `infrastructure` finalizaron correctamente.
- Suite `utils`: 186 pruebas, 0 fallos, 0 errores y 0 omitidas.
- Suite `core`: 453 pruebas, 0 fallos, 0 errores y 0 omitidas.
- Suite `infrastructure`: 381 pruebas, 0 fallos, 0 errores y 0 omitidas.
- JaCoCo posterior al segundo incremento de la Fase 3: `utils` 94.07 % de lineas y 96.97 % de ramas; `core` 97.78 % de lineas y 90.58 % de ramas; `infrastructure` 88.67 % de lineas y 81.33 % de ramas.
- Auditoria de persistencia de Organizacion: pruebas AAA sin asserts triviales ni deshabilitados, cliente SurrealDB simulado, consultas y mapeos verificados, excepcion `BusinessException` especifica y cobertura completa de las clases nuevas.
- Pruebas focalizadas del registro de Organizacion: 35 ejecuciones, 0 fallos, 0 errores y 0 omitidas entre `utils`, `core` e `infrastructure`.
- Reactor completo posterior al tercer incremento de la Fase 3 con `clean verify`: `utils`, `core` e `infrastructure` finalizaron correctamente.
- Suite `utils`: 186 pruebas, 0 fallos, 0 errores y 0 omitidas.
- Suite `core`: 479 pruebas, 0 fallos, 0 errores y 0 omitidas.
- Suite `infrastructure`: 386 pruebas, 0 fallos, 0 errores y 0 omitidas.
- JaCoCo posterior al tercer incremento de la Fase 3: `utils` 94.11 % de lineas y 96.97 % de ramas; `core` 97.85 % de lineas y 90.85 % de ramas; `infrastructure` 88.70 % de lineas y 81.33 % de ramas.
- Auditoria del registro de Organizacion: pruebas AAA sin asserts triviales ni deshabilitados, `core` sin Spring ni adaptadores concretos, reglas probadas individualmente, composite con orden y cortocircuito, excepciones de dominio y tecnica verificadas, y configuracion probada con puertos simulados.
- Pruebas focalizadas de la relacion Aplicacion-Organizacion: 78 ejecuciones, 0 fallos, 0 errores y 0 omitidas entre `utils`, `core` e `infrastructure`.
- Reactor completo posterior al cuarto incremento de la Fase 3 con `clean verify`: `utils`, `core` e `infrastructure` finalizaron correctamente.
- Suite `utils`: 186 pruebas, 0 fallos, 0 errores y 0 omitidas.
- Suite `core`: 493 pruebas, 0 fallos, 0 errores y 0 omitidas.
- Suite `infrastructure`: 393 pruebas, 0 fallos, 0 errores y 0 omitidas.
- JaCoCo posterior al cuarto incremento de la Fase 3: `utils` 94.13 % de lineas y 96.97 % de ramas; `core` 97.89 % de lineas y 90.97 % de ramas; `infrastructure` 89.02 % de lineas y 81.33 % de ramas.
- Auditoria de Aplicacion-Organizacion: reglas probadas individualmente, orden y cortocircuito del composite, puertos simulados, SurrealQL exacto, proyeccion documental verificada, excepciones especificas y ausencia de dependencias de infraestructura en pruebas de `core`.
- Pruebas focalizadas de persistencia de identidad externa: 29 ejecuciones, 0 fallos, 0 errores y 0 omitidas entre `core` e `infrastructure`.
- Reactor completo posterior al quinto incremento de la Fase 3 con `clean verify`: `utils`, `core` e `infrastructure` finalizaron correctamente.
- Suite `utils`: 186 pruebas, 0 fallos, 0 errores y 0 omitidas.
- Suite `core`: 503 pruebas, 0 fallos, 0 errores y 0 omitidas.
- Suite `infrastructure`: 412 pruebas, 0 fallos, 0 errores y 0 omitidas.
- JaCoCo posterior al quinto incremento de la Fase 3: los controles de cobertura de linea y rama, configurados con minimo de 80 %, se cumplieron en todos los modulos.
- Auditoria de persistencia de identidad externa: pruebas AAA sin asserts triviales ni deshabilitados, `core` sin Spring ni adaptadores concretos, escape SurrealQL verificado, email NONE/null manejado, update restringido a email, logs sin datos sensibles, excepciones `BusinessException` INFRASTRUCTURE verificadas.
- Reactor completo al finalizar la Fase 3 con `clean verify`: `utils`, `core` e `infrastructure` finalizaron correctamente.
- Suite `utils`: 186 pruebas, 0 fallos, 0 errores y 0 omitidas.
- Suite `core`: 537 pruebas, 0 fallos, 0 errores y 0 omitidas.
- Suite `infrastructure`: 423 pruebas, 0 fallos, 0 errores y 0 omitidas.
- JaCoCo al finalizar la Fase 3: `utils` 94.15 % de lineas y 96.97 % de ramas; `core` 98.00 % de lineas y 91.39 % de ramas; `infrastructure` 89.40 % de lineas y 81.26 % de ramas.
- Auditoria final de autorizacion: pruebas AAA sin asserts triviales ni deshabilitados, regla probada directamente con excepciones tipadas, puertos simulados en `core`, cliente SurrealDB simulado en infraestructura, consultas y herencia de alcance verificadas, y Quality Gate de cobertura cumplido en los tres modulos.
- Reactor completo posterior al primer incremento de la Fase 4 con `clean verify`: `utils`, `core` e `infrastructure` finalizaron correctamente.
- Suite `utils`: 186 pruebas, 0 fallos, 0 errores y 0 omitidas.
- Suite `core`: 541 pruebas, 0 fallos, 0 errores y 0 omitidas.
- Suite `infrastructure`: 450 pruebas, 0 fallos, 0 errores y 0 omitidas.
- JaCoCo posterior al primer incremento de la Fase 4: `utils` 94.15 % de lineas y 96.97 % de ramas; `core` 98.02 % de lineas y 91.39 % de ramas; `infrastructure` 89.99 % de lineas y 81.09 % de ramas.
- Auditoria del primer incremento de contexto activo: pruebas AAA sin asserts triviales ni deshabilitados, `core` sin frameworks, SurrealDB y Redis simulados, errores persistentes tipados, fallos de cache no fatales y ausencia de tokens o identidades en claves y logs.
- Reactor completo al finalizar la Fase 4 con `clean verify`: `utils`, `core` e `infrastructure` finalizaron correctamente.
- Suite `utils`: 188 pruebas, 0 fallos, 0 errores y 0 omitidas.
- Suite `core`: 573 pruebas, 0 fallos, 0 errores y 0 omitidas.
- Suite `infrastructure`: 462 pruebas, 0 fallos, 0 errores y 0 omitidas.
- JaCoCo al finalizar la Fase 4: los controles de cobertura de linea y rama, configurados con minimo de 80 %, se cumplieron en los tres modulos.
- Auditoria final de contexto activo: pruebas AAA con resultados observables, reglas probadas directamente, excepciones `UnauthorizedException`, `ForbiddenException`, `NotFoundException`, `BusinessRuleException` y `ConflictException` verificadas segun el escenario, puertos simulados en `core`, clientes externos simulados en infraestructura y sin pruebas deshabilitadas ni asserts triviales.
- Reactor completo del primer incremento de la Fase 5 con `clean verify`: `utils`, `core` e `infrastructure` finalizaron correctamente.
- Suite `utils`: 188 pruebas, 0 fallos, 0 errores y 0 omitidas.
- Suite `core`: 577 pruebas, 0 fallos, 0 errores y 0 omitidas.
- Suite `infrastructure`: 465 pruebas, 0 fallos, 0 errores y 0 omitidas.
- JaCoCo del primer incremento de la Fase 5: `utils` 97.39 % de lineas y 96.97 % de ramas; `core` 96.42 % de lineas y 90.68 % de ramas; `infrastructure` 89.10 % de lineas y 81.15 % de ramas; controles de cobertura con minimo de 80 % cumplidos en los tres modulos.
- Auditoria de pruebas de la Fase 5: pruebas AAA con asserts no triviales, excepciones de la jerarquia de dominio (`UnauthorizedException`, `ForbiddenException`, `BusinessException`, `BusinessRuleException`, `CrossWordsException`), puertos y `MessageEnvironmentResolver` simulados en `core` sin frameworks, `verifyNoInteractions` donde el resolver no debe intervenir, captors de `MessageAccessContext` en el controller, omision del token legado verificada y orden de interceptores (`-100`/`0`) comprobado; sin pruebas deshabilitadas.
- `git diff --check`: sin errores de whitespace (solo avisos LF/CRLF).
- Reactor completo posterior a la unificacion del patron de validacion con `clean install/verify`: `utils`, `core` e `infrastructure` finalizaron correctamente.
- Suite posterior a la unificacion: `utils` 196 pruebas, `core` 1066 pruebas e `infrastructure` 492 pruebas; total 1754, sin fallos, errores ni omitidas.
- JaCoCo posterior a la unificacion: controles de cobertura de linea y rama con minimo de 80 % cumplidos en los tres modulos.
- Auditoria de las reglas migradas: pruebas individuales de `RuleValidator`, authorization, context, message, organization y application; excepciones `401`, `403`, `404`, `409` y `422`, orden, cortocircuito y ausencia de referencias Java a implementaciones legacy verificados.
- Reactor completo del incremento de creacion, catalogos y administracion de la Fase 5 con `clean verify`: `utils`, `core` e `infrastructure` finalizaron correctamente.
- Suite `utils`: 188 pruebas, 0 fallos, 0 errores y 0 omitidas.
- Suite `core`: 603 pruebas, 0 fallos, 0 errores y 0 omitidas.
- Suite `infrastructure`: 467 pruebas, 0 fallos, 0 errores y 0 omitidas.
- JaCoCo del incremento: `utils` 97.39 % de lineas y 96.97 % de ramas; `core` 96.51 % de lineas y 91.22 % de ramas; `infrastructure` 88.93 % de lineas y 81.15 % de ramas; controles de cobertura con minimo de 80 % cumplidos en los tres modulos.
- Auditoria de pruebas del incremento (`unit-test-validator`): 13 clases con veredicto ✅; se corrigio `CreateMessageControllerImplTest` (asserts agrupados con `assertAll`, caso de error `ForbiddenException` y `eq` de `ArgumentMatchers`); sin asserts triviales, `@Disabled`, excepciones genericas ni frameworks en `core`.
- Pruebas focalizadas de las 13 clases auditadas: 0 fallos y 0 errores.
- `git diff --check`: sin errores de whitespace (solo avisos LF/CRLF).

### Pendiente inmediato

La verificacion de codigo de la Fase 7 termino mediante `unit-test-agent` autorizado.
La limpieza de secretos remotos y datos preexistentes no se ejecuta automaticamente. Completar
la integracion por usuario de la Fase 6 sin modificar el componente externo de
seguridad. El endpoint de registro de organizaciones conserva su politica pendiente.

### Archivos y verificacion del retiro de 2026-10-20

- `core/src/main/java`: eliminacion de DTO, facades, casos de uso, reglas, modelos,
  puertos de token/secretos y DTO de claves; `PageBuilder` sale del paquete `token`
  sin cambiar su comportamiento. Contexto local renombrado a `workspaceEnvironmentId`.
- `infrastructure/src/main/java`: retiro de emision, RSA, repositorios SurrealDB y
  API Doppler; `ActiveContextInterceptor` sustituye al filtro de token sin fallback.
- `utils/src/main/java`: retiro de `UtilPairKey` y codigos de catalogo exclusivos.
- `infrastructure/pom.xml`, propiedades, catalogo Redis y seed SurrealDB: retirada
  de dependencias y configuracion exclusivas, tablas y eventos de tokens.
- Compose local/Azure, `deployment/azure/deploy.sh` y pipelines: retiro de la
  credencial de escritura de la app, manteniendo el CLI y las credenciales del PEP.
- OpenAPI, README, arquitectura, guias y este plan: contrato sin token propietario.
- Frontend MessageUcoLab: `src/api.js` sin parametros de token obsoletos y retiro
  de `src/api.js.bak2`, copia versionada sin cambios locales del cliente anterior.
  Los cambios previos del usuario y la sesion BFF se conservan.
- Se retiraron pruebas exclusivas de clases productivas eliminadas y se conservaron
  las pruebas de catalogo, paginacion y comportamiento de negocio; `PageBuilderTest`
  sigue cubriendo paginacion tras el cambio de paquete.
- `ActiveContextInterceptorTest` (8 casos) cubre ruta externa, `Token` y
  `Authorization` no resueltos sin identidad (`401`), `/me` con y sin identidad,
  contexto presente, ausente y errores `401/403`; ningún rechazo continúa la cadena.
  `EnvironmentContextResolverTest` conserva las verificaciones de contexto ausente
  como `UnauthorizedException` y propagación de excepciones de dominio. Se conserva
  `LocalWorkspaceProfileBindingTest` para el límite de perfil y el filtro local.
- `EnumConstantsTest` conserva los asserts de constantes vigentes y elimina solo
  el caso RSA retirado. Búsqueda focalizada de código activo (`core`, `infrastructure`,
  `utils`, recursos/deployment Redis y frontend MessageUcoLab) no encontró referencias
  de implementación RSA/token propietario; `openapi.yaml` mantiene la frase que
  rechaza explícitamente el header legado como contrato, no como implementación.
- Verificación final: el wrapper Maven (`mvnw.cmd clean verify`) con Java 25 terminó con
  `BUILD SUCCESS`. Conteos del resumen Maven: `utils` 208, `core` 1134 e
  `infrastructure` 574 tests; cero fallos, errores u omitidos.
  JaCoCo: utils líneas 521/541 (96.30%), ramas 64/66 (96.97%); core líneas
  1763/1804 (97.73%), ramas 336/342 (98.25%); infrastructure líneas 2133/2327
  (91.66%), ramas 521/614 (84.85%). Los reportes por módulo están en
  `target/site/jacoco/index.html` y `target/site/jacoco/jacoco.xml`.
- JaCoCo de los objetivos nuevos: `ActiveContextInterceptor` líneas 21/21 y ramas
  6/6; `EnvironmentContextResolver` líneas 9/9 y ramas 4/4. Los checks configurados
  de cobertura pasan en `core` e `infrastructure`; `utils` queda según su reporte
  individual sin ampliar alcance.
- Auditoría focalizada `unit-test-validator` de las pruebas nuevas/modificadas en
  esta intervención: ✅ `EnumConstantsTest`, ✅ `ActiveContextInterceptorTest`, ✅
  `EnvironmentContextResolverTest` (revisión de cobertura/escenarios). No se
  encontraron asserts triviales, excepciones genéricas en `assertThrows`,
  `@Disabled` ni mocks de infraestructura real en estos tests. Este veredicto no
  representa una auditoría completa de todas las pruebas del monorepo. Hallazgo
  arquitectónico previo, no ampliado en esta tarea:
  `PageBuilderTest` (igual que tests de paginación y estrategias de catálogo ya
  existentes) utiliza tipos `org.springframework.data` porque `PageBuilder` y los
  puertos actuales exponen `PageRequest`/`Pageable`. Se preservó el test solicitado
  y no se alteró producción fuera de alcance; por la regla estática amplia de la
  skill que prohíbe todo `org.springframework` en `core`, ese grupo conserva la
  salvedad pese a que los tests son unitarios y no levantan contexto.
- Frontend MessageUcoLab: `npm run build` completó con Vite 5.4.21; no hay framework
  de pruebas configurado en `package.json`, por lo que esto verifica build y no
  pruebas automatizadas frontend.
- No se desplegaron servicios, no se ejecutaron pruebas runtime contra el proveedor
  externo, no se purgaron datos/secrets remotos y no se modificó ningún componente,
  despliegue, frontend o Docker de Security. El puente real a `ExternalIdentity`
  fuera de `local-workspace` sigue pendiente y es una dependencia externa.

## Historial de cambios

| Fecha | Fase | Cambio | Estado resultante |
|---|---|---|---|
| 2026-10-20 | Fase 6 / UX de sesion | Por decision del usuario se sustituye la reautenticacion automatica por un popup del frontend MessageUcoLab con boton Volver a Conectar. El 401 no navega, solo bloquea el acceso y pausa SSE; el login se abre exclusivamente al pulsar. Sin cambios en datos ni componentes de seguridad | Implementado; npm run build exitoso y diff --check sin errores. No se crearon ni ejecutaron pruebas; autorizacion pendiente |
| 2026-10-20 | Fase 6 / sesion frontend | Recuperacion centralizada de 401 mediante login publico con retorno, guard persistente contra bucles, bloqueo explicito y pausa de SSE; sin reenvio de mutaciones ni cambios en seguridad. Import Keycloak configura access token de 300 segundos, no timeout de inactividad | Implementado y compilado; pruebas nuevas pendientes de autorizacion, sin validacion runtime de expiracion |
| 2026-10-20 | Fase 7 | Retiro del mecanismo propietario y su cliente obsoleto; pruebas obsoletas retiradas/adaptadas, filtro de contexto externo cubierto, `clean verify` exitoso y cobertura/auditoría registradas; PEP, sesión BFF y componentes externos preservados | Codigo completado y verificado; fase operativa PARCIAL por limpieza remota y validacion runtime no ejecutadas |
| 2026-09-21 | Planificacion | Creacion del documento; se registran decisiones, fases, riesgos y criterios de salida | Fase 0 `PARCIAL`; Fase 1 `PENDIENTE` |
| 2026-09-21 | Fase 1 | Se valida la expiracion antes de acceder a secretos o criptografia y se agregan pruebas de vigencia y limite temporal | Fase 1 `EN CURSO`; incremento de expiracion completado |
| 2026-09-21 | Fase 1 | Se protege el listado, se obtiene el ambiente desde el token y se elimina el fallback global de SurrealDB | Fase 1 `EN CURSO`; aislamiento de lectura completado |
| 2026-09-21 | Fase 1 | La creacion usa el ambiente autenticado y valida la jerarquia con aplicacion y funcionalidad | Fase 1 `EN CURSO`; aislamiento de escritura completado |
| 2026-09-21 | Fase 1 | Se comparan contextos mediante UUID, se mueve la composicion Spring a infraestructura y se completa la cobertura de la regla | Fase 1 `EN CURSO`; aislamiento de escritura auditado |
| 2026-09-21 | Fase 1 | Se eliminan tokens, material criptografico, consultas de token y cuerpos HTTP de los logs | Fase 1 `EN CURSO`; saneamiento de logs completado |
| 2026-09-21 | Fase 1 | Se tipan los rechazos de autenticacion como `401` y los de contexto como `403` | Fase 1 `COMPLETADA`; todos los criterios de salida verificados |
| 2026-09-22 | Fase 2 | Se crea la identidad externa y su puerto, se reemplaza el token fijo por resolucion simulada explicita y se admite `Authorization: Bearer` sin retirar el header legado | Fase 2 `COMPLETADA`; identidad sin contexto verificada |
| 2026-09-22 | Fase 3 | Se crea `OrganizationEntity` siguiendo la jerarquia `Entity<UUID>` y su puerto de persistencia en `core`, dejando SurrealDB y autorizacion para incrementos posteriores | Fase 3 `PARCIAL`; contrato de Organizacion completado |
| 2026-09-22 | Fase 3 | Se implementan el esquema, modelo, mapper y adaptador SurrealDB de Organizacion con pruebas unitarias y verificacion completa del reactor | Fase 3 `PARCIAL`; persistencia de Organizacion completada |
| 2026-09-22 | Fase 3 | Se implementan DTO, facade, caso de uso, reglas de nombre y unicidad, composicion Spring y catalogo para registrar organizaciones; el indice de nombre se refuerza como unico | Fase 3 `PARCIAL`; registro de Organizacion completado |
| 2026-09-22 | Fase 3 | Se asocia obligatoriamente cada nueva Aplicacion con una Organizacion validada, persistida y proyectada, preservando referencias parciales internas y documentando la migracion de datos existentes | Fase 3 `PARCIAL`; jerarquia Organizacion-Aplicacion completada |
| 2026-09-22 | Gobierno arquitectonico | Se establece como ley innegociable replicar la arquitectura y convenciones existentes antes de introducir cualquier clase o patron nuevo | Regla permanente y transversal a todas las fases |
| 2026-09-22 | Fase 3 | Se implementa la entidad `ExternalIdentityEntity`, el puerto `ExternalIdentityRepository`, el esquema SurrealDB con indice unico compuesto `(issuer, subject)`, modelo, mapper, adaptador y pruebas unitarias completas de la persistencia de identidades externas | Fase 3 `PARCIAL`; persistencia de identidades externas completada |
| 2026-09-22 | Fase 3 | Se modelan membresias, roles, permisos y asignaciones por Organizacion; se implementan el puerto y adaptador de consulta, la politica `401`/`403` y el filtrado de catalogos con pruebas de aislamiento y verificacion completa del reactor | Fase 3 `COMPLETADA`; autorizacion de negocio verificada |
| 2026-09-22 | Fase 4 | Se implementan el modelo y los puertos de contexto activo, su persistencia unica por identidad en SurrealDB y una cache Redis de mejor esfuerzo con TTL y claves SHA-256 derivadas de `issuer + subject` | Fase 4 `PARCIAL`; primera mitad completada y verificada |
| 2026-09-22 | Fase 4 | Se implementan los endpoints de contextos disponibles, consulta y seleccion, con validacion de identidad, autorizacion `CONTEXT_SELECT`, jerarquia, recuperacion cache-aside, OpenAPI y pruebas completas | Fase 4 `COMPLETADA`; criterio de salida verificado |
| 2026-09-22 | Fase 5 | Se migran la consulta, el listado y la traduccion de mensajes al contexto autorizado con `MessageAccessContext` y `MessageEnvironmentResolver`, convivencia de interceptores por orden, OpenAPI dual y pruebas con auditoria y Quality Gate verificados | Fase 5 `EN CURSO`; consulta, listado y traduccion migrados |
| 2026-09-22 | Fase 5 | Se migran la creacion de mensajes (`MESSAGE_CREATE`), la consulta de catalogos (match con `ActiveContext.applicationId`) y la administracion de aplicaciones, ambientes y funcionalidades (`APPLICATION_CREATE`, `ENVIRONMENT_CREATE`, `FUNCTIONALITY_CREATE`), con pruebas de aislamiento delegadas, auditoría `unit-test-validator` y Quality Gate verificado | Fase 5 `COMPLETADA`; criterio de salida de la fase cumplido |
| 2026-09-26 | Gobierno arquitectonico | Se unifican las validaciones de Aplicacion, Organizacion, autorizacion, contexto y mensajes con `Specification`, `RuleValidator` y `CompositeValidator`; se eliminan interfaces e implementaciones legacy, se preservan las categorias HTTP y se documenta el orden obligatorio de cortocircuito | Patron de validacion vigente, 1754 pruebas correctas y Quality Gate cumplido |
| 2026-09-26 | Gobierno arquitectonico | Se migra la validacion transversal de paginacion a composites explicitos, contextos tipados y reglas `RuleValidator` bajo `page/rule`; se eliminan wrappers y validadores imperativos legacy, preservando mensajes dinamicos y cortocircuito | Estructura de paginacion alineada con el patron vigente y pruebas focalizadas correctas |
