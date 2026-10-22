package co.edu.uco.infraestructure.deployment;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Contrato estático del script de inicialización de SurrealDB.
 *
 * <p>El comportamiento semántico real (import ejecutable, {@code created_at} inmutable,
 * {@code updated_at} recalculado y reejecución del init) se valida contra una instancia
 * desechable de SurrealDB 3.1.4; no puede ejecutarse dentro de Maven porque la tubería de
 * CI ejecuta {@code mvn clean verify} dentro de un contenedor Java sin acceso al demonio de
 * Docker. Esta clase fija en cada build el contrato declarativo que hace posible esas
 * invariantes: definiciones de campo, captura de la hora de la semilla y recreación
 * idempotente de los registros deterministas.</p>
 *
 * <p>La semilla de la aplicación demo fija además exactamente tres environments deterministas
 * (Develop, Testing y Production) que comparten aplicación, estado Active y la captura
 * {@code $init_now} de los timestamps.</p>
 *
 * <p>El archivo se localiza subiendo por los ancestros del directorio de trabajo de Maven. Cuando ese
 * árbol no contiene {@code deployment/} (etapa de build de la imagen Docker, que solo copia poms y
 * {@code src}), el contrato se omite en lugar de romper el build; si {@code deployment/} sí está pero el
 * script no, la prueba falla para denunciar un archivo movido o renombrado.</p>
 */
class SurrealInitScriptContractTest {

    private static final Path SCRIPT_RELATIVE_PATH =
            Path.of("deployment", "docker", "scripts", "surreal", "surreal-init.surql");

    private static final Path DEPLOYMENT_DIRECTORY = Path.of("deployment");

    private static final Path MODULE_DIRECTORY = Path.of("infrastructure");

    private static final String SCRIPT_NOT_PACKAGED_MESSAGE = SCRIPT_RELATIVE_PATH
            + " no está disponible y deployment/ tampoco forma parte de este contexto de build; el contrato "
            + "solo aplica donde el árbol de fuentes completo está presente";

