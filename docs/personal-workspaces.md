# Organizaciones personales y aplicaciones propias

## Decisión

Seguridad autentica al usuario y el PEP mantiene los permisos por ruta de la
aplicación MessageUcoLab. Las aplicaciones consumidoras cuyos mensajes se
administran aquí son recursos de negocio: no se registran automáticamente en
seguridad ni comparten identificadores con sus aplicaciones.

El backend reenvía únicamente la cookie BFF a `GET /api/v1/session` del PDP
configurado. El contrato consultado en el cliente administrativo de seguridad
expone `data.subject`, `data.tenantId`, `data.email` y `data.name`.
Una sesión ausente devuelve 401; un BFF inaccesible o con identidad malformada
impide el acceso. No hay fallback por correo, identidad nula o lista global.

## Datos y aislamiento

- `app_user`: referencia local, sin contraseñas ni tokens. Su UUID se deriva de
  la terna `identity_source + tenant_id + subject`, codificada con longitudes
  explícitas. El índice único conserva esa misma identidad. Correo y nombre se
  actualizan desde el BFF, nunca desde el formulario del cliente.
- `organization.owner_user_id`: propietario único de la organización.
- `application.organization_id`: la aplicación hereda la propiedad de su
  organización. Su UUID se genera siempre en el servidor. No duplica `user_id`.
- Los nombres de organizaciones son únicos por propietario; los nombres de
  aplicaciones son únicos dentro de su organización.
- El usuario no puede consultar ni modificar organizaciones ajenas. Los
  catálogos, funcionalidades, mensajes, traducciones y SSE conservan el alcance
  de aplicación y ambiente, con la lista de aplicaciones propias resuelta en
  el servidor para cada petición. No se cachean asignaciones de seguridad.
- No hay colaboradores, invitaciones, membresías ni RBAC local. Una futura
  evolución puede añadir membresías manteniendo organización → aplicación.

`components.security.session.identity-source` tiene por defecto
`security-baseline`. Es un namespace confiable del proveedor de identidad:
mantenerlo estable; cambiarlo o cambiar `tenantId`/`subject` requiere una
migración explícita de identidad. No usar correo como llave de recuperación.

## Contrato HTTP

Todas las rutas usan sesión BFF y permisos por ruta del PEP. Prefijo:
`/messageucolab/v1`.

| Método | Ruta | Resultado |
|---|---|---|
| GET | `/session/user` | `data: [{id, email, name}]` del usuario actual |
| GET | `/organizations` | Organizaciones propias: `data: [{id, name}]` |
| POST | `/organizations` | Cuerpo `{name}`; propietario tomado de la sesión; retorna el elemento creado |
| GET | `/organizations/{organizationId}/applications` | Aplicaciones de una organización propia: `data: [{id, name}]` |
| POST | `/application` | Cuerpo `{name, organizationId, languageId, stateId}`; crea aplicación y sus tres ambientes |
| GET | `/session/applications` | Aplicaciones propias con su organización; `registered` se conserva como `true` |

Los cuerpos no admiten elegir otro propietario. El `id` legado de creación de
aplicación se ignora. Las operaciones de mensajes siguen enviando
`X-Application-Id`; ese UUID ahora es de negocio, no de seguridad. La aplicación
de enforcement del PEP sigue siendo MessageUcoLab.
Se confirmó en el bytecode del starter PEP 0.1.3 que la decisión envía
`applicationName` y `environment` desde su configuración, más ruta y método;
no toma `X-Application-Id` como aplicación de enforcement. Esta consulta es
estática y no sustituye la validación en ejecución.

Organización ajena o inexistente devuelve 403 con el mismo mensaje de dominio
(`FUN_215`), sin revelar la existencia de datos ajenos. UUID de organización
inválido usa la validación de dominio (`FUN_214`). Ver el detalle de esquemas
en `infrastructure/src/main/resources/static/openapi.yaml`.

## Inicialización de nuevos despliegues

Se acordó preparar Azure desde cero porque todavía no contiene datos relevantes.
La estructura vigente se mantiene en los seeds; no se conserva un script separado
de migración incremental para este cambio. La base local ya está actualizada y no
necesita volver a inicializarse.

1. Para una base **nueva y vacía de MessageUcoLab**, ejecutar
   `deployment/docker/scripts/surreal/surreal-init.surql` con la configuración del
   entorno. Incluye `app_user`, el propietario de organización y los índices
   nuevos. No usar el seed completo para actualizar una base con datos que deban
   conservarse: contiene inicialización histórica de catálogos y registros.
2. Para una instancia nueva de Redis de MessageUcoLab, cargar el catálogo mediante
   `deployment/docker/scripts/redis/CatalogMessageInit.sh`. Incluye `TCH_076`,
   `TCH_077`, `FUN_214`, `FUN_215`, `FUN_216`, ya cargadas en el entorno local.
   No es necesario vaciar ni reinicializar el Redis local.
3. El responsable de seguridad debe habilitar recursos y permisos para:
   `GET /messageucolab/v1/session/user`, `GET /messageucolab/v1/organizations`,
   `POST /messageucolab/v1/organizations` y
   `GET /messageucolab/v1/organizations/{organizationId}/applications`, con el
   patrón de ruta que acepte su PEP. Conservar los permisos de las rutas previas.
   Este repositorio no modifica seguridad para habilitarlos.
