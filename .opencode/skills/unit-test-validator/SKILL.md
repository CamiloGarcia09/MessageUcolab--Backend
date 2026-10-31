---
name: unit-test-validator
description: Usa esta skill después de crear, completar o modificar pruebas unitarias en el proyecto MessageUcoLab, para auditar su calidad real antes de darlas por terminadas — no basta con que compilen y pasen. Detecta asserts triviales, tests vacíos o deshabilitados, mocking indebido de infraestructura real en tests de core, excepciones genéricas en vez de la jerarquía de dominio, violaciones de la arquitectura hexagonal, y verifica que la cobertura JaCoCo reportada sea real y no inflada. Úsala también si el usuario pide "revisa/valida/audita las pruebas que se crearon".
license: Apache-2.0
---

# Validación de pruebas unitarias

Objetivo: confirmar que las pruebas creadas prueban comportamiento real, respetan
la arquitectura hexagonal del proyecto y que la cobertura reportada es confiable
— no solo que `./mvnw test` pase en verde.

Aplica esta validación a TODA prueba nueva o modificada antes de reportarla como
terminada. Si algo falla, no la marques como válida: corrígela o repórtala como
pendiente con la razón exacta.

## 1. Verificación final de ejecución y cobertura

Esta validación forma parte del flujo de `unit-test-agent`. Ejecuta el siguiente
comando una sola vez, después de completar la auditoría estática y todas las
ediciones. No lo ejecutes como baseline ni lo repitas si la tarea actual ya tiene
un resultado final vigente del mismo comando:

```bash
./mvnw clean verify
```

- Confirma BUILD SUCCESS en los 3 módulos (`utils`, `core`, `infrastructure`).
- Abre el reporte JaCoCo de los módulos afectados (`target/site/jacoco/index.html`
  o `jacoco.xml`) y revisa la cobertura de **líneas Y ramas** (branches). Reporta
  el resultado por módulo y cualquier incumplimiento del Quality Gate configurado,
  pero no crees pruebas de clases no relacionadas solo para elevar el porcentaje.
- Sospecha si un archivo pasó de 0% a 100% con muy pocos tests: revisa que no sea
  cobertura "de paso" (el código se ejecuta pero no se verifica nada relevante).

## 2. Detección de asserts triviales o inexistentes

Busca patrones que indican que el test no verifica comportamiento real:

```bash
grep -rn "assertTrue(true)\|assertFalse(false)" --include="*Test.java" src/test
grep -rn "@Disabled\|@Ignore" --include="*Test.java" src/test
grep -rn "// TODO\|fail(\"not implemented" --include="*Test.java" src/test
```

Reglas de rechazo:
- Un `@Test` cuyo único assert es `assertNotNull(objeto)` sin verificar ningún
  valor/campo/comportamiento específico → **rechazar**.
- Un `@Test` que solo verifica que "no lanza excepción" sin comprobar el
  resultado → **rechazar**, salvo que el propósito explícito del test sea ese
  (ej. verificar que un caso válido no lance error de validación) Y además
  verifique el resultado retornado.
- Cualquier `@Disabled`/`@Ignore` sin un comentario que justifique por qué
  (ticket, bug conocido) → **rechazar**.
- Un método de test cuyo cuerpo está vacío o solo tiene un comentario →
  **rechazar**.

## 3. Verificación de arquitectura hexagonal (específico de este proyecto)

Para tests en `core/src/test/**`:
```bash
grep -rln "@SpringBootTest\|org.springframework" core/src/test
grep -rln "SurrealDB\|RedisTemplate\|PulsarClient" core/src/test
```
Si alguno de estos comandos devuelve resultados → **rechazar**. Los tests de
`core` deben ser JUnit 5 + Mockito puro, mockeando puertos (interfaces), nunca
frameworks ni clientes concretos.

Para tests en `infrastructure/src/test/**`:
- Verifica que los tests unitarios (no de integración) mockeen los clientes
  externos (SurrealDB client, `RedisTemplate`, `PulsarClient`, LangChain4j model)
  en vez de instanciarlos reales o requerir `docker compose up`.
