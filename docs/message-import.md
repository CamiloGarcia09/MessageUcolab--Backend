# Importación masiva de mensajes

## Experiencia administrativa

En `MessageUcolab--Frontend/src/components/CreateMessage.vue` se añade una franja
compacta debajo de la creación individual: **Descargar plantilla** e **Importar
Excel**. Abrir o cerrar el asistente conserva lo escrito en el formulario.

El asistente tiene tres pasos:

1. **Archivo y ambientes**: contexto de organización/aplicación de la sesión,
   selección de uno o varios ambientes de esa aplicación y un archivo `.xlsx`.
   Develop aparece seleccionado cuando existe; nunca se selecciona Production
   como sustituto automático. No es necesario completar la creación individual.
2. **Validación**: cantidades de filas válidas/erróneas, filtro de errores,
   paginación de 4–5 filas, detalle por fila y reporte Excel descargable. No se
   crean mensajes al validar. Se debe corregir el archivo completo antes de
   confirmar. Si se cambian archivo o ambientes, se exige una nueva validación.
   El detalle muestra nombres sin UUID y ofrece **← Volver al listado**, conservando
   la página y el filtro seleccionados, sin repetir la validación.
3. **Resultado**: conteo real confirmado, ambientes y referencia del lote, con
   acciones para volver al listado o iniciar otra importación.

La tabla no crece con el archivo. En escritorio se usa un diálogo ancho y compacto;
en móvil, ventanas pequeñas o zoom se conserva el scroll accesible sin ocultar
campos. No se utiliza `overflow: hidden` sobre la página para forzar el encaje.

## Archivo y referencias

Los textos y encabezados actuales están en español; ver
`docs/spanish-localization.md`. La activación del catálogo completo y sus
referencias en un entorno existente se realiza de forma explícita, sin
reinicializar datos. Los resultados históricos de este documento preceden a ese cambio.

- Hasta **500 mensajes**, entre las filas Excel **2–501**, y **5 MiB** de archivo.
- La plantilla incluye `Mensajes`, `Instrucciones` y `Referencias`. Las columnas
  de `Mensajes` son `Código`, `Funcionalidad`, `Tipo`, `Categoría`, `Estado`,
  `Título`, `Contenido`, en ese orden.
- La primera fila de `Instrucciones` está oculta y contiene los metadatos técnicos:
  `A1`, la versión `MESSAGEUCOLAB_IMPORT_V2`; `B1`, el UUID de aplicación. No se
  muestran al completar el archivo. Se comprueban antes de validar las filas,
  pero no se usan como identidad ni autorización. No se admiten versiones anteriores.
- Los desplegables muestran únicamente nombres. El backend exige coincidencia
  exacta (incluidas mayúsculas y tildes) con una única referencia de su catálogo;
  un nombre inexistente o ambiguo es un error de fila, nunca se elige la primera
  coincidencia. Las funcionalidades se buscan solo en la aplicación autorizada.
  Solo se admiten nombres: ni `Nombre [UUID]` ni UUID exactos. La plantilla solo
  ofrece funcionalidades activas; las que ya no están activas se rechazan con
  las mismas reglas de creación individual.
- Los UUID resueltos se conservan internamente en el borrador y se vuelven a
  validar al confirmar. Renombrar o reutilizar un nombre no reasigna el lote a
  otra referencia. El detalle y el reporte conservan los datos originales de la
  fila; si una referencia inválida contiene un UUID, en pantalla se indica usar
  el nombre del catálogo, sin mostrar ese identificador.
- Organización, aplicación y ambientes **no se escriben por fila**. Todos los
  mensajes del lote comparten aplicación y selección explícita de ambientes.
- No se actualizan mensajes ni se crean funcionalidades. Un código existente en
  cualquier ambiente de esa aplicación es un error. Habilitar un mensaje
  existente en otro ambiente es una operación distinta, no una importación nueva.

## Validaciones y seguridad

Se reutiliza `CreateMessageCompositeValidator`, sin copiar sus reglas de negocio:
código obligatorio, formato y longitud de sufijo, título 10–50, contenido 10–100,
catálogos existentes y funcionalidad activa perteneciente a la aplicación.
La creación individual conserva su comportamiento fail-fast; la importación
recoge un error accionable por campo y continúa con las demás filas. No ejecuta
reglas dependientes de un campo que ya falló. Los errores de infraestructura
no se convierten en supuestos errores del usuario.

Los ambientes se validan una vez por lote mediante el composite compartido:
obligatoriedad, UUID, no repetición, existencia y pertenencia. Los códigos se
comparan después de normalizar y se detectan repetidos internos y existentes
con una consulta en lote. Se vuelven a comprobar al confirmar.

