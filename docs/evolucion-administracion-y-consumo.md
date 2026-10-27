# Evolución de MessageUcoLab: administración primero, consumo después

Fecha: 21 de octubre de 2026.

## 1. Propósito y estado

Este documento reúne las decisiones, propuestas, riesgos y tareas de la conversación sobre la evolución de MessageUcoLab y la transcripción de la reunión con el profesor del 14 de octubre de 2026.

**Prioridad: dejar completa la sección administrativa.** La autenticación de aplicaciones consumidoras se diseña y ejecuta en una fase posterior; no bloquea el trabajo administrativo que pueda completarse con el contrato de seguridad disponible.

Este es un diseño y un checklist de trabajo, no evidencia de implementación. Crear este documento no implica cambiar backend, frontend, base de datos, seguridad o despliegues. Las rutas, campos y nombres nuevos incluidos como propuestas no son contratos publicados.

El orden de ejecución está en la [sección 12](#12-todo-ordenado-de-implementación). Los pendientes de seguridad que afecten al administrador sí deben resolverse antes de habilitar las operaciones correspondientes.

## 2. Decisiones y límites de alcance

| Tema | Decisión o estado |
|---|---|
| Prioridad actual | Obtener información confiable de seguridad y completar el frontend administrativo con su backend. |
| Organizaciones | Conservar `Organization` como representación local del tenant. |
| Aplicaciones | Conservar `Application`, vinculada a su organización y con configuración propia de mensajes. |
| Correspondencia externa | Cada aplicación del listado de seguridad representa una aplicación consumidora cuyos mensajes se pueden administrar. |
| Identificadores externos | Seguridad entrega nombres, no IDs, en la información compartida. No diseñar suponiendo que esos IDs existen. |
| Identificadores internos | Conservar o generar UUID propios para organizaciones y aplicaciones en MessageUcoLab. |
| Usuarios | No crear un directorio ni una tabla local de usuarios para este flujo. |
| Roles y permisos | Seguridad es su fuente de autoridad; no replicar su administración en MessageUcoLab. |
| Habilitación local | Preferencia de diseño: configuración explícita por una persona autorizada, no creación automática al consultar o iniciar sesión. |
| Aplicación seleccionada | Selección explícita en navegación y solicitudes; no depender de una fila `Active_Context` por usuario. |
| Ambientes | Selección múltiple de uno, dos o tres ambientes al crear el mensaje, según permisos. |
| Contenido | Un mensaje y su contenido actual compartido; disponibilidad independiente por ambiente. |
| Versiones | Fuera del alcance inicial. Posible evolución futura. |
| Consumo externo | Fase final, separada de la administración. Dirección preferida: seguridad reconoce identidades técnicas, pendiente de confirmar soporte. |
| Credenciales propias | No emitir API keys ni recuperar el token propietario como consecuencia automática de este diseño. |

La preselección de Desarrollo, la navegación exacta, los nombres de campos y la política de operaciones masivas son propuestas por cerrar, no decisiones ya implementadas.

## 3. Información que recibimos de seguridad

### 3.1. Forma conocida de los datos

Ejemplo anonimizado, conservando la estructura relevante de la respuesta compartida:

```json
{
  "code": "CURRENT_USER_APPLICATIONS_READ",
  "message": "Aplicaciones y roles del usuario consultados",
  "data": {
    "tenant": {
      "name": "Universidad de ejemplo"
    },
    "user": {
      "name": "Persona administradora",
      "email": "persona@example.org"
    },
    "applications": [
      {
        "name": "Carga masiva",
        "tenantRoles": [],
        "applicationRoles": [{ "name": "ADMIN" }],
        "profiles": []
      },
      {
        "name": "Biblioteca",
        "tenantRoles": [],
        "applicationRoles": [{ "name": "ROL DE EJEMPLO" }],
        "profiles": [
          { "name": "Perfil de ejemplo", "roles": [] }
        ]
      }
    ]
  },
  "timestamp": "2026-10-21T00:00:00Z",
  "requestId": "identificador-de-peticion",
  "correlationId": "identificador-de-correlacion"
}
```

- El tenant representa una organización local.
- Las aplicaciones alimentan la selección visual del administrador.
- El nombre y correo permiten mostrar quién inició sesión; no requieren persistir un usuario.
- Los nombres de roles y perfiles no permiten inferir por sí solos los permisos efectivos de cada operación.
- Un rol llamado `ADMIN` no concede automáticamente todas las operaciones de MessageUcoLab. El administrador de seguridad y el administrador de mensajes no son necesariamente equivalentes.
- La lista permite descubrir aplicaciones accesibles, pero hay que confirmar qué acceso garantiza exactamente y cómo se autorizan los recursos y datos de MessageUcoLab.

### 3.2. Contrato todavía pendiente

No está confirmado si esta información llegará en claims o mediante un endpoint. Antes de implementar hay que definir:

1. Fuente real, ruta o nombres de claims y estructura efectiva.
2. Cómo el backend obtiene información autenticada del usuario actual, sin confiar en un JSON remitido por el navegador.
3. Si son claims, validación mediante la integración confiable existente; decodificar un token no es validarlo.
4. Si es un endpoint, cómo consulta el backend en nombre del usuario mediante el mecanismo soportado, sin extraer ni exponer tokens internos.
5. Qué ocurre si cambia el tenant, desaparece una aplicación, se revoca acceso o vence la sesión.
6. Si hay un solo tenant activo o un contrato multitenant más amplio. No inventar soporte para varios tenants en una respuesta que solo contiene uno.
7. Permisos para descubrir aplicaciones, habilitarlas, gestionar mensajes, traducir y modificar disponibilidad por ambiente.
8. Tratamiento de errores, indisponibilidad y frescura de los datos de autorización.

El login BFF y las cookies actuales se conservan. La lectura del frontend sirve para la experiencia visual; la decisión de acceso corresponde al backend y al enforcement de seguridad.

## 4. Relación con la base de datos sin IDs externos

### 4.1. Modelo lógico propuesto

Los nombres siguientes son conceptuales. Hay que adaptarlos al esquema real de SurrealDB, no aplicar SQL relacional ni crear una segunda base de datos.

**Organization**

| Campo | Uso |
|---|---|
| `id` | UUID interno estable. |
| `security_tenant_name` | Nombre externo usado para vincular el tenant. |
| `name` | Nombre visible local; puede reutilizarse si no se necesita una etiqueta diferente. |

**Application**

| Campo | Uso |
|---|---|
| `id` | UUID interno estable. |
| `organization_id` | Referencia a la organización local. |
| `security_application_name` | Nombre externo usado para vincular la aplicación. |
| `name` | Nombre visible local. |
| Idioma base | Configuración propia del catálogo de mensajes de esa aplicación consumidora. |
| Estado/configuración | Conservar y adaptar las capacidades actuales necesarias. |

Vínculos conceptuales únicos:

```text
security_tenant_name → Organization.id

Organization.id + security_application_name → Application.id
```

Las relaciones de mensajes, módulos, funcionalidades y ambientes utilizan los IDs internos, nunca el nombre externo como referencia de negocio.

### 4.2. Condiciones necesarias y límites

Los nombres se convierten en claves de integración, aunque sean nombres visibles:

- El nombre de tenant debe identificar inequívocamente a una organización en el alcance de esta integración.
- El nombre de aplicación debe ser único dentro del tenant correspondiente.
- Una aplicación con el mismo nombre en otro tenant es otra aplicación.
- Dos tenants homónimos no se pueden distinguir con este contrato.
- Dos aplicaciones homónimas dentro del mismo tenant tampoco se pueden distinguir.
- No aplicar automáticamente minúsculas, eliminación de acentos o normalización de espacios sin un acuerdo: podría unir identidades distintas.
- Un UUID local o un hash del nombre no añade información que permita resolver esas ambigüedades.

**Si seguridad no garantiza unicidad y una política de renombrado, no existe una vinculación automática plenamente segura con esta respuesta.** Se debe reportar el bloqueo de la vinculación afectada, no adivinar coincidencias.

Si `Carga masiva` pasa a llamarse `Gestión de archivos`, el backend no puede deducir si hubo renombrado, una aplicación nueva o reutilización de un nombre. No fusionar registros, trasladar datos ni crear reemplazos automáticamente. Definir una revinculación autorizada, verificada y auditada, considerando sesiones o claims antiguos y el posible uso posterior del nombre anterior.

Perder acceso al listado no implica borrar la aplicación local ni sus mensajes. Primero revisar autorización y vinculación.

### 4.3. Habilitación explícita, compartida por todos los usuarios

```text
Persona inicia sesión
        ↓
Backend obtiene aplicaciones autorizadas de seguridad
        ↓
Cruza con vínculos locales sin escribir datos
        ↓
Aplicación configurada → Gestionar mensajes
Aplicación sin configurar → Configurar, si tiene permiso
                         → Pendiente de configuración, en otro caso
```

Al confirmar la configuración:

1. Volver a validar la identidad, el acceso a la aplicación y el permiso de habilitación.
2. Resolver inequívocamente el tenant y la aplicación de seguridad; no aceptar nombres arbitrarios del cliente como prueba de acceso.
3. Reutilizar la organización local o crearla si falta.
4. Reutilizar la aplicación si ya está habilitada; de lo contrario, crearla con idioma base y configuración requerida.
5. Aprovisionar o reutilizar los ambientes mediante el comportamiento existente, evitando duplicados.
6. Hacer la operación consistente y segura ante concurrencia y reintentos; una segunda solicitud no debe sobrescribir configuración existente.

La organización y la aplicación no se crean por usuario. Una segunda persona autorizada accede a la misma configuración y catálogo.

Para aplicaciones todavía no habilitadas se debe diseñar cómo identificar la selección al configurar: referencia externa validada o identificador opaco de descubrimiento. No asignar falsos UUID locales ni tratar la selección del navegador como autorización.

## 5. Usuarios, autorización, contexto y auditoría

### 5.1. Qué conservar y qué retirar

| Elemento actual | Dirección del diseño |
|---|---|
| `Organization` | Conservar como dato de negocio vinculado al tenant. |
| `Application` | Conservar como configuración del catálogo de una aplicación consumidora. |
| `Environment` | Conservar para controlar disponibilidad de mensajes. |
| `Active_Context` | Retirar la dependencia persistida por usuario después de sustituir todos sus usos. |
| `Membership`, `Role_assignment`, `Role`, `Permission`, `Role_Permission` | Dejar de usarlos como autoridad local si seguridad cubre sus responsabilidades. Inventariar dependencias antes de retirar código o tablas. |
| `External_Identity` | Evaluar su necesidad real: puede dejar de persistirse, pero aún puede requerirse una identidad autenticada transitoria para autorización y auditoría. |

No crear una tabla `User` para reconstruir la pertenencia usuario–organización–aplicación que ya proporciona seguridad. Esto no elimina la necesidad de identificar de forma confiable al actor de cada solicitud.

No borrar tablas, registros, volúmenes o cachés como primer paso. La retirada física requiere inventario, respaldo, migración, autorización delimitada y un camino de recuperación.

### 5.2. Selección explícita de aplicación

Propuesta de navegación:

```text
/applications/{applicationId}/messages
```

La URL conserva la aplicación local seleccionada al recargar y permite que dos pestañas trabajen en aplicaciones distintas. No exige una tabla `Active_Context`.

Cada operación vuelve a validar que la aplicación local corresponde a un tenant y una aplicación autorizados para esa identidad. Cambiar el UUID en la URL no concede acceso.

El ambiente deja de ser el contexto global obligatorio para administrar una aplicación. Puede ser un filtro de listado o una selección de publicación, nunca una credencial.

### 5.3. Auditoría sin directorio local de usuarios

Evaluar `created_at`, `created_by`, `updated_at` y `updated_by`, así como auditoría de habilitación, revinculación y cambios de disponibilidad.

Preferir un identificador estable del principal autenticado si la integración lo proporciona; no suponer que existe en el JSON compartido. Si solo se dispone de nombre y correo, conservarlos como instantáneas históricas cuando corresponda, no como identidad inmutable, clave de acceso o relación de pertenencia. Definir privacidad, acceso y retención de estos datos.

## 6. Experiencia del frontend administrativo

### 6.1. Entrada y selector

```text
Universidad de ejemplo                         Persona administradora

¿En qué aplicación quieres gestionar mensajes?

[ Carga masiva ]                     [ Biblioteca ]
[ Gestionar mensajes ]              [ Configurar MessageUcoLab ]
```

- Una aplicación configurada y accesible: se puede entrar directamente.
- Una aplicación sin configurar: mostrar la configuración si tiene permiso; no saltarla por selección automática.
- Varias aplicaciones: mostrar tarjetas o lista accesible.
- Ninguna aplicación: mostrar un estado vacío explicativo, no un menú silenciosamente desaparecido.
- Error de carga: distinguirlo de una lista vacía y permitir reintento de lectura.
- Cabecera persistente con organización y aplicación; acción para cambiar de aplicación.

### 6.2. Navegación propuesta

```text
Universidad de ejemplo / Carga masiva ▾

Mensajes | Crear mensaje | Consultar | Traducir | Configuración
```

La sección Integración se deja para la fase de consumidores. No mostrar botones funcionales de generación de credenciales mientras esa capacidad no esté acordada.

Al cambiar de aplicación: limpiar datos, catálogos dependientes, selección, paginación y suscripciones anteriores; cancelar o ignorar respuestas en vuelo de la aplicación previa. Revisar formularios con cambios sin guardar.

### 6.3. Sesión y permisos

- Conservar `credentials: 'include'`, protección de sesión y recuperación manual de `401` existente.
- Un `403` significa falta de permiso; no debe disparar reautenticación.
- Una caída de red no equivale a sesión vencida.
- Nunca reenviar automáticamente una mutación al recuperar sesión.
- Ocultar o deshabilitar botones mejora la interfaz, pero no reemplaza autorización en backend.
- Mantener compatibilidad de cookies, CORS, gateway y protección CSRF según el contrato existente.
- No almacenar secretos técnicos en el navegador ni utilizar `localStorage` como autoridad de acceso.

## 7. Mensajes y ambientes: primera versión sin versionado

### 7.1. Modelo funcional acordado

```text
Organization
  └── Application
        ├── Idioma base
        ├── Ambientes
        ├── Módulos / funcionalidades
        └── Mensajes
              ├── Traducciones
              └── Asociaciones de disponibilidad por ambiente
```

Hay un solo mensaje con contenido actual compartido. Las asociaciones a ambientes controlan dónde está disponible; no crean copias independientes.

| Mensaje | Desarrollo | Pruebas | Producción |
|---|---|---|---|
| `MSG_EJEMPLO` | Activo | Activo | Inactivo |

En la creación se seleccionan uno, dos o tres ambientes mediante casillas o un desplegable multiselección. Reutilizar los ambientes existentes de la aplicación; verificar los nombres técnicos actuales `Develop`, `Testing` y `Production` y sus etiquetas visuales.

Se puede proponer Desarrollo preseleccionado, pero la elección inicial y los permisos por ambiente deben cerrarse antes de implementarlo. Crear un mensaje no implica poder habilitarlo en Producción.

Validaciones necesarias:

- Aplicación autorizada y configurada.
- Al menos un ambiente, si se mantiene el alcance acordado sin borradores no publicados.
- Sin IDs repetidos y con todos los ambientes pertenecientes a esa aplicación.
- Permiso para la operación y para todos los ambientes afectados.
- Funcionalidad, idioma base y datos de catálogo coherentes con la aplicación.
- Unicidad de código dentro de la aplicación, conservando las reglas actuales que correspondan.

Propuesta inicial: validar todo antes de escribir y crear mensaje más asociaciones de forma atómica; un ambiente inválido no debe dejar una publicación parcial. Cerrar también la semántica de las operaciones masivas antes de implementarlas.

### 7.2. Consecuencias de compartir contenido

**Editar un mensaje afecta a todos los ambientes donde está habilitado, incluida Producción.** No existe una versión de Pruebas independiente en esta primera entrega.

El frontend debe advertir el impacto real antes de guardar. Definir y aplicar en backend quién puede editar contenido compartido cuando el mensaje ya está disponible en Producción; no basta con proteger el botón de publicar.

Las traducciones corresponden al contenido compartido. Definir qué ocurre cuando cambia el original: invalidación, estado pendiente de revisión o regeneración autorizada, evitando servir como vigente una traducción obsoleta.

También deben revisarse cachés, proyecciones y SSE para que una actualización llegue a todos los ámbitos afectados sin mezclar aplicaciones. No borrar Redis ni cambiar formatos persistidos como atajo.

### 7.3. Gestión posterior

- Habilitar o deshabilitar un mensaje por ambiente sin duplicarlo.
- Mantener distinguibles el estado global del mensaje y su disponibilidad en cada ambiente.
- Mostrar ambientes habilitados en listado y detalle; poder filtrar sin cambiar un contexto de autorización persistido.
- Incorporar habilitación/deshabilitación masiva dentro del cierre administrativo, con permisos y resultados claros.
- Deshabilitar una asociación no elimina el mensaje ni sus otras asociaciones.

Evolución futura: versiones de un mismo mensaje y una versión publicada por ambiente, conservando su identidad. No implementar ahora historial de publicación, snapshots ni contenidos independientes por ambiente.

## 8. Contrato administrativo y arquitectura

Diseñar primero las operaciones y después su transporte HTTP. El contrato debe cubrir:

| Operación | Efecto esperado |
|---|---|
| Consultar información administrativa actual | Datos confiables de persona/tenant y aplicaciones; sin escrituras. |
| Descubrir aplicaciones | Nombre externo, vínculo local si existe, estado de configuración y capacidades de interfaz derivadas del contrato autorizado. |
| Habilitar aplicación | Configuración explícita, protegida y segura ante reintentos. |
| Consultar configuración y ambientes | Lectura limitada a la aplicación autorizada. |
| Gestionar mensajes | Aplicación explícita, sin deducirla de un ambiente global persistido. |
| Cambiar disponibilidad | Operación individual y masiva, validando todos los ambientes afectados. |
| Gestionar traducciones y catálogos dependientes | Mismo aislamiento por aplicación y política de contenido compartido. |
| Suscribirse a eventos | Alcance autorizado por aplicación y, si corresponde, filtro de ambiente. |

Como ejemplo de diseño, no endpoint existente ni ruta definitiva:

```http
POST /messageucolab/v1/applications/{applicationId}/messages
```

El body podría incorporar `environmentIds` junto con los datos actuales del mensaje. Definir compatibilidad, validaciones, envelope, estados HTTP y errores antes de cambiar contratos. No exigir un UUID local inexistente para habilitar una aplicación descubierta.

Un `401` corresponde a autenticación inválida; un `403`, a permisos insuficientes. Acordar cuándo ocultar recursos ajenos mediante `404` sin filtrar información. No asumir que los errores del PEP tienen el mismo cuerpo que los errores del dominio.

Arquitectura obligatoria:

- `infrastructure → core → utils`, nunca al revés.
- Puerto de acceso a la información externa y modelo interno estable, independiente de si la fuente final son claims o un endpoint.
- Adaptador concreto y traducción HTTP en `infrastructure`.
- Reglas de negocio en `core`, sin añadir dependencias de frameworks; validación Composite según patrones del proyecto.
- Textos, errores y plantillas nuevas en `CatalogPort`, códigos y seed Redis; no introducir fallbacks de texto Java hardcodeados.
- No ampliar esta tarea a refactorizaciones ajenas al flujo administrativo.

## 9. Estado del código relevante para planificar

Estas son referencias del estado inspeccionado al redactar este documento, no una auditoría completa ni una verificación ejecutada:

### Backend

- `core/src/main/java/co/edu/uco/application/usecase/CreateMessageUseCase.java`: obtiene el ambiente mediante el contexto y desde él deduce la aplicación; actualmente crea con un ambiente autenticado.
- `core/src/main/java/co/edu/uco/application/primaryports/dto/message/CreateMessageDTO.java`: no contiene lista de ambientes.
- `core/src/main/java/co/edu/uco/application/usecase/domain/security/MessageAccessContext.java` y resolutores de ambiente: revisar al sustituir el contexto persistido.
- `ActiveContextUseCase`, interceptores, repositorios y caché de contexto: inventariar consumidores antes de retirarlos.
- Ya existe el concepto `message_environment`; adaptar sus capacidades antes de introducir una estructura paralela.
- `infrastructure/src/main/resources/application.properties`: enforcement PEP habilitado. No desactivarlo ni declarar públicas las rutas administrativas.

### Frontend MessageUcoLab

Repositorio: `../MessageUcolab--Frontend`.

- `src/store.js`: consulta y selecciona aplicaciones de la sesión o contextos y persiste la selección en `localStorage`; la carga del destino fijo fue retirada.
- `src/App.vue`: condiciona el menú a `activeContext`; la conexión y el filtrado SSE dependen del ambiente activo.
- `src/components/SessionApplicationSelect.vue`: selector predeterminado de aplicaciones; el selector de contexto anterior se retiró.
- `src/components/CreateMessage.vue`: adaptar a múltiples ambientes y aplicación explícita.
- `src/components/ListMessages.vue`, `ViewMessage.vue`, `TranslateMessage.vue` y `CreateCatalog.vue`: revisar contratos y datos dependientes de la aplicación.
- `src/api.js`: conserva sesión BFF con cookies y define los contratos actuales de contextos, catálogos y mensajes.
- `src/messageEventService.js` y `src/security/session.js`: preservar aislamiento de eventos y recuperación de sesión.

Antes de modificar ese repositorio, volver a consultar sus instrucciones aplicables. No se encontró `AGENTS.md` en su raíz durante esta lectura; eso no sustituye la revisión de instrucciones de directorios afectados al implementar.

El mecanismo temporal de destino fijo fue retirado del backend, frontend y despliegue de MessageUcoLab; el acceso administrativo actual usa la aplicación validada contra la sesión, sin fallback de permisos.

## 10. Aportes del profesor y cómo se incorporan

| Idea de la transcripción | Aplicación en este diseño |
|---|---|
| Un mensaje, varios ambientes | Adoptado mediante disponibilidad por ambiente y contenido compartido. |
| Encender/apagar por ambiente | Adoptado; no hace falta duplicar o importar mensajes entre ambientes. |
| Habilitación masiva | Incluida en el checklist administrativo. |
| Empezar en Desarrollo y avanzar | Compatible como política de interfaz, pero se mantiene la selección múltiple solicitada y se deben acordar permisos. |
| Autogestión por el responsable de la aplicación | Habilitación local y administración según permisos externos; no implica poder autoconcederse acceso. |
| Credencial ligada a aplicación y ambiente | Requisito para la fase final de integración técnica. |
| Vigencia, revocación y varias credenciales | Requisitos de rotación e integración por acordar con seguridad. |
| Las identidades no son solo personas | Separar autenticación humana administrativa de autenticación de aplicaciones. |
| Salt y hash | Técnicas de verificación/protección de secretos, no un protocolo completo de autenticación. |

La transcripción contiene fragmentos imprecisos. No tomar expresiones como «descifrar el hash» literalmente: un hash no se descifra. El salt no necesita ser secreto y no sustituye la generación segura de credenciales, TLS, autorización, revocación ni limitación de uso.

## 11. Consumo por otras aplicaciones: fase final

### 11.1. Dirección preferida, pendiente de capacidad externa

Seguridad reconoce una identidad técnica que no depende del login de una persona:

```text
Backend consumidor
  → credencial técnica admitida por seguridad
  → identidad y permisos comprobados por seguridad / PEP
  → alcance autorizado en MessageUcoLab
  → mensajes habilitados para aplicación y ambiente
```

Si seguridad soporta OAuth 2.0 Client Credentials, el consumidor obtiene automáticamente un token temporal con su credencial y lo utiliza en las consultas. Si soporta validación directa de API keys, se integra según ese contrato. Ninguna modalidad está confirmada por la información disponible.

Obtener una credencial mediante una persona autorizada no autentica futuras peticiones por sí solo. En cada consulta se debe comprobar la credencial, su alcance y su vigencia; los tokens deben seguir una política definida de expiración y revocación.

El secreto de registro de MessageUcoLab ante el PEP no es una credencial para consumidores y no se comparte con ellos.

### 11.2. Qué queda por resolver después del administrador

- Registro de identidades técnicas y permisos por aplicación y ambiente.
- Quién emite, almacena, verifica, rota y revoca los secretos.
- Si la administración se hace en seguridad o mediante una API autorizada integrable en el admin de MessageUcoLab.
- Cómo recibe MessageUcoLab una identidad/alcance confiable; un permiso genérico de ruta no basta para proteger datos de otras aplicaciones.
- Vigencia, revocación efectiva, solapamiento de credenciales durante rotación, auditoría y límites de uso.
- Custodia de secretos en backend/BFF del consumidor y un gestor de secretos; nunca en JavaScript público, URLs o logs.
- Compatibilidad del contrato de consulta existente con el nuevo alcance, sin romper consumidores inadvertidamente.

La alternativa discutida de una API key propia con identificador público, secreto aleatorio y verificador con salt/hash queda como alternativa conceptual, **no como implementación aprobada**. Si seguridad es la autoridad elegida, no crear automáticamente `ConsumerCredential` ni un segundo emisor local.

Si seguridad no soporta identidades técnicas, reportar una dependencia externa. No declarar públicas rutas de consumo, desactivar el PEP ni reintroducir emisión RSA, el header `Token` o el mecanismo propietario retirado.

### 11.3. Lo que no corresponde al Building Block

El consumidor decide sus pantallas y cómo mostrar mensajes. El idioma base local corresponde a esa aplicación consumidora; no describe la navegación del admin. La evolución de consulta/traducción puede operar por aplicación, módulo, funcionalidad o conjunto de códigos sin conocer las pantallas del consumidor.

Los mensajes usan el contenido compartido actual y la disponibilidad por ambiente acordados. Consultas por conjuntos, selección de traducciones, caché del consumidor y SDKs se diseñan posteriormente sin atribuirles soporte actual.

## 12. ToDo ordenado de implementación

Todas las casillas siguientes representan trabajo pendiente. No indican autorización para ejecutar ahora migraciones, cambios externos o pruebas.

### Paso 1. Obtener y cerrar el contrato de información administrativa

- [ ] Confirmar con seguridad si los datos llegan en claims o por endpoint, sin exigir IDs que no entregará.
- [ ] Obtener la ruta o claims reales, estructura, autenticación y semántica de la respuesta.
- [ ] Confirmar cómo el backend accede a la información del usuario actual usando la integración soportada.
- [ ] Acordar unicidad, comparación, renombrado y posible reutilización de nombres de tenant/aplicación.
- [ ] Definir comportamiento para datos incompletos, duplicados, lista vacía, cambio de tenant, revocación e indisponibilidad.
- [ ] Confirmar permisos de descubrimiento, habilitación, lectura, creación, edición, traducción y publicación por ambiente.
- [ ] Documentar bloqueos externos sin modificar seguridad ni inventar permisos desde nombres de roles.

**Salida:** contrato confiable y ejemplos anonimizados; ambigüedades resueltas o bloqueos delimitados.

### Paso 2. Inventariar datos actuales y definir la migración

- [ ] Revisar esquema SurrealDB, referencias, seeds y operaciones existentes para organizaciones, aplicaciones y ambientes.
- [ ] Diseñar los campos de vinculación por nombres y sus restricciones de unicidad.
- [ ] Inventariar usos de identidad, membresías, roles, permisos y contexto activo en todas las operaciones.
- [ ] Identificar organizaciones/aplicaciones existentes que deben vincularse mediante una decisión verificada; no fusionarlas por parecido de nombre.
- [ ] Definir respaldo, migración idempotente, validación de datos y recuperación, sin borrar datos como atajo.
- [ ] Conservar UUID, mensajes, traducciones y asociaciones de ambiente existentes.
- [ ] Definir política de auditoría y su identidad disponible sin crear un directorio local de usuarios.

**Salida:** modelo y migración revisados; ninguna tabla eliminada prematuramente.

### Paso 3. Implementar la lectura confiable de seguridad en el backend

- [ ] Crear o adaptar el puerto y modelo interno de información administrativa en `core`.
- [ ] Implementar el adaptador en `infrastructure` para la fuente confirmada.
- [ ] Mantener autenticación, enforcement PEP y aislamiento por tenant/aplicación.
- [ ] Resolver vínculos locales y estados configurada/pendiente sin escrituras durante consultas o login.
- [ ] Diseñar una política de frescura que no convierta datos obsoletos del navegador en autorización.
- [ ] Registrar textos y errores nuevos en el catálogo Redis siguiendo el patrón existente.

**Salida:** el backend puede informar qué aplicaciones puede descubrir el usuario y cuáles están configuradas.

### Paso 4. Cerrar e implementar el contrato de habilitación y selección

- [ ] Documentar request/response, errores y permisos de descubrimiento, habilitación y configuración.
- [ ] Definir cómo se referencia una aplicación todavía sin UUID local y cómo se revalida en backend.
- [ ] Implementar habilitación explícita: organización, aplicación, idioma base y ambientes necesarios.
- [ ] Proteger contra duplicados por concurrencia y reintentos, sin sobrescribir configuración existente.
- [ ] Usar aplicación local explícita para las operaciones; comprobar siempre su vínculo autorizado.
- [ ] Definir transición de las rutas actuales y compatibilidad; no retirar todavía dependencias sin reemplazo.

**Salida:** habilitar una aplicación una sola vez y reutilizarla entre usuarios autorizados.

### Paso 5. Cambiar la entrada y navegación del frontend administrativo

- [ ] Revisar instrucciones del repositorio frontend y actualizar `src/api.js` al contrato acordado.
- [ ] Sustituir selección de contexto por listado/tarjetas de aplicaciones y su estado de configuración.
- [ ] Implementar estados de carga, vacío, error, acceso denegado y pendiente de configuración.
- [ ] Añadir formulario de habilitación con idioma base para quien tenga permiso.
- [ ] Mostrar organización/aplicación en cabecera y permitir cambiar de aplicación.
- [ ] Llevar el UUID local a una navegación explícita, conservable al recargar y aislada por pestaña.
- [ ] Eliminar la dependencia visual del antiguo `activeContext` cuando el reemplazo esté completo.
- [ ] Limpiar estados y controlar respuestas en vuelo al cambiar de aplicación.
- [ ] Preservar recuperación manual de sesión, tratamiento de `403`, accesibilidad y cambios sin guardar.

**Salida:** una persona puede entrar, elegir o configurar su aplicación y navegar sin contexto persistido por usuario.

### Paso 6. Adaptar mensajes y disponibilidad múltiple en el backend

- [ ] Pasar de «ambiente del contexto → aplicación» a «aplicación autorizada → ambientes elegidos».
- [ ] Adaptar DTO, validadores Composite, caso de uso y puertos de repositorio.
- [ ] Reutilizar `message_environment`; crear un mensaje con sus asociaciones, no varias copias.
- [ ] Validar ambientes, pertenencia, duplicados y permisos antes de escribir.
- [ ] Acordar e implementar atomicidad de creación y asociaciones.
- [ ] Implementar consulta y cambio de disponibilidad individual por ambiente.
- [ ] Definir restricciones de edición de contenido compartido habilitado en Producción.
- [ ] Adaptar actualización, consulta por código, traducción y reglas de unicidad al alcance explícito.
- [ ] Mantener coherencia del estado global y los estados por ambiente.

**Salida:** un mensaje puede estar habilitado en uno, dos o tres ambientes con contenido compartido.

### Paso 7. Completar formularios, listado y detalle del admin

- [ ] Mostrar la aplicación elegida sin pedir al usuario repetirla en cada formulario.
- [ ] Cargar funcionalidades y catálogos de esa aplicación.
- [ ] Añadir casillas o desplegable multiselección de ambientes en creación.
- [ ] Mostrar únicamente acciones permitidas y explicar ambientes no autorizados.
- [ ] Incorporar disponibilidad por ambiente en listado y detalle, además de filtros.
- [ ] Permitir habilitar/deshabilitar por ambiente sin borrar el mensaje.
- [ ] Advertir al editar contenido o traducciones compartidos con Producción.
- [ ] Ajustar creación/configuración de aplicaciones: habilitar desde seguridad, no crear identidades externas libres.
- [ ] Coordinar módulos, funcionalidades, parámetros y catálogos existentes según el alcance actual, sin crear features ajenas.

**Salida:** la gestión individual de mensajes está completa y es coherente con el nuevo modelo.

### Paso 8. Completar operaciones masivas, traducciones y sincronización

- [ ] Definir e implementar habilitación/deshabilitación masiva por ambiente con permisos y resultado inequívoco.
- [ ] Evitar mezclar mensajes de otras aplicaciones en una selección masiva.
- [ ] Cerrar tratamiento de traducciones cuando cambia el contenido original.
- [ ] Actualizar proyecciones y claves/invalidez de caché para todos los ambientes afectados, sin vaciar Redis.
- [ ] Adaptar SSE de backend y frontend: aplicación autorizada, ambientes afectados y cierre al cambiar de aplicación.
- [ ] Evitar que lecturas, eventos o datos en memoria de una aplicación aparezcan en otra.

**Salida:** el admin no queda inconsistente al cambiar disponibilidad, editar o trabajar con varias pestañas.

### Paso 9. Retirar dependencias antiguas de forma controlada

- [ ] Confirmar que ninguna operación sigue dependiendo del contexto activo persistido ni de autorización local reemplazada.
- [ ] Retirar endpoints, interceptores, puertos, repositorios, cachés y UI de contexto que hayan quedado sin uso.
- [ ] Planificar y realizar solo la migración/limpieza expresamente autorizada de datos propios de MessageUcoLab.
- [ ] Evaluar por separado la persistencia de `External_Identity`; conservar lo necesario para identidad transitoria y auditoría.
- [x] Retirar el modo de destino fijo de desarrollo y su campo de ambiente del contexto de acceso, sin usarlo como fallback de permisos.
- [ ] No tocar registros o configuración de seguridad ni eliminar usuarios, roles o asignaciones externos.

**Salida:** no hay dos modelos de autorización compitiendo ni rutas antiguas que eludan el nuevo alcance.

### Paso 10. Verificar y cerrar la sección administrativa

- [ ] Solicitar autorización para invocar `unit-test-agent` antes de crear, modificar, ejecutar o auditar pruebas; no hacerlo automáticamente por cambios de producción.
- [ ] Tras autorizarse, delegar todo trabajo de pruebas a `unit-test-agent`, con GPT-6 Luna y `background: false`, incluidos sus comandos; no ejecutar pruebas en la conversación principal.
- [ ] Verificar contrato real, aislamiento tenant/aplicación/ambiente, habilitación concurrente y acceso por UUID ajeno.
- [ ] Cubrir creación multiambiente, rollback ante errores, disponibilidad individual/masiva, contenido compartido, traducciones, caché y SSE.
- [ ] Cubrir sesión vencida, `401`, `403`, red, recarga, cambio de aplicación y respuestas en vuelo.
- [ ] Auditar pruebas backend con `unit-test-validator` dentro del subagente y ejecutar Maven Wrapper `clean verify` en primer plano según `AGENTS.md`.
- [ ] Para pruebas frontend autorizadas, usar su framework y sus instrucciones mediante el mismo agente, sin tocar seguridad.
- [ ] Actualizar OpenAPI y documentación frontend/backend una vez implementados los contratos; no publicar documentación de propuestas como capacidades existentes.
- [ ] Revisar compatibilidad, arquitectura hexagonal, cobertura y Quality Gate; reportar evidencias reales y bloqueos.
- [ ] Coordinar despliegue de MessageUcoLab y su frontend sin operar seguridad ni ejecutar un `up` general.

**Salida:** administrador implementado, verificado con autorización y documentado. Solo entonces avanzar a integración de consumidores.

### Paso 11. Acordar autenticación de aplicaciones consumidoras

- [ ] Confirmar con seguridad soporte de identidades técnicas, Client Credentials o validación directa de API keys.
- [ ] Definir quién registra clientes, emite credenciales, verifica secretos, rota y revoca.
- [ ] Acordar la identidad y el alcance confiable que recibirá MessageUcoLab por aplicación y ambiente.
- [ ] Diseñar permisos de consumo sin usar sesión ni contexto activo de una persona.
- [ ] Acordar vigencia, revocación efectiva y coexistencia temporal de credenciales durante rotación.
- [ ] Determinar si el admin de MessageUcoLab puede integrar gestión autorizada o solo mostrar instrucciones de seguridad.
- [ ] Registrar dependencias externas si el componente no soporta estas capacidades; no resolverlas deshabilitando PEP o modificando seguridad.

**Salida:** contrato máquina a máquina confirmado; todavía no asumir que una API key propia es necesaria.

### Paso 12. Implementar y documentar el consumo externo

- [ ] Integrar el mecanismo confirmado sin recuperar credenciales propietarias retiradas.
- [ ] Adaptar resolución de aplicación y ambiente de consumo a la identidad técnica autorizada.
- [ ] Mantener la consulta limitada a mensajes activos y habilitados, con aislamiento de códigos por aplicación.
- [ ] Revisar el endpoint existente de resolución y actualizar su documentación sin romper contratos inadvertidamente.
- [ ] Añadir sección Integración al admin solo con capacidades realmente disponibles.
- [ ] Documentar integración desde backend/BFF, custodia de secretos, TLS, expiración, rotación, revocación, errores y límites de uso.
- [ ] Definir alcance inicial de idiomas, traducciones, lotes y caché del consumidor sin asumir soporte adicional.
- [ ] Obtener autorización de pruebas si corresponde y validar todo mediante el agente obligatorio en primer plano.

**Salida:** una aplicación consulta sus mensajes sin sesión humana y sin acceso a aplicaciones o ambientes ajenos.

## 13. Criterios de terminado del administrador

Antes de considerar lista esta prioridad deben cumplirse todos:

1. Información administrativa obtenida desde una fuente autenticada y contrato de nombres con límites claros.
2. Selección visual de aplicaciones y estados configurada/pendiente/vacío/error.
3. Habilitación explícita, única, autorizada y reutilizable entre personas.
4. Organización y aplicación locales con UUID propios, sin replicar un directorio de usuarios o permisos.
5. Navegación y solicitudes con aplicación explícita, sin dependencia del contexto activo persistido.
6. Creación de un mensaje en uno, dos o tres ambientes, con validaciones y consistencia.
7. Listado, consulta, edición, traducción y configuración aislados por aplicación.
8. Disponibilidad individual y masiva por ambiente; advertencias y autorización para cambios que afectan Producción.
9. Contenido compartido y consecuencias conocidas; sin versionado implícito.
10. Sesión, errores, SSE, caché y cambios entre aplicaciones coherentes y protegidos.
11. Migración sin pérdida de datos ni cambios sobre seguridad.
12. Verificación autorizada y documentación de contratos implementados; si hay bloqueos, no presentar el administrador como terminado.

No son requisitos de esta primera entrega: emitir credenciales técnicas, implementar Client Credentials, SDKs de consumo, publicación de versiones o controlar las pantallas de aplicaciones consumidoras.

## 14. Restricciones de ejecución y referencias

- Este documento no autoriza cambios en código, frontends, archivos, despliegues o datos del Building Block de seguridad. Sus componentes siguen siendo de solo lectura para esta tarea.
- No iniciar, detener, recrear, reconstruir ni mutar sus contenedores. Las capacidades faltantes se coordinan con su responsable.
- No modificar secretos, imprimir tokens, guardar credenciales en archivos ni alterar la selección de modelos o permisos de `opencode.jsonc`.
- Configuración local con Doppler: proyecto `messageucolab`, configuración `dev`; no exigir `.env` ni confundir con `dev_azure`.
- Usar Maven Wrapper y Java 25. Toda prueba requiere la autorización y delegación descritas en `AGENTS.md`, incluso si un build Docker las ejecuta indirectamente.
- Registrar fallos y validaciones reales; la autorización histórica de otras tareas no se reutiliza.

Documentación actual que debe conservarse como referencia de comportamiento existente y actualizarse cuando se implemente esta evolución:

- [Instrucciones del proyecto](../AGENTS.md).
- [Integración de seguridad](security-integration.md).
- [Contrato actual de consumo](message-consumer-api.md).
- [Guía actual de integración de consumidores](guia-integracion-consumidores.md).
- `infrastructure/src/main/resources/static/openapi.yaml`.

Los documentos actuales todavía describen partes del contexto activo. Este diseño no los modifica ni anuncia que esa migración ya ocurrió.