4. Mantener la verificación automatizada al modificar el flujo. La adaptación,
   auditoría y ejecución iniciales ya se completaron mediante `unit-test-agent`
   con autorización del usuario; ver resultados abajo.
5. Desplegar únicamente MessageUcoLab y su frontend una vez validado el cambio.

**Persistencia en Azure:** eliminar y recrear contenedores no borra necesariamente
la base. Si se reutiliza un volumen o almacenamiento persistente, la estructura
anterior puede permanecer. La futura inicialización desde cero debe contemplar
ese almacenamiento y limitarse a MessageUcoLab, sin modificar el componente de
seguridad ni su despliegue. Esta decisión documental no ejecuta eliminaciones
ni recreaciones en Azure o en el entorno local.

En local se conservaron las organizaciones preexistentes sin `owner_user_id`;
ellas y sus aplicaciones no aparecen a ningún usuario. Cualquier asignación
posterior exige identificar explícitamente la organización y su propietario;
no reclamar datos por nombre o correo ni apropiarse de registros antiguos al
iniciar sesión.

## Estado de verificación

Compilación de producción del backend, build Vite, ESLint de los cuatro archivos
frontend intervenidos y lectura sintáctica del YAML OpenAPI completados.

Posteriormente, con autorización expresa, `unit-test-agent` (GPT-6 Luna Fast)
adaptó y añadió pruebas, aplicó `unit-test-validator` y completó
`.\mvnw.cmd clean verify` en primer plano con **BUILD SUCCESS**. La ejecución
final reportó 1.812 pruebas: 208 en `utils`, 1.088 en `core` y 516 en
`infrastructure`, sin fallos, errores ni omitidas. Se corrigió una incompatibilidad
del nuevo repositorio `final` con proxies de Spring y se habilitó la inyección
de HttpClient para pruebas del contrato BFF sin conectarse a seguridad.

| Módulo | Cobertura de líneas | Cobertura de ramas |
|---|---|---|
| utils | 529/549 — 96,36 % | 64/66 — 96,97 % |
| core | 1712/1798 — 95,22 % | 346/392 — 88,27 % |
| infrastructure | 2056/2265 — 90,77 % | 535/641 — 83,46 % |

Los gates JaCoCo locales (80 % líneas y ramas por módulo) pasaron sin rebajar
umbrales ni introducir exclusiones. Una ejecución anterior quedó en 79,09 % de
ramas en infraestructura y falló; no es el resultado final. Auditoría de pruebas
nuevas/modificadas favorable. Esto no certifica el Quality Gate remoto de
SonarCloud ni una ejecución real de los servicios: se simularon HTTP y SurrealDB.
No se incorporó un framework de pruebas frontend.

## Activación local completada — 22 de octubre de 2026

- Se aplicó exclusivamente la migración transaccional de esquema sobre
  `messageucolab/messageucolab` usando Doppler `messageucolab/dev`. La base pasó
  de 21 a 22 tablas; se confirmó `app_user`, el campo de propietario y todos los
  índices nuevos. Los conteos de las 21 tablas previas se conservaron.
- Se cargaron y se volvieron a leer únicamente `TCH_076`, `TCH_077`, `FUN_214`,
  `FUN_215`, `FUN_216` en Redis, base 0, con los cinco campos de cada entrada
  coincidentes con el catálogo. No se ejecutó el seed completo ni se vació Redis.
- La organización histórica y sus dos aplicaciones siguen conservadas, sin
  propietario asignado. No se muestran en el espacio personal de ningún usuario;
  no son datos borrados. No se crearon organizaciones, aplicaciones o usuarios
  como parte de la comprobación operativa.
- Desde `deployment/docker`, `unit-test-agent` ejecutó en primer plano:

  ```powershell
  doppler run --project messageucolab --config dev -- docker compose up -d --build --no-deps messageucolab
  ```

  Docker reconstruyó la imagen `docker-messageucolab` y recreó únicamente
  `messageucolab-app`. El `clean verify` del Dockerfile ejecutó 208 pruebas en
  `utils`, 1.088 en `core` y 516 en `infrastructure`, sin fallos, errores ni
  omitidas. Los tres controles JaCoCo pasaron y el reactor terminó en
  **BUILD SUCCESS**, sin rebajar gates ni añadir exclusiones.
- El contenedor nuevo quedó `running/healthy` en 8085, con
  `GET /actuator/health` → 200. Las solicitudes sin sesión a las cuatro nuevas
  operaciones se rechazaron con 401; el rechazo también se observó vía Kong
  en `/session/user`. Estas comprobaciones no demuestran por sí solas acceso
  autorizado ni ejecución de los controladores, ya que los filtros actúan antes.
- El servidor Vite ya existente en 5174 respondió 200 y sirve las fuentes del
  flujo de organizaciones personales. Se repitió `npm run build` con éxito.
  No se instalaron dependencias ni se operaron frontends de seguridad.

**Validación autenticada pendiente:** entrar desde el navegador con una sesión
propia, crear una organización y una aplicación, y gestionar sus mensajes.
No se extrajeron cookies o tokens ni se usaron cuentas ajenas para esa validación.
La existencia y asignación efectiva de los permisos PEP para las cuatro rutas
nuevas no se confirmó: si el navegador recibe 403, debe habilitarlos el responsable
de seguridad conforme a la lista anterior. No se modificaron sus archivos,
datos, permisos, contenedores ni despliegue. No se certifica SonarCloud remoto.
