# Textos de MessageUcoLab en español

## Alcance

- Interfaz administrativa, errores de red/HTTP, etiquetas de idiomas, estados,
  tipos y categorías, campos de los reportes y textos de ejemplo.
- Títulos y contenidos del catálogo Redis, instrucciones y plantillas de IA,
  mensajes propios de registro y catálogo de arranque previo a Redis.
- Catálogos de SurrealDB y mensaje de demostración de los seeds, incluidas sus
  proyecciones. El ejemplo tiene ahora idioma base español, coherente con su texto.
- Excepción solicitada: ambientes **Develop**, **Produccion** y **Testing**.

No se traducen UUID, códigos, claves JSON, nombres de tablas, rutas, cabeceras,
enumeraciones técnicas de Redis (`FUNCTIONAL`, `TECHNICAL`, etc.), marcadores
`%s`/`%d`/`{}` ni identificadores del esquema de traducción. La capacidad de
traducir contenido a otros idiomas permanece disponible: el idioma de la
interfaz administrativa no obliga a las aplicaciones consumidoras a usar español.

Los nombres y textos de negocio introducidos por el usuario no se reescriben
automáticamente. Tampoco se modifican seguridad, sus frontends ni sus datos.
Los mensajes producidos por bibliotecas, navegador o servicios externos no forman
parte del catálogo propio y pueden depender de la configuración de esos sistemas.

## Transición sin pérdida de datos

Los nombres de catálogo nuevos son `Activo`/`Inactivo`, `Funcional`/`Técnico`,
`Información`/`Confirmación`/`Advertencia`/`Depuración`/`Error` y
`Inglés`/`Español`/`Francés`. Se conservan sus identificadores.

Las comprobaciones de actividad reconocen los nombres antiguos y nuevos sin
confundir inactivo con activo. La confirmación de producción sigue siendo
obligatoria con `Production`, `Produccion` o `Producción`; la traducción no
desactiva esa protección. La presentación del frontend traduce las etiquetas
conocidas sin cambiar los UUID que envía ni elegir por defecto otro estado al editar.

El listado de aplicaciones lee `language_id.language AS language_name`, de
acuerdo con el esquema real del seed. No consulta el inexistente campo `name`
del idioma ni inventa un idioma si falta la referencia.

## Activación pendiente en un entorno existente

**Los cambios de archivos no actualizan registros ni contenedores en ejecución.**
El usuario autorizó las pruebas y la actualización de catálogos. Las pruebas
terminaron correctamente, pero el contenedor examinado aún no incluye
`CatalogNames.class`, necesario para la transición. El usuario eligió actualizar
el backend por su cuenta. Por ello, **todavía no se escribió ningún catálogo**:
cambiar estados o el nombre de producción con el backend anterior puede bloquear
registros activos o dejar sin efecto la confirmación de producción.

Antes de ejecutar los pasos siguientes, confirmar que el backend actualizado y
compatible está en ejecución. No reconstruir ni reiniciar seguridad o servicios
ajenos para resolver esta condición. Evitar nuevas importaciones durante la
ventana de activación hasta completar los catálogos.

1. Recargar el catálogo **completo** de Redis de MessageUcoLab mediante
   `CatalogMessageInit.sh`, sin `--imports-only`: ahora también cambian los textos
   fuera de importación y se incorporan `TCH_080`–`TCH_105` e `IMP_017`.
   El script hace `HSET` de las claves del catálogo, sin vaciar Redis ni otras cachés.
   Usar Doppler para inyectar credenciales sin imprimirlas.
2. Tras revisar y autorizar la actualización de datos, aplicar exclusivamente
   `deployment/docker/scripts/surreal/catalog-spanish.surql` sobre la base de
   MessageUcoLab. Solo cambia etiquetas de 23 registros de catálogo conocidos,
   dentro de una transacción; no crea ni elimina registros ni cambia relaciones,
   mensajes, propietarios o idiomas seleccionados de aplicaciones existentes.
   No toca proyecciones históricas ni fuerza una traducción de textos de usuario.
   Un conflicto de unicidad debe abortar y revisarse, no solucionarse borrando datos.
3. Publicar también el frontend de MessageUcoLab si todavía no sirve los archivos
   actualizados. El backend compatible debe estar activo **antes** de modificar
   los catálogos, según la condición anterior. No reinicializar la base.
4. Descargar una plantilla actualizada. Sus encabezados son `Código`,
   `Funcionalidad`, `Tipo`, `Categoría`, `Estado`, `Título` y `Contenido`.
   La importación conserva la versión V2 y solo acepta nombres exactos del
   catálogo vigente, sin añadir compatibilidad con referencias UUID.

**Nunca ejecutar `surreal-init.surql` como migración de esta traducción:** el
seed completo contiene inicialización destructiva de demostración. Solo se
actualizó su contenido para nuevas bases inicializadas expresamente.

## Verificación

El 28-10-2026, con autorización del usuario, `unit-test-agent` ejecutó y auditó
las pruebas mediante **GPT-6 Luna Fast** (`openai/gpt-6-luna-fast`), en primer
plano, aplicando `java-junit` y `unit-test-validator`.
`.\mvnw.cmd clean verify` terminó **BUILD SUCCESS** con Java 25: **2.002 pruebas**,
cero fallos, errores y omitidas. Los controles de cobertura JaCoCo pasaron.

| Módulo | Pruebas | Líneas JaCoCo | Ramas JaCoCo |
|---|---:|---:|---:|
| utils | 208 | 591/611 — 96,73 % | 64/66 — 96,97 % |
| core | 1.207 | 2049/2150 — 95,30 % | 457/524 — 87,21 % |
| infrastructure | 587 | 2463/2702 — 91,15 % | 683/826 — 82,69 % |

La auditoría de las 38 clases de pruebas nuevas/modificadas no tuvo veredictos
negativos. Se adaptaron expectativas de mensajes, referencias y encabezados en
español; se comprobó el campo «Contenido» en el reporte mediante `IMP_017` y se
añadieron regresiones para los nombres de catálogo y sus reglas.
Los XML Surefire se conciliaron mediante rutas extendidas de Windows: los
587 nodos `testcase` de infrastructure coinciden con Maven. La suma de atributos
`testsuite.tests` es 585 por las dos discrepancias heredadas de
`CatalogControllerImplTest` y `MessageEndpointsRequestMappingTest`.

Previamente pasaron la compilación de producción, el build Vite, ESLint dirigido
y `bash -n` del seed Redis, sin ejecutarlo. No se hicieron pruebas automatizadas
de navegación visual ni se verificaron SonarCloud remoto o servicios reales.
No se ejecutaron seeds, migraciones, despliegues ni operaciones de seguridad;
la actualización de catálogos queda pendiente del backend que publicará el usuario.
