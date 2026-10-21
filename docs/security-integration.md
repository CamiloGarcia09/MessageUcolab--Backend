# Integración local de MessageUcoLab con Security Baseline

Esta configuración pertenece únicamente a MessageUcoLab. No modifica ni arranca
el stack `security-baseline`, sus repositorios o su frontend. El equipo responsable
de seguridad debe mantener disponibles los servicios y permisos requeridos.

## Proyectos que se usan conjuntamente

Las rutas completas y los puertos están en la sección de coordinación de
`AGENTS.md`. Además del backend y `MessageUcolab--Frontend`, hay tres referencias
externas distintas bajo `Security`:

- `security-baseline-local`: despliegue del Docker `security-baseline`.
- `securityBaseline-vue`: frontend administrativo independiente, puerto **5173**.
- `security-ui`: microfrontend Security UI, puerto **5175**, bundle
  `/security-administration.js`; no es el frontend anterior.

El frontend MessageUcoLab usa **5174**, su backend **8085** y Kong **8000**. Seguridad
publica PDP/BFF en **18080**, PEP en **18081** y Keycloak en
`http://keycloak.localhost:19090`. Estos datos describen el entorno local, no su
disponibilidad actual. No iniciar ni modificar los proyectos externos para
resolver una tarea de MessageUcoLab.

Las autorizaciones históricas para actualizar registros específicos de seguridad
fueron excepciones puntuales ya consumidas. No conceden autorización permanente
ni habilitan nuevas escrituras en la base de datos. Este documento no conserva
identificadores de cuentas, credenciales ni comandos de recuperación reutilizables.

## Starter Maven

`infrastructure/pom.xml` consume el starter publicado
`co.edu.uco:security-pep-integration-spring-boot-starter:0.1.3`, desde el repositorio
GitHub Packages con id `github`. Es la versión disponible en los metadatos
consultados; el `0.1.0-SNAPSHOT` de la guía no está disponible allí.

El POM publicado declara Spring Boot 4.1 y `spring-boot-starter-webclient`.
MessageUcoLab usa **Spring Boot 4.1.1 / Spring Framework 7.0.9**, alineados mediante
el BOM de Boot, y consume ese starter sin excluir WebClient ni alterar su binario.
Se actualizan SpringDoc a **3.1.1** y Spring Cloud Azure a **7.4.0**, manteniendo Java
25 y Maven Wrapper 3.9.9. No se cambia el stack externo de seguridad.

La combinación anterior Boot 3.5 / starter 0.1.3 compilaba pero fallaba en ejecución
con `NoSuchMethodError: HttpHeaders.getOrEmpty(String)`: excluir un starter de Boot 4
no modifica las llamadas a Spring 7 ya compiladas en el filtro. No solucionar este
fallo desactivando enforcement, parcheando el JAR ni mezclando versiones manuales
de Spring Framework. Las rutas protegidas se deben verificar también en ejecución.

## Serialización y datos existentes

- Boot usa Jackson 3 para sus codecs HTTP y el cliente del PEP, con
  `spring.jackson.use-jackson2-defaults=true` para conservar los defaults JSON
  previos. Las anotaciones `com.fasterxml.jackson.annotation` siguen siendo válidas.
  Las fechas HTTP usan `spring.jackson.datatype.datetime.write-dates-as-timestamps=false`;
  la antigua propiedad bajo `spring.jackson.serialization` no es válida en Jackson 3.
- `AdapterJsonConfig` proporciona un mapper Jackson 2 explícito para los adaptadores
  internos existentes (traducción y caché), con Java Time y fechas ISO.
  No reemplaza el mapper HTTP Jackson 3 de Boot.
- Los serializadores JSON/XML/YAML y los formatos persistidos de Redis se conservan.
  YAML se declara como dependencia directa, no como efecto incidental de SpringDoc.
  No borrar cachés ni migrar datos como parte de esta corrección del framework.
- `Jackson2JsonRedisSerializer` sigue presente para leer los valores existentes;
  Spring Data Redis lo marca para retirada futura. Su sustitución requiere una
  migración de formato independiente, no ocultar el aviso ni cambiarlo a ciegas.

### Referencias de compatibilidad