El adaptador Excel rechaza macros, fórmulas de celda, vínculos externos,
objetos incrustados y fuentes de datos externas. Antes de abrirlo, limita el
ZIP a 20 MiB expandidos y 2.000 entradas. Rechaza columnas adicionales con datos,
errores de celda, fechas y celdas excesivamente grandes. El reporte escribe
strings, no fórmulas, aun si el texto comienza con `=`.

Todas las rutas permanecen bajo `/messageucolab/v1/messages/imports`, por lo que
`SessionApplicationInterceptor` valida la sesión BFF y la propiedad de la
aplicación. Se exige `X-Application-Id`; no se añaden credenciales, roles ni
excepciones al PEP. Production requiere además confirmación explícita tanto
en frontend como en backend. Esa confirmación no sustituye los permisos.

## Contrato y permisos externos

El contrato está en `infrastructure/src/main/resources/static/openapi.yaml`.
El responsable de seguridad debe registrar/verificar **cada método y ruta**:

| Método | Ruta, relativa a `/messageucolab/v1/messages/imports` | Acción |
|---|---|---|
| GET | `/template` | Descargar plantilla contextualizada |
| POST | `/validate` | Validar multipart con `file` y query `environmentIds=id1,id2` |
| GET | `/{importId}` | Leer el borrador y sus errores actuales |
| GET | `/{importId}/errors` | Descargar reporte Excel |
| GET | `/{importId}/result` | Consultar recibo sin repetir escritura |
| POST | `/{importId}/confirm` | Crear lote; body `{"productionConfirmed": false}` |

Son permisos de negocio, no el rol protegido `ADMIN` de seguridad. Este cambio
no modifica seguridad ni sus frontends, datos, configuración o despliegue.
Un 403 no se resuelve desactivando enforcement ni cambiando rutas del navegador.

Las respuestas JSON de estas rutas usan `{ "data": objeto }`, no un array.
Plantilla y reporte son binarios `.xlsx`. Las respuestas no son cacheables.
Validar puede devolver 200 con errores de filas; confirmar los rechaza con 422.

Las descargas envian `Accept: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet, application/json`
para aceptar Excel en exito y JSON en errores. El filtro de formatos de texto
`AcceptHeaderInterceptor` delega exclusivamente los GET de plantilla y reporte a
la negociacion nativa de WebFlux; no intenta resolver Excel ni esa lista de tipos
mediante `SerializerRegistry`. Las demas rutas conservan su validacion de formatos,
y los filtros de sesion/PEP se siguen ejecutando tambien para las descargas.

## Atomicidad, concurrencia y recuperación

- El borrador pertenece a la aplicación y vive **30 minutos en Redis**, bajo
  claves `message-import:v1:<applicationId>:<importId>`. No cambia los formatos
  de las cachés existentes. La confirmación no acepta filas desde el cliente.
- `core` contiene la coordinación y reglas, libre de nuevas dependencias de
  framework; Excel, Redis, SurrealDB y HTTP quedan en `infrastructure`.
- Una transacción SurrealDB usa `CREATE`, nunca `UPSERT`, para todos los mensajes,
  sus relaciones con ambientes y `message_import_receipt:<importId>`. Comprueba
  referencias dentro de la transacción y conserva el índice único ya existente
  `(application_id, code)`. Un conflicto aborta el lote completo.
- Solo se informa éxito cuando existe el recibo persistido por esa transacción.
  El mismo `importId` recupera el recibo original, incluso después de vencer el
  borrador. El recibo no contiene títulos ni contenidos del archivo.
- Una respuesta perdida o 500 deja un resultado **incierto**, no un supuesto
  fracaso sin escrituras. Reintentar la misma confirmación o consultar `/result`
  permite recuperarlo. El frontend no repite mutaciones al renovar sesión.
- Antes de confirmar, el frontend conserva únicamente el identificador pendiente
  por aplicación en `sessionStorage`. Al reabrir el asistente consulta el resultado
  y, si procede, el borrador, sin volver a importar automáticamente. No guarda
  tokens, credenciales ni filas Excel en el navegador.
- Una revalidación con errores actualiza el borrador para poder descargar el
  reporte vigente. La aplicación propia se comprueba de nuevo en cada petición.

## Activación sin reinicializar datos

Para bases nuevas, el seed ya incluye `message_import_receipt`. Para una base
existente hay un esquema **solo aditivo**: `message-import-schema.surql`.
**No ejecutar el seed completo** como migración de esta funcionalidad.

Con Redis y SurrealDB de MessageUcoLab ya disponibles, desde `deployment/docker`:

```powershell
doppler run --project messageucolab --config dev -- docker compose run --rm --no-deps redis-init --imports-only
doppler run --project messageucolab --config dev -- docker compose run --rm --no-deps surrealdb-import-schema
```

El primer comando carga exclusivamente `IMP_001`–`IMP_016` y `TCH_079`, sin
recargar otros textos. El segundo solo añade la tabla de recibos y sus campos.
No borran mensajes, vacían Redis ni tocan componentes de seguridad.

Tras la autorización y verificación del agente de pruebas, reconstruir únicamente
MessageUcoLab con el procedimiento habitual `--no-deps`. El Dockerfile ejecuta
`clean verify`; no usar su build para saltarse la autorización de pruebas.

El ajuste de nombres y navegación requiere publicar también el frontend y
recargar `IMP_005` e `IMP_012` con el modo `--imports-only` anterior. No exige
migraciones de esquema ni vaciar Redis. Las verificaciones históricas descritas
abajo corresponden a implementaciones anteriores; el ajuste posterior se verificó
con la autorización del usuario y los resultados de la siguiente sección.

## Verificación de plantilla V2 e idioma base

El 28-10-2026, `unit-test-agent` con **GPT-6 Luna Fast** ejecutó y auditó las
pruebas en primer plano, aplicando `java-junit` y `unit-test-validator`.
La ejecución final de `.\mvnw.cmd clean verify` terminó **BUILD SUCCESS** con
Java 25 y Maven Wrapper 3.9.9: **1.977 pruebas**, cero fallos, errores y omitidas.

| Módulo | Pruebas | Líneas JaCoCo | Ramas JaCoCo |
|---|---:|---:|---:|
| utils | 208 | 552/572 — 96,50 % | 64/66 — 96,97 % |
| core | 1.182 | 2040/2141 — 95,28 % | 437/504 — 86,71 % |
| infrastructure | 587 | 2462/2698 — 91,25 % | 682/824 — 82,77 % |

Las regresiones cubren nombres exactos, inexistentes y ambiguos sin selección
arbitraria; rechazo de UUID, etiquetas `Nombre [UUID]` y plantillas V1; fila
técnica oculta en plantilla y reporte V2; confirmación que conserva el ID original
al renombrar/reutilizar el nombre y bloquea la inactivación posterior sin escribir.
También verifican el mapeo real del idioma persistido con SDK SurrealDB simulado
y el envelope HTTP local del listado de aplicaciones, incluida la omisión de
`language` en JSON cuando falta la referencia o su nombre.

La auditoría dio ✅ a `MessageImportUseCaseTest`, `ImportReferenceRuleTest`,
`CatalogItemDTOTest`, `PersonalWorkspaceSurrealAdapterTest`,
`PersonalWorkspaceApplicationsHttpTest` y `MessageImportWorkbookAdapterTest`.
No se rebajaron umbrales ni se añadieron exclusiones. Los 92 XML finales de
infrastructure, leídos por rutas extendidas de Windows, suman 587 nodos
`testcase`, de acuerdo con Maven. La suma de `testsuite.tests` es 585 por las dos
discrepancias heredadas de suites anidadas documentadas más abajo; un conteo
intermedio incompleto de 580 no corresponde al resultado final conciliado.

El build Vite y ESLint dirigido del frontend pasaron. No se añadieron pruebas
automatizadas de navegación visual ni se validaron SonarCloud remoto, SurrealDB,
BFF/PEP o servicios reales. No se inició, reconstruyó ni actualizó ningún
contenedor, ni se cargó el catálogo Redis en ejecución.

## Estado de esta implementación

Validación backend ejecutada el 28-10-2026 con Java 25 mediante
`.\mvnw.cmd clean verify` en primer plano: **BUILD SUCCESS**. El resumen Maven
registró utils 208, core 1178 e infrastructure 570 pruebas, todas con cero fallos,
errores o skips. No se inició Docker ni se consultó/escribió una base real.

JaCoCo (líneas / ramas del módulo): utils **96.50% / 96.97%**; core
**94.74% / 85.91%**; infrastructure **90.69% / 82.11%**. Clases focales:
`MessageImportUseCase` 95.05% / 85.71%; `MessageImportSurrealAdapter` 97.10% /
92.86%; `MessageImportController` 67.65% (sin ramas JaCoCo); `ExcelImportSafety`
96.00% / 82.14%; `MessageImportWorkbookAdapter` 84.71% / 70.13%.

Se añadieron tests unitarios directos de `MessageImportUseCase`,
`MessageImportSurrealAdapter` y `MessageImportController`, además de escenarios
adicionales de validación compuesta, seguridad Excel y workbook/reporte. El adapter
Surreal se probó con el SDK mockeado: las verificaciones acreditan el contrato SQL
y el tratamiento de resultados/recibos, **no** atomicidad en una base SurrealDB
real. El controller se invoca localmente con mocks y `MockServerWebExchange`.