    private static final Pattern COMMENT = Pattern.compile("--[^\\r\\n]*");
    private static final Pattern CREATED_AT_DEFINITION =
            Pattern.compile("^DEFINE FIELD\\s+(?:IF NOT EXISTS\\s+|OVERWRITE\\s+)?created_at\\b",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern UPDATED_AT_DEFINITION =
            Pattern.compile("^DEFINE FIELD\\s+(?:IF NOT EXISTS\\s+|OVERWRITE\\s+)?updated_at\\b",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern FIELD_TABLE = Pattern.compile("\\bON\\s+([a-z_]+)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern VALUE_COMPUTATION = Pattern.compile("\\bVALUE\\b");
    private static final Pattern FIXED_DATETIME_LITERAL = Pattern.compile("\\bd'\\d{4}-\\d{2}-\\d{2}T");
    private static final Pattern TIMESTAMP_ASSIGNMENT =
            Pattern.compile("\\b(created_at|updated_at|generated_at)\\s*:\\s*([^,}\\r\\n]+)");
    private static final Pattern RECORD_TARGET = Pattern.compile("^(UPSERT|DELETE)\\s+([a-z_]+:`[^`]+`)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern INIT_NOW_CAPTURE =
            Pattern.compile("^LET\\s+\\$init_now\\s*=\\s*time::now\\(\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SEED_FIELD = Pattern.compile("\\b([a-z_]+)\\s*:\\s*([^,}\\r\\n]+)");
    private static final Pattern CATALOG_UPSERT =
            Pattern.compile("^UPSERT\\s+([a-z_]+):`([^`]+)`", Pattern.CASE_INSENSITIVE);
    private static final Pattern CATALOG_NAME = Pattern.compile("\\bname\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern RECORD_ID = Pattern.compile("^[a-z_]+:`([^`]+)`$", Pattern.CASE_INSENSITIVE);

    private static final int DEMO_ENVIRONMENT_COUNT = 3;

    private static final List<String> DEMO_ENVIRONMENT_TYPES = List.of("Develop", "Testing", "Production");

    private static final List<String> ENVIRONMENT_TIMESTAMP_FIELDS = List.of("created_at", "updated_at");

    private static final String DEMO_ENVIRONMENT_PREFIX = "UPSERT environment:`";

    private static final String DEMO_APPLICATION_PREFIX = "UPSERT application:`";

    private static final String INIT_NOW_VALUE = "$init_now";

    private static final Set<String> TABLES_WITH_IMMUTABLE_CREATED_AT = Set.of(
            "domain_events", "organization", "application", "environment", "functionality", "message",
            "message_environment");

    private static final Set<String> TABLES_WITH_MUTABLE_UPDATED_AT = Set.of(
            "organization", "application", "environment", "functionality", "message", "message_environment");

    private String script;

    @BeforeEach
    void loadInitScript() {
        Path startDirectory = Path.of("").toAbsolutePath();
        Path scriptPath = findInitScript(startDirectory);
        if (scriptPath != null) {
            script = readInitScript(scriptPath);
        } else if (hasDeploymentContext(startDirectory)) {
            fail(String.format(
                    "%s forma parte del árbol de fuentes, por tanto %s debe existir buscando hacia arriba desde %s",
                    DEPLOYMENT_DIRECTORY, SCRIPT_RELATIVE_PATH, startDirectory));
        }
    }

    @Nested
        @DisplayName("created_at es inmutable en las tablas que lo declaran")
    class ImmutableCreatedAt {

        @BeforeEach
        void assumeScriptAvailable() {
            Assumptions.assumeTrue(script != null, SCRIPT_NOT_PACKAGED_MESSAGE);
        }

        @Test
        @DisplayName("Toda definición de created_at usa DEFAULT time::now() con READONLY, nunca VALUE")
        void createdAtDefinitions_elevenTables_useDefaultTimeNowAndReadonly() {
            List<String> violations = createdAtFieldViolations(script);

            assertAll(
                    () -> assertThat(violations).isEmpty(),
                    () -> assertThat(tablesDefining(script, CREATED_AT_DEFINITION))
                            .containsExactlyInAnyOrderElementsOf(TABLES_WITH_IMMUTABLE_CREATED_AT));
        }

        @Test
        @DisplayName("Los updated_at declarados siguen usando VALUE time::now() para recalcularse")
        void updatedAtDefinitions_eightTables_keepValueTimeNow() {
            assertThat(tablesDefining(script, UPDATED_AT_DEFINITION))
                    .containsExactlyInAnyOrderElementsOf(TABLES_WITH_MUTABLE_UPDATED_AT);
        }

        @Test
        @DisplayName("Se denuncia created_at cuando alguien lo devuelve a VALUE time::now()")
        void createdAtDefinitions_reportViolation_whenReadOnlyIsReplacedByValue() {
            String regression = script.replace(
                    "ON organization TYPE datetime DEFAULT time::now() READONLY",
                    "ON organization TYPE datetime VALUE time::now()");

            assertThat(createdAtFieldViolations(regression))
                    .anySatisfy(violation -> assertThat(violation).contains("organization"));
        }
    }

    @Nested
    @DisplayName("La semilla toma la hora de la captura $init_now en vez de fechas fijas")
    class SeedTimestampsUseCapturedNow {

        @BeforeEach
        void assumeScriptAvailable() {
            Assumptions.assumeTrue(script != null, SCRIPT_NOT_PACKAGED_MESSAGE);
        }

        @Test
        @DisplayName("Todo created_at, updated_at o generated_at de la semilla vale $init_now")
        void seedTimestamps_assignInitNow_toEveryTimestampField() {
            assertAll(
                    () -> assertThat(seedTimestampViolations(script)).isEmpty(),
                    () -> assertThat(fixedDatetimeLiterals(script)).isEmpty());
        }

        @Test
        @DisplayName("Un apóstrofe de comentario no se interpreta como fecha fija, pero una fecha real sí")
        void fixedDateDetection_ignoresComments_butDetectsRealLiterals() {
            String commentedOnly = "-- la fecha seed's d'2026-01-01T00:00:00Z queda fuera\n"
                    + "UPSERT organization:`8f2d3a10-6b47-4c91-a5e8-2d7f9b3c1a40` CONTENT "
                    + "{ created_at: $init_now };";
            String withRealLiteral = commentedOnly.replace("$init_now", "d'2026-01-01T00:00:00Z'");

            assertAll(
                    () -> assertThat(fixedDatetimeLiterals(commentedOnly)).isEmpty(),
                    () -> assertThat(fixedDatetimeLiterals(withRealLiteral)).hasSize(1));
        }

        @Test
        @DisplayName("Se denuncia la fecha fija 2026-01-01 si vuelve a la semilla")
        void seedTimestamps_reportViolation_whenFixedDateIsRestored() {
            String regression = script.replace("created_at: $init_now",
                    "created_at: d'2026-01-01T00:00:00Z'");

            assertThat(seedTimestampViolations(regression))
                    .anySatisfy(violation -> assertThat(violation).contains("organization"));
        }

        @Test
        @DisplayName("Se denuncia la semilla si desaparece la captura LET $init_now = time::now()")
        void seedTimestamps_reportViolation_whenInitNowCaptureIsMissing() {
            String regression = script.replace("LET $init_now = time::now();", "");

            assertThat(seedTimestampViolations(regression))
                    .anySatisfy(violation -> assertThat(violation).contains("$init_now"));
        }
    }

    @Nested
    @DisplayName("El init puede reejecutarse porque la semilla se borra antes de recrearse")
    class ReExecutionIsIdempotent {

        @BeforeEach
        void assumeScriptAvailable() {
            Assumptions.assumeTrue(script != null, SCRIPT_NOT_PACKAGED_MESSAGE);
        }

        @Test
        @DisplayName("Cada registro de la semilla se elimina antes de su UPSERT y no se duplica")
        void seedRecords_areDeletedBeforeUpsert() {
            assertThat(reExecutionViolations(script)).isEmpty();
        }

        @Test
        @DisplayName("La semilla recrea diez registros deterministas sin repetir ninguno")
        void seedRecords_keepTwentyOneDeterministicTargets() {
            List<String> targets = seedUpsertTargets(script);

            assertAll(
                    () -> assertThat(targets).doesNotHaveDuplicates(),
                    () -> assertThat(targets).hasSize(10),
                    () -> assertThat(targets).contains("organization:`8f2d3a10-6b47-4c91-a5e8-2d7f9b3c1a40`",
                            "environment:`7b3e5d91-a2c8-46f0-9d14-5e7a1b6c3f82`",
                            "environment:`ebe114e4-01d4-427b-9773-b63325ddd55c`",
                            "environment:`de1b8223-1e9c-4807-beec-b2c1cff44e73`",
                            "message:`b6d3f821-7a4e-49c5-8d12-3f0b9e6a274c`",
                            "message_environment_readmodel:`2e7a4c91-6b35-48fd-a2e8-1c9d5f703b64`"));
        }

        @Test
        @DisplayName("Se denuncia el UPSERT del mensaje si se elimina su DELETE previo")
        void seedRecords_reportViolation_whenDeleteBeforeUpsertIsRemoved() {
            String regression = script.replace(
                    "DELETE message:`b6d3f821-7a4e-49c5-8d12-3f0b9e6a274c`;", "");

            assertThat(reExecutionViolations(regression))
                    .anySatisfy(violation -> assertThat(violation)
                            .contains("message:`b6d3f821-7a4e-49c5-8d12-3f0b9e6a274c`"));
        }
    }

    @Nested
    @DisplayName("La aplicación demo se siembra con exactamente tres environments: Develop, Testing y Production")
    class DemoApplicationEnvironments {

        @BeforeEach
        void assumeScriptAvailable() {
            Assumptions.assumeTrue(script != null, SCRIPT_NOT_PACKAGED_MESSAGE);
        }

        @Test
        @DisplayName("Los tres environments comparten aplicación, estado Active, tipos y timestamps $init_now")
        void seedEnvironments_demoApplication_shareApplicationActiveStateTypesAndInitNow() {
            assertThat(demoEnvironmentViolations(script)).isEmpty();
        }

        @Test
        @DisplayName("Se denuncia el recuento si la semilla deja de recrear exactamente tres environments")
        void seedEnvironments_reportViolation_whenOnlyTwoEnvironmentsAreSeeded() {
            String regression = script.replace(
                    "UPSERT environment:`ebe114e4-01d4-427b-9773-b63325ddd55c` CONTENT",
                    "UPSERT environment_draft:`ebe114e4-01d4-427b-9773-b63325ddd55c` CONTENT");

            assertThat(demoEnvironmentViolations(regression))
                    .anySatisfy(violation -> assertThat(violation).contains("exactamente 3"));
        }

        @Test
        @DisplayName("Se denuncia la semilla si el environment de Testing adopta el tipo Develop")
        void seedEnvironments_reportViolation_whenTestingEnvironmentTypeIsReplacedByDevelop() {
            String regression = script.replace(
                    "type_id: environment_type:`19b374bd-503a-4e65-b729-4c2442a69a4e`",
                    "type_id: environment_type:`3ba48618-abb9-40c2-b700-e4413dd9332b`");

            assertThat(demoEnvironmentViolations(regression))
                    .anySatisfy(violation -> assertThat(violation).contains("Testing"));
        }

        @Test
        @DisplayName("Se denuncia la semilla si los environments dejan de compartir el estado Active")
        void seedEnvironments_reportViolation_whenActiveStateIsReplacedByInactive() {
            String regression = script.replace(
                    "state_id: environment_state:`e6e00788-eda8-40fd-aa31-5763133e8834`",
                    "state_id: environment_state:`ec30283f-2b63-485e-ab7a-5bd6f44c8878`");

            assertThat(demoEnvironmentViolations(regression))
                    .anySatisfy(violation -> assertThat(violation).contains("state_id"));
        }

        @Test
        @DisplayName("Se denuncia la semilla si los environments dejan de apuntar a la aplicación demo")
        void seedEnvironments_reportViolation_whenDemoApplicationIsReplaced() {
            String regression = script.replace(
                    "application_id: application:`c4a91e72-8d36-4f5b-b2a7-6e1c9d804f23`,",
                    "application_id: application:`22222222-2222-2222-2222-222222222222`,");

            assertThat(demoEnvironmentViolations(regression))
                    .anySatisfy(violation -> assertThat(violation).contains("application_id"));
        }
    }

    @Nested
    @DisplayName("El script se localiza subiendo por los ancestros del directorio de trabajo")
    class ScriptResolution {

        @Test
        @DisplayName("Encuentra surreal-init.surql al subir desde un subdirectorio cuando deployment/ está incluido")
        void findInitScript_nestedDirectory_returnsScriptWhenDeploymentIsPackaged(@TempDir Path sandbox)
                throws IOException {
            Path expectedScript = sandbox.resolve(SCRIPT_RELATIVE_PATH);
            Files.createDirectories(expectedScript.getParent());
            Files.writeString(expectedScript, "-- contrato", StandardCharsets.UTF_8);
            Path startDirectory = Files.createDirectories(sandbox.resolve(MODULE_DIRECTORY).resolve("target"));

            Path scriptPath = findInitScript(startDirectory);

            assertThat(scriptPath).isEqualTo(expectedScript);
        }

        @Test
        @DisplayName("Sin deployment/ en los ancestros no encuentra el script ni contexto empaquetado")
        void findInitScript_withoutDeploymentContext_returnsNullAndReportsUnpackagedContext(@TempDir Path sandbox)
                throws IOException {
            Path startDirectory = Files.createDirectories(sandbox.resolve(MODULE_DIRECTORY));

            assertAll(
                    () -> assertThat(findInitScript(startDirectory)).isNull(),
                    () -> assertThat(hasDeploymentContext(startDirectory)).isFalse());
        }

        @Test
        @DisplayName("Con deployment/ presente pero sin el script, el contexto se detecta para fallar en vez de omitir")
        void hasDeploymentContext_deploymentWithoutScript_reportsPackagedContext(@TempDir Path sandbox)
                throws IOException {
            Path startDirectory = Files.createDirectories(sandbox.resolve(MODULE_DIRECTORY));
            Files.createDirectories(sandbox.resolve(DEPLOYMENT_DIRECTORY).resolve("docker"));

            assertAll(
                    () -> assertThat(hasDeploymentContext(startDirectory)).isTrue(),
                    () -> assertThat(findInitScript(startDirectory)).isNull());
        }
    }

    private static String readInitScript(Path scriptPath) {
        try {
            return Files.readString(scriptPath, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static Path findInitScript(Path startDirectory) {
        Path directory = startDirectory;
        while (directory != null) {
            Path candidate = directory.resolve(SCRIPT_RELATIVE_PATH);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            directory = directory.getParent();
        }
        return null;
    }

    private static boolean hasDeploymentContext(Path startDirectory) {
        Path directory = startDirectory;
        while (directory != null) {
            if (Files.isDirectory(directory.resolve(DEPLOYMENT_DIRECTORY))) {
                return true;
            }
            directory = directory.getParent();
        }
        return false;
    }

    private static List<String> createdAtFieldViolations(String initScript) {
        List<String> violations = new ArrayList<>();
        for (String statement : statements(initScript)) {
            if (!CREATED_AT_DEFINITION.matcher(statement).find()) {
                continue;
            }
            String table = tableOf(statement);
            if (!statement.contains("TYPE datetime")) {
                violations.add("created_at de " + table + " no está declarado como datetime");
            }
            if (!statement.contains("DEFAULT time::now()")) {
                violations.add("created_at de " + table + " debe usar DEFAULT time::now()");
            }
            if (!statement.contains("READONLY")) {
                violations.add("created_at de " + table + " debe declararse READONLY");
            }
            if (VALUE_COMPUTATION.matcher(statement).find()) {
                violations.add("created_at de " + table + " no debe calcularse con VALUE");
            }
        }
        return violations;
    }

    private static List<String> seedTimestampViolations(String initScript) {
        List<String> violations = new ArrayList<>();
        if (initNowCaptureIndex(initScript) < 0) {
            violations.add("Falta la captura LET $init_now = time::now() antes de la semilla");
            return violations;
        }
        List<String> seedUpserts = seedUpserts(initScript);
        if (seedUpserts.isEmpty()) {
            violations.add("La sección posterior a LET $init_now no contiene UPSERT de semilla");
            return violations;
        }
        for (String statement : seedUpserts) {
            Matcher literal = FIXED_DATETIME_LITERAL.matcher(statement);
            if (literal.find()) {
                violations.add("Fecha fija " + literal.group() + " en " + targetOf(statement));
            }
            Matcher assignment = TIMESTAMP_ASSIGNMENT.matcher(statement);
            while (assignment.find()) {
                String value = assignment.group(2).trim();
                if (!INIT_NOW_VALUE.equals(value)) {
                    violations.add(assignment.group(1) + " de " + targetOf(statement) + " vale " + value);
                }
            }
        }
        return violations;
    }

    private static List<String> reExecutionViolations(String initScript) {
        List<String> violations = new ArrayList<>();
        Set<String> deleted = new LinkedHashSet<>();
        Set<String> upserted = new LinkedHashSet<>();
        for (String statement : seedStatements(initScript)) {
            Matcher matcher = RECORD_TARGET.matcher(statement);
            if (!matcher.find()) {
                continue;
            }
            String record = matcher.group(2);
            if ("DELETE".equalsIgnoreCase(matcher.group(1))) {
                deleted.add(record);
            } else if (!deleted.contains(record)) {
                violations.add("UPSERT sin DELETE previo para " + record);
            } else if (!upserted.add(record)) {
                violations.add("UPSERT repetido para " + record);
            }
        }
        return violations;
    }

    private static List<String> fixedDatetimeLiterals(String initScript) {
        List<String> matches = new ArrayList<>();
        for (String statement : statements(initScript)) {
            if (FIXED_DATETIME_LITERAL.matcher(statement).find()) {
                matches.add(statement);
            }
        }
        return matches;
    }

    private static List<String> seedUpsertTargets(String initScript) {
        List<String> targets = new ArrayList<>();
        for (String statement : seedUpserts(initScript)) {
            targets.add(targetOf(statement));
        }
        return targets;
    }

    private static List<String> demoEnvironmentViolations(String initScript) {
        List<String> environments = seedEnvironmentUpserts(initScript);
        Map<String, String> typeNames = catalogNames(initScript, "environment_type");
        String application = seedApplicationReference(initScript);
        String activeState = recordReference("environment_state",
                idNamed(catalogNames(initScript, "environment_state"), "Active"));

        List<String> violations = new ArrayList<>(environmentCountViolations(environments.size()));
        violations.addAll(sharedEnvironmentViolations(environments, application, activeState));
        violations.addAll(environmentTimestampViolations(environments));
        violations.addAll(environmentTypeViolations(environments, typeNames));
        return violations;
    }

    private static List<String> environmentCountViolations(int environmentCount) {
        if (environmentCount == DEMO_ENVIRONMENT_COUNT) {
            return List.of();
        }
        return List.of("La semilla debe recrear exactamente " + DEMO_ENVIRONMENT_COUNT
                + " environments de la aplicación demo, pero recrea " + environmentCount);
    }

    private static List<String> sharedEnvironmentViolations(List<String> environments, String application,
            String activeState) {
        List<String> violations = new ArrayList<>();
        if (application == null) {
            violations.add("La semilla no recrea la aplicación demo que deben compartir los environments");
        }
        if (activeState == null) {
            violations.add("El catálogo environment_state no define el estado Active compartido");
        }
        if (application == null || activeState == null) {
            return violations;
        }
        for (String environment : environments) {
            Map<String, String> fields = seedFields(environment);
            String target = targetOf(environment);
            addMismatch(violations, "application_id", target, fields.get("application_id"), application);
            addMismatch(violations, "state_id", target, fields.get("state_id"), activeState);
        }
        return violations;
    }

    private static List<String> environmentTimestampViolations(List<String> environments) {
        List<String> violations = new ArrayList<>();
        for (String environment : environments) {
            Map<String, String> fields = seedFields(environment);
            String target = targetOf(environment);
            for (String field : ENVIRONMENT_TIMESTAMP_FIELDS) {
                addMismatch(violations, field, target, fields.get(field), INIT_NOW_VALUE);
            }
        }
        return violations;
    }

    private static List<String> environmentTypeViolations(List<String> environments, Map<String, String> typeNames) {
        List<String> violations = new ArrayList<>();
        Map<String, Integer> occurrences = new LinkedHashMap<>();
        for (String environment : environments) {
            String target = targetOf(environment);
            String typeReference = seedFields(environment).get("type_id");
            String typeName = typeReference == null ? null : typeNames.get(recordId(typeReference));
            if (typeName == null) {
                violations.add("type_id de " + target + " no resuelve a un environment_type sembrado: "
                        + typeReference);
            } else {
                occurrences.merge(typeName, 1, Integer::sum);
            }
        }
        violations.addAll(environmentTypeCoverageViolations(occurrences));
        return violations;
    }

    private static List<String> environmentTypeCoverageViolations(Map<String, Integer> occurrences) {
        List<String> violations = new ArrayList<>();
        for (String expectedType : DEMO_ENVIRONMENT_TYPES) {
            int count = occurrences.getOrDefault(expectedType, 0);
            if (count != 1) {
                violations.add("El environment_type " + expectedType + " debe aparecer una sola vez, aparece "
                        + count);
            }
        }
        for (String unexpectedType : occurrences.keySet()) {
            if (!DEMO_ENVIRONMENT_TYPES.contains(unexpectedType)) {
                violations.add("El environment_type " + unexpectedType + " no es Develop, Testing ni Production");
            }
        }
        return violations;
    }

    private static void addMismatch(List<String> violations, String field, String target, String actual,
            String expected) {
        if (!expected.equals(actual)) {
            violations.add(field + " de " + target + " vale " + actual + " y debe ser " + expected);
        }
    }

    private static List<String> seedEnvironmentUpserts(String initScript) {
        List<String> environments = new ArrayList<>();
        for (String statement : seedUpserts(initScript)) {
            if (statement.startsWith(DEMO_ENVIRONMENT_PREFIX)) {
                environments.add(statement);
            }
        }
        return environments;
    }

    private static String seedApplicationReference(String initScript) {
        for (String statement : seedUpserts(initScript)) {
            if (statement.startsWith(DEMO_APPLICATION_PREFIX)) {
                return targetOf(statement);
            }
        }
        return null;
    }

    private static Map<String, String> catalogNames(String initScript, String table) {
        Map<String, String> names = new LinkedHashMap<>();
        for (String statement : statements(initScript)) {
            Matcher target = CATALOG_UPSERT.matcher(statement);
            if (!target.find() || !table.equals(target.group(1))) {
                continue;
            }
            Matcher name = CATALOG_NAME.matcher(statement);
            if (name.find()) {
                names.put(target.group(2), name.group(1));
            }
        }
        return names;
    }

    private static String idNamed(Map<String, String> catalog, String name) {
        return catalog.entrySet().stream()
                .filter(entry -> name.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    private static String recordReference(String table, String id) {
        return id == null ? null : table + ":`" + id + "`";
    }

    private static String recordId(String recordReference) {
        Matcher matcher = RECORD_ID.matcher(recordReference);
        return matcher.find() ? matcher.group(1) : recordReference;
    }

    private static Map<String, String> seedFields(String statement) {
        Map<String, String> fields = new LinkedHashMap<>();
        int contentIndex = statement.indexOf('{');
        Matcher matcher = SEED_FIELD.matcher(contentIndex < 0 ? statement : statement.substring(contentIndex));
        while (matcher.find()) {
            fields.putIfAbsent(matcher.group(1), matcher.group(2).trim());
        }
        return fields;
    }

    private static Set<String> tablesDefining(String initScript, Pattern definition) {
        Set<String> tables = new LinkedHashSet<>();
        for (String statement : statements(initScript)) {
            if (definition.matcher(statement).find()) {
                tables.add(tableOf(statement));
            }
        }
        return tables;
    }

    private static List<String> seedUpserts(String initScript) {
        List<String> upserts = new ArrayList<>();
        for (String statement : seedStatements(initScript)) {
            if (statement.startsWith("UPSERT")) {
                upserts.add(statement);
            }
        }
        return upserts;
    }

    private static List<String> seedStatements(String initScript) {
        List<String> all = statements(initScript);
        int capture = initNowCaptureIndex(initScript);
        if (capture < 0) {
            return List.of();
        }
        return all.subList(capture + 1, all.size());
    }

    private static int initNowCaptureIndex(String initScript) {
        List<String> all = statements(initScript);
        for (int index = 0; index < all.size(); index++) {
            if (INIT_NOW_CAPTURE.matcher(all.get(index)).find()) {
                return index;
            }
        }
        return -1;
    }

    private static String tableOf(String statement) {
        Matcher matcher = FIELD_TABLE.matcher(statement);
        return matcher.find() ? matcher.group(1) : "<sin tabla>";
    }

    private static String targetOf(String statement) {
        Matcher matcher = RECORD_TARGET.matcher(statement);
        return matcher.find() ? matcher.group(2) : "<sin registro>";
    }

    private static List<String> statements(String initScript) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean insideSingleQuotes = false;
        boolean insideBackticks = false;
        for (char character : stripComments(initScript).toCharArray()) {
            if (character == '\'' && !insideBackticks) {
                insideSingleQuotes = !insideSingleQuotes;
                current.append(character);
            } else if (character == '`' && !insideSingleQuotes) {
                insideBackticks = !insideBackticks;
                current.append(character);
            } else if (character == ';' && !insideSingleQuotes && !insideBackticks) {
                addStatement(statements, current.toString());
                current.setLength(0);
            } else {
                current.append(character);
            }
        }
        addStatement(statements, current.toString());
        return statements;
    }

    private static void addStatement(List<String> statements, String rawStatement) {
        String normalized = rawStatement.replaceAll("\\s+", " ").trim();
        if (!normalized.isEmpty()) {
            statements.add(normalized);
        }
    }

    private static String stripComments(String initScript) {
        return COMMENT.matcher(initScript).replaceAll("");
    }
}