- [Guía oficial de migración a Boot 4](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide).
- [SpringDoc 3 para Boot 4](https://springdoc.org/).
- [Spring Cloud Azure: historial de compatibilidad](https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/spring/CHANGELOG.md):
  la versión 7.4.0 declara Boot 4.0–4.1.0 y compatibilidad esperada con parches 4.1.x.

## Autenticación de Maven

La autenticación Maven usa el servidor `github` de `~/.m2/settings.xml`, ya
configurado por el desarrollador. Su contraseña debe referenciar
`${env.GITHUB_PACKAGES_TOKEN}`, nunca contener un token literal. Si el usuario
referencia `${env.GITHUB_PACKAGES_USER}`, esa variable también debe estar definida.

### GitHub Packages en Azure Pipelines

El agente hospedado no dispone del `~/.m2/settings.xml` del desarrollador, y las
variables del agente no entran automáticamente en `docker run` ni en BuildKit.
La plantilla versionada `.mvn/settings.xml` declara el servidor `github` con
referencias a variables de entorno; no contiene credenciales literales.

Configurar estas variables en Azure DevOps, sin poner sus valores en Git ni en el chat:

| Variable | Valor requerido | Secreta |
|---|---|---|
| `GITHUB_PACKAGES_USER` | Login de GitHub de la cuenta propietaria del token; no el correo ni el nombre visible | No |
| `GITHUB_PACKAGES_TOKEN` | PAT compatible con Maven Packages, vigente, con `read:packages` y acceso al paquete `security-pep-integration-spring-boot-starter` | Sí |

- **CI / PR (`azure-pipelines.yml`)**: definir ambas en Variables del pipeline.
  No se incorpora el grupo de despliegue completo al CI: el PR no necesita
  credenciales de Azure, SSH ni del PEP. Si se prefiere Library, usar un grupo
  exclusivo para lectura de paquetes y autorizar únicamente el pipeline pertinente.
- **CD (`azure-deploy-pipeline.yml`)**: definirlas en el grupo ya existente
  `messageucolab-cicd`, autorizado para ese pipeline. No sustituirlas por
  `System.AccessToken`, `PEP_REGISTRATION_TOKEN` ni el token del CLI de Doppler.
- Si la organización GitHub exige SSO, autorizar el PAT para esa organización.
  Un token expirado o sin acceso al paquete puede seguir produciendo `401/403`
  aunque el YAML esté correcto. No es necesario cambiar la versión del starter.
- Los pasos validan que las variables existan y no sean macros sin resolver,
  mediante `deployment/ci/Assert-GitHubPackagesCredentials.ps1`, sin imprimir valores.
- Maven recibe las variables explícitamente en el contenedor y usa
  `-s .mvn/settings.xml -U`; esto evita depender del settings local y permite
  reconsultar dependencias tras un fallo guardado en caché, sin borrar toda la caché.
- Los builds de imagen de CI y CD entregan `maven_settings` y
  `github_packages_token` como secretos BuildKit. Solo el login no secreto se
  transmite como argumento de build. El Dockerfile existente consume esos secretos
  en sus pasos Maven, sin copiarlos a la imagen final ni al runtime.
- En PRs de forks o de ramas no confiables, no habilitar secretos indiscriminadamente
  para evitar el `401`. Usar una validación en contexto confiable según la política
  del repositorio; ningún script del fork debe poder extraer el PAT.

La corrección del YAML no crea ni renueva secretos en Azure DevOps. Su existencia,
vigencia y autorización deben confirmarse en la ejecución remota; una compilación
local con credenciales propias no demuestra que el pipeline ya tenga acceso.

## Credenciales en Docker

- `PEP_REGISTRATION_TOKEN` pertenece al runtime de MessageUcoLab. Compose lo exige
  e inyecta en el backend desde Doppler (`messageucolab`, configuración `dev`). No
  se pasa al frontend ni al build.
- `GITHUB_PACKAGES_TOKEN` sirve solo para descargar dependencias Maven durante el
  build. Compose lo entrega como secreto de BuildKit; no es un `ARG`, no se copia
  al contexto ni se incorpora a la imagen final.
- El archivo Maven existente se monta como secreto de BuildKit. Por defecto se
  busca en `${HOME}/.m2/settings.xml` o `${USERPROFILE}/.m2/settings.xml` en Windows.
  `MAVEN_SETTINGS_FILE` permite elegir otro archivo sin copiar sus credenciales.
- El usuario Maven puede seguir siendo el literal no secreto del archivo existente
  o usar `GITHUB_PACKAGES_USER`, pasado como argumento de build no secreto.
- Ni los secretos ni `settings.xml` se montan en el contenedor de runtime.

Mantener el token GitHub en el entorno local o gestor de secretos; no copiarlo a
archivos del repositorio. Doppler inyecta las variables del runtime, pero estas no
se propagan automáticamente al contenedor sin su declaración en Compose.

## URL del PEP

- Java ejecutado en el host usa por defecto `http://localhost:18081`.
- El backend en Docker usa `http://host.docker.internal:18081`, mediante
  `SECURITY_PEP_ENFORCEMENT_PEP_URL`. El alias `host-gateway` se configura únicamente
  en el contenedor de MessageUcoLab para acceder al puerto ya publicado del PEP.
- Se puede reemplazar esa URL por la dirección del PEP del entorno. No usar
  `localhost` dentro del contenedor para un PEP externo.
- El HTTP inseguro de la configuración existente es solo para el laboratorio
  local; no reutilizar esa excepción en producción.

## Aplicación y validación de los cambios

Recrear el contenedor de MessageUcoLab será necesario para que reciba las nuevas
variables y el nuevo starter; un simple reinicio no cambia su imagen ni su entorno.
No ejecutar un `up` general, inicializadores de datos ni operaciones sobre seguridad.

El Dockerfile ejecuta pruebas con `clean verify`: pedir autorización para
`unit-test-agent` antes de construir o validar, siempre en primer plano. Una vez
autorizado, desde `deployment/docker`, el agente puede reconstruir y recrear solo
MessageUcoLab con `--no-deps`, preservando el overlay local si corresponde. El
perfil `local-workspace` selecciona el contexto de negocio, no reemplaza el PEP.

No dar la integración por validada únicamente por compilar: comprobar salud,
rechazo sin sesión (401), denegación sin permiso (403), acceso permitido y carga de
Security UI. Si seguridad está detenida, reportar el bloqueo sin arrancarla ni
alterarla para provocar o resolver fallos. No exponer credenciales en las evidencias.

### Evidencia de la migración a Boot 4

La verificación autorizada en primer plano completó `clean verify` en el build de
Docker con **2.068 pruebas**, sin fallos, errores ni omitidas, y los controles JaCoCo
de los tres módulos aprobados. Se probaron el filtro PEP real con Spring 7 y la
convivencia de los mappers Jackson 2/3, sin simular el filtro.

La imagen migrada se desplegó solo en MessageUcoLab. Con seguridad disponible,
los endpoints de salud del backend, gateway, PDP y PEP respondieron 200. Las rutas
de mensajes, workspace local y catálogo respondieron 401 sin sesión tanto por
8085 como por el gateway 8000; desaparecieron el `NoSuchMethodError` y los 502.
Los preflights CORS conservaron origen exacto y credenciales habilitadas.

Durante esa comprobación inicial no estaban disponibles el frontend 5174 ni el
bundle 5175; por tanto, esa evidencia no certificó acceso autenticado ni
administración en navegador. No se alteró ni arrancó ningún componente de
seguridad para obtener esos resultados.

### Hallazgos posteriores en navegador (20 de octubre de 2026)

- Después, el usuario inició seguridad y sus frontends y consiguió iniciar sesión.
  Tras registrar el recurso del workspace y configurar acceso con otro perfil,
  cargaron el contexto y el menú de MessageUcoLab. No interpretar la indisponibilidad
  de la comprobación inicial como el estado permanente de esos proyectos.
- MessageUcoLab condiciona la navegación a la autenticación y a `activeContext`
  (`MessageUcolab--Frontend/src/App.vue`). Si
  `GET /messageucolab/v1/workspace/local` devuelve 403, no se carga ese contexto y
  puede desaparecer todo el menú. El overlay `local-workspace` fija
  UCO / Arquisoft / Develop, pero no evita el control de permisos del PEP.
- El 403 que inicialmente afectaba al listado de mensajes quedó **resuelto**:
  el usuario confirmó que, tras agregar las rutas y sus permisos, todo funciona.
  La captura posterior muestra el listado cargado correctamente. No mantener
  `GET /messageucolab/v1/messages` como bloqueo pendiente; su acceso requiere
  el permiso de negocio correspondiente, no solo compatibilidad del framework.
  Esta confirmación funcional procede del usuario y de la captura, no de una
  nueva ejecución automatizada de todas las operaciones.
- El frontend administrativo externo protege el rol de aplicación `ADMIN` y
  deshabilita sus acciones de recursos, edición y eliminación. Conservar ese rol
  para administración y gestionar permisos de negocio mediante otros roles y
  perfiles; no indicar que deben añadirse recursos a ese `ADMIN` desde esa pantalla.
- En la vista externa de asignaciones, cualquier `validUntil` no nulo se presenta
  como «Revocada», incluso si es futuro, y no hay edición de vigencia. Es un problema
  conocido de presentación del componente externo: no demuestra por sí solo una
  revocación ni convierte el registro en un dato ficticio. No corregirlo aquí ni
  borrar asignaciones para ocultar la etiqueta.

Estos son hallazgos históricos, no una nueva ejecución de pruebas ni una consulta
actual del estado de los servicios. Los cambios puntuales de datos autorizados en
esa sesión no amplían el alcance permitido para futuras intervenciones.

## Retiro del token propietario (20 de octubre de 2026)

- Se retiran emisión, validación RSA, repositorios, modelos y catálogo exclusivos
  del mecanismo propietario, así como su API de escritura de claves en Doppler.
- Doppler CLI sigue inyectando la configuración. La app ya no requiere
  `DOPPLERTOKEN`; el despliegue Azure solo recibe el secreto de lectura del CLI,
  no un segundo secreto de escritura de la aplicación.
- El frontend conserva `credentials: 'include'` y la sesión BFF. Se eliminan los
  parámetros de token sin uso y el respaldo versionado del cliente anterior.
- `local-workspace` conserva el contexto fijo validado por el PEP. Su ambiente
  se transporta en `MessageAccessContext.workspaceEnvironmentId`, no como legado.
- Fuera de ese perfil, `ActiveContextInterceptor` exige identidad externa y no
  interpreta credenciales propietarias. El adaptador real del proveedor hacia
  `ExternalIdentity` no forma parte de este retiro: una sesión PEP no debe
  confundirse con la existencia de ese puente. Sin él, el modo por usuario sigue
  cerrado (`401`), no se habilita un acceso alternativo.
- El seed nuevo no define las tablas ni eventos exclusivos de tokens. No se
  borraron volúmenes, registros persistidos, entradas Redis ni secretos remotos.
  Su eventual limpieza requiere una operación independiente y delimitada.
- No se modifica ni se opera el componente externo de seguridad.

## Recuperación de acceso del frontend (20 de octubre de 2026)

Se identificó en el código público del release `v0.1.6` que `SessionController`
consulta el principal de la sesión BFF, mientras `InternalBffSessionTokenController`
carga el access token almacenado mediante `loadAuthorizedClient`, sin renovarlo en
ese recorrido. Por eso un `200` de `/api/v1/session` no demuestra que la credencial
que utiliza el PEP siga vigente. Los registros consultados contienen rechazos
`401` en el PEP; no se inspeccionaron credenciales de usuario ni se reprodujo su
expiración de forma automatizada.

La adaptación se limita al frontend MessageUcoLab:

- `src/security/session.js` centraliza los rechazos `401` y bloquea el acceso local,
  sin redirigir ni reautenticar automáticamente. Solo el botón **Volver a Conectar**
  abre el endpoint público `/oauth2/authorization/keycloak`, conservando `return_to`.
  No cierra la sesión global ni llama al canal interno de intercambio de tokens.
- `src/api.js` detecta `401` incluso cuando el BFF reconoce la sesión; las consultas
  usan `cache: 'no-store'`. El estado de recuperación se confirma mediante una
  lectura protegida exitosa, nunca por el `200` del BFF ni por una escritura.
- `src/components/SessionExpiredDialog.vue` muestra un diálogo modal nativo, con
  los colores, tipografía y botón del frontend. Conserva el foco dentro del diálogo
  y bloquea la interacción con el fondo. El aviso menciona inactividad o vencimiento,
  porque un `401` no permite atribuir el rechazo exclusivamente a inactividad.
- `sessionStorage` recuerda si se regresó de un intento manual sin recuperar el
  acceso, para mostrar una explicación apropiada. Ya no hay intentos automáticos
  ni ventana de 60 segundos: la reconexión siempre requiere pulsar el botón.
- `src/store.js`, `src/main.js` y `src/App.vue` muestran el bloqueo, suspenden la
  navegación y paran SSE al perder la sesión. El contenido administrativo no se
  sustituye por una pantalla de recuperación: el popup lo cubre y lo deja inerte.
  Tras volver del login, el arranque normal
  recarga el contexto y las lecturas. Ninguna mutación se reenvía automáticamente.
- Los `403` siguen siendo denegaciones de permisos: no desencadenan reautenticación.
- Los fallos de red no se presentan como sesión perdida por inactividad. La
  recuperación de transporte SSE existente sigue siendo independiente del `401`;
  no inicia login ni ejecuta escrituras de negocio.

### Tiempos observados

Se leyó, sin modificarlo, el archivo de importación del contenedor Keycloak
`/opt/keycloak/data/import/security-baseline-realm.json`: define
`accessTokenLifespan: 300` segundos (5 minutos), sin overrides de duración por
cliente en ese archivo. Es vigencia desde la emisión del token, no inactividad.
No se puede garantizar con el archivo de importación que el realm persistido no
haya sido modificado después desde administración.

El archivo no establece explícitamente `ssoSessionIdleTimeout` ni los tiempos
de sesión por cliente. No se consultó el API administrativo ni se asume un valor
de inactividad efectivo a partir del tiempo de vida del access token. El frontend
no introduce un temporizador de cierre por inactividad.

Validación de este incremento: build del frontend con Vite completado. No había
suite ni comando `test` en el frontend; creación/ejecución de pruebas nuevas queda
pendiente de autorización para `unit-test-agent`. No se modificó ni reinició
seguridad, no se ejecutaron mutaciones sobre sus datos y no se desplegó el backend.