- Si un test de infraestructura requiere la infraestructura real corriendo,
  confirma que esté correctamente separado como test de integración (no
  mezclado con la suite unitaria que corre en cada build).

## 4. Verificación de manejo de excepciones

```bash
grep -rn "assertThrows(RuntimeException.class\|assertThrows(Exception.class" --include="*Test.java" src/test
```
Si aparece, **rechazar**: el test debe verificar la excepción específica de la
jerarquía de dominio (`BusinessException`, `ValidationException`,
`ConflictException`, `NotFoundException`, `TechnicalException` o su subclase
concreta), nunca la clase genérica.

Para validadores (`*RuleImpl`), confirma que:
- Cada regla tiene su propia clase de test, no solo se prueba indirectamente a
  través del `CompositeValidator`.
- El `CompositeValidator` tiene al menos un test que confirma que agrega
  correctamente los resultados de varias reglas (no solo prueba una regla
  aislada).

## 5. Verificación de estructura y nomenclatura

Por cada clase de test nueva, confirma:
- [ ] Nombre de método describe comportamiento (`metodo_condicion_resultado` o
      `@DisplayName` equivalente) — rechaza nombres genéricos como `test1()`,
      `testMetodo()`.
- [ ] Estructura Arrange-Act-Assert identificable (aunque no esté comentada).
- [ ] Al menos: 1 caso feliz, 1 caso límite/borde, 1 caso de error (si el método
      puede fallar).
- [ ] Sin imports no usados ni código comentado muerto (esto lo marca
      SonarCloud como code smell en el Quality Gate).
- [ ] Sin duplicación evidente con otro test ya existente en la suite.

## 5.5. Chequeos específicos de SonarCloud (issues recurrentes en este proyecto)

Estos son patrones que SonarCloud marca frecuentemente en las pruebas de este
repo — revísalos explícitamente antes de dar una clase de test por terminada:

- **Asserts múltiples sueltos sobre el mismo objeto/resultado**: si hay ≥2
  `assertEquals`/`assertThat` consecutivos verificando distintos campos de un
  mismo objeto, agrúpalos en `assertAll(...)` o encadénalos con AssertJ
  (`assertThat(obj).extracting(...).containsExactly(...)`). Sonar lo marca como
  "Join these multiple assertions subject to one assertion chain."
  ```bash
  grep -n "assertEquals\|assertThat" ClaseTest.java
  # si hay líneas consecutivas sobre el mismo objeto → agrupar
  ```

- **Verificación de `Optional` vacío**: nunca uses
  `assertThat(x.isPresent()).isFalse()` — usa `assertThat(x).isNotPresent()` o
  `.isEmpty()`. Mismo criterio para `assertTrue(x.isEmpty())` sobre un Optional.

- **Comparación de tipo con `.getClass().equals(...)`**: siempre usa
  `assertThat(obj).isInstanceOf(Clase.class)` o `assertInstanceOf` de JUnit 5,
  nunca comparación manual de `Class`.
  ```bash
  grep -rn "getClass().equals\|getClass() ==" --include="*Test.java" src/test
  ```

- **`throws Exception` innecesario en la firma del método de test**: si el
  cuerpo del test no invoca nada que declare una checked exception, el método
  no debe declarar `throws Exception`. Revisa cada `@Test` con `throws` en su
  firma y confirma que realmente lo necesita.
  ```bash
  grep -rn "void .*() throws Exception {" --include="*Test.java" src/test
  # por cada resultado, confirmar que algo dentro del cuerpo realmente
  # requiere esa declaración; si no, quitarla
  ```