Conciliación Surefire: `Get-ChildItem -LiteralPath
infrastructure/target/surefire-reports -Filter 'TEST-*.xml'` enumeró 90 XML. Los 90
se leyeron por `FileInfo.FullName` usando el prefijo Windows `\\?\` para la ruta
extendida; dos rutas ordinarias medían 261 y 262 caracteres y fallaban con “no se
puede encontrar una parte de la ruta”. Los XML no faltaban: el acceso extendido
permitió leerlos. El total fue 568 al sumar atributos `testsuite/@tests`, pero 570
al contar elementos `testcase`; failures/errors/skipped sumaron cero. El segundo
conteo **sí coincide** con el resumen Maven de infrastructure (570).

La diferencia de dos entre los dos métodos XML procede de dos reportes con un
nombre de testcase repetido en elementos de clase distintos: `CatalogControllerImplTest.xml`
contiene `delegatesWithApplicationIdAndIdentity` dos veces con classnames
`getFunctionalitiesByApplication` y `getEnvironmentsByApplication`; el atributo
`tests` del suite es 8 frente a 9 testcase. `MessageEndpointsRequestMappingTest.xml`
contiene `classMapping_isAnchoredToTheV1MessagesResource` bajo las clases anidadas
`$MessagesControllerEndpoints` y `$CreateMessageControllerEndpoints`; su atributo
`tests` es 5 frente a 6 testcase. Esto documenta por qué sumar el atributo suite
no concilia; no se atribuye a reintentos ni se alteran umbrales/suites. utils y
core coinciden entre el resumen Maven y XML (208 y 1178). La auditoría estática
verificó los tests añadidos/modificados; SonarCloud remoto, frontend y atomicidad
de almacenamiento real no fueron validados.

### Regresión HTTP 406 de descargas (ejecución actual)

Después de la corrección acotada en `AcceptHeaderInterceptor`, se añadió
`MessageImportDownloadHttpRegressionTest`. Con `WebTestClient` contra contexto
WebFlux local se recorren el filtro real y el mapeo/negociación HTTP real; solo se
mockean los puertos/adaptadores del caso de uso y workbook. Las 14 invocaciones
verifican Accept mixto del frontend en plantilla y reporte (status, Content-Type,
bytes, attachment y no-store), Excel y wildcard en ambas descargas, JSON puro con
406 nativo, endpoints ajenos y métodos/rutas fuera de la exención, UUID inválido,
sufijo/prefijo similares, endpoint JSON y conservación de la excepción SSE.
No se simularon sesión/PEP ni se accedió a servicios externos.

La ejecución completa actual de `\.\mvnw.cmd clean verify` terminó **BUILD SUCCESS**
con Java 25. El resumen Maven mostró utils 208, core 1178 e infrastructure 584;
en los tres módulos hubo cero fallos, errores y skips. La conciliación XML de esta
ejecución encontró utils 208 testcase, core 1178 e infrastructure 584 testcase
(91 XML). Se leyeron por `FileInfo.FullName`, usando la ruta extendida Windows
para las rutas largas. En infrastructure la suma de `testsuite/@tests` es 582,
dos menos que los elementos `testcase`; las diferencias están en
`CatalogControllerImplTest.xml` (8 vs. 9, testcase repetido bajo dos classnames)
y `MessageEndpointsRequestMappingTest.xml` (5 vs. 6, métodos de dos clases
anidadas). El conteo de elementos concuerda con Maven; ningún reporte quedó
inaccesible y no hubo fallos/errores/skips en XML.

JaCoCo actual (líneas / ramas): utils **96.50% / 96.97%**, core **94.74% /
85.91%**, infrastructure **91.08% / 82.60%**. `MessageImportController` quedó en
**97.06%** de líneas (sin ramas); `AcceptHeaderInterceptor`, **95.00% / 87.50%**.
La cobertura anterior documentada arriba corresponde a la ejecución precedente
y no sustituye estos valores actuales. La auditoría con `unit-test-validator` dio
✅ a `MessageImportDownloadHttpRegressionTest`: AAA, comprobaciones HTTP
conductuales, sin asserts triviales, imports sin uso ni infraestructura real.

Por instrucción del usuario, esta ejecución fue solo de pruebas: **no se construyó
imagen Docker ni se desplegó/actualizó/inició ningún contenedor**; el usuario se
encarga de actualizar el contenedor. No se validaron SonarCloud remoto, frontend,
sesión/PEP reales ni base real.