- **Lambda de `assertThrows` con más de una invocación que puede lanzar**:
  Sonar exige que el lambda pasado a `assertThrows(Excepcion.class, () -> {...})`
  contenga SOLO la llamada que se espera que falle. Si el lambda tiene setup
  adicional antes de la llamada real, sepáralo fuera del lambda.
  ```java
  // Mal — Sonar marca "Refactor the code of the lambda to have only one
  // invocation possibly throwing a runtime exception."
  assertThrows(ValidationException.class, () -> {
      Cliente c = repository.buscar(id); // esto también puede lanzar
      validator.validar(c);
  });

  // Bien — separa el setup, deja solo la invocación bajo prueba en el lambda
  Cliente c = repository.buscar(id);
  assertThrows(ValidationException.class, () -> validator.validar(c));
  ```

- **Imports estáticos para Mockito/JUnit en vez de calificados**: usa
  `import static org.mockito.Mockito.times;` y
  `import static org.mockito.Mockito.doThrow;` (entre otros) en vez de escribir
  `Mockito.times(...)` o `Mockito.doThrow(...)` calificado.
  ```bash
  grep -rn "Mockito\.times(\|Mockito\.doThrow(\|Mockito\.verify(" --include="*Test.java" src/test
  ```

- **`eq(...)` innecesario en verificaciones de Mockito**: si TODOS los
  argumentos de un `verify(mock).metodo(eq(a), eq(b))` usan `eq(...)`, quítalo y
  pasa los valores directos — `eq()` solo es necesario cuando se mezcla con
  otros matchers como `any()`.
  ```java
  // Mal
  verify(repository).guardar(eq(cliente), eq(true));
  // Bien
  verify(repository).guardar(cliente, true);
  ```

- **Imports sin usar**: cualquier import (de Mockito, AssertJ, excepciones de
  dominio, etc.) que no se referencie en el archivo debe eliminarse.
  ```bash
  # revisión manual por archivo; no hay un grep universal confiable para esto,
  # pero cualquier import que aparezca 1 sola vez en el archivo (la línea del
  # propio import) es candidato a sobrar
  ```

- **Aserciones específicas de AssertJ en vez de genéricas**: prefiere el
  método dedicado de AssertJ cuando existe, en vez de reconstruirlo a mano:
  - `assertThat(coleccion).contains(esperado)` en vez de verificar con
    `assertTrue(coleccion.contains(esperado))`.
  - `assertThat(obj).hasToString(esperado)` en vez de
    `assertEquals(esperado, obj.toString())`.
  - `assertThat(optional).isNotPresent()`/`.isEmpty()` en vez de
    `assertFalse(optional.isPresent())`.

- **Tests casi idénticos que solo cambian un valor de entrada/salida**: si hay
  3 o más métodos `@Test` con la misma estructura y solo cambia el dato de
  entrada/resultado esperado, conviértelos en un único `@ParameterizedTest`
  en vez de mantenerlos duplicados (ver la skill `java-junit`, sección de
  pruebas parametrizadas).

- **Excepciones genéricas en código de PRODUCCIÓN (no de test)**: si al
  auditar aparece un `throw new RuntimeException(...)` o `Exception` genérica
  en `src/main`, esto está FUERA del alcance de este agente de testing —
  repórtalo como hallazgo aparte, no lo corrijas tú mismo salvo instrucción
  explícita, porque implica tocar lógica de negocio y la jerarquía de
  excepciones de dominio del proyecto.

Si encuentras cualquiera de estos patrones, corrígelo antes de marcar la clase
de test con veredicto ✅ — aunque el build pase y la cobertura esté completa,
un ❌ o ⚠️ de SonarCloud en el PR bloquea el Quality Gate igual.

## 6. Reporte de validación

Al terminar, entrega un veredicto por clase de test revisada, en esta forma:

```
✅ PedidoServiceTest — cumple: AAA, excepciones de dominio, sin mocks indebidos.
❌ ClienteValidatorTest — rechazado: usa assertThrows(Exception.class) en línea 42,
   debe verificar ValidationException específica.
⚠️  PagoRepositoryTest — revisar: mockea RedisTemplate correctamente, pero el
   caso de error solo verifica "no lanza excepción" sin comprobar el resultado.
```

No des por válida ninguna prueba con veredicto ❌. Corrígela tú mismo si tienes
permisos de edición, o repórtala explícitamente como pendiente si no.
