package co.edu.uco.application.primaryports.dto.catalog;

import co.edu.uco.crosscutting.helpers.json.UtilMapperJson;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class CatalogItemDTOTest {

    @Nested
    class Getters {

        @Test
        void create_returnsTrimmedIdAndName() {
            CatalogItemDTO dto = CatalogItemDTO.create("  app-1  ", "  Message App  ");

            assertAll(
                    () -> assertThat(dto.getId()).isEqualTo("app-1"),
                    () -> assertThat(dto.getName()).isEqualTo("Message App"));
        }

        @Test
        void constructor_returnsTrimmedIdAndName() {
            CatalogItemDTO dto = new CatalogItemDTO("  app-1  ", "  env-1  ");

            assertAll(
                    () -> assertThat(dto.getId()).isEqualTo("app-1"),
                    () -> assertThat(dto.getName()).isEqualTo("env-1"));
        }

        @Test
        void create_returnsEmptyStrings_whenValuesAreNull() {
            CatalogItemDTO dto = CatalogItemDTO.create(null, null);

            assertAll(
                    () -> assertThat(dto.getId()).isEmpty(),
                    () -> assertThat(dto.getName()).isEmpty());
        }
    }

    @Nested
    class Immutability {

        @Test
        void class_isNotARecord() {
            assertThat(CatalogItemDTO.class.isRecord()).isFalse();
        }

        @Test
        void classIsSealedAndDeclaredFieldsAreFinal_withoutExposingSetters() {
            List<Field> fields = List.of(CatalogItemDTO.class.getDeclaredFields());
            List<String> setters = Arrays.stream(CatalogItemDTO.class.getMethods())
                    .map(Method::getName)
                    .filter(name -> name.startsWith("set"))
                    .toList();

            assertAll(
                    () -> assertThat(CatalogItemDTO.class.isSealed()).isTrue(),
                    () -> assertThat(CatalogItemDTO.class.getPermittedSubclasses())
                            .containsExactly(FunctionalityCatalogItemDTO.class),
                    () -> assertThat(fields)
                            .isNotEmpty()
                            .allSatisfy(field -> assertThat(Modifier.isFinal(field.getModifiers())).isTrue()),
                    () -> assertThat(setters).isEmpty());
        }
    }

    @Nested
    class ValueEquality {

        @Test
        void equals_returnsTrueAndSameHashCode_whenAllFieldsMatch() {
            CatalogItemDTO first = CatalogItemDTO.create("app-1", "Message App");
            CatalogItemDTO second = new CatalogItemDTO("app-1", "Message App");

            assertAll(
                    () -> assertThat(first).isEqualTo(second),
                    () -> assertThat(first).hasSameHashCodeAs(second));
        }

        @Test
        void equals_returnsTrue_whenValuesDifferOnlyBySurroundingWhitespace() {
            CatalogItemDTO trimmed = CatalogItemDTO.create("app-1", "Message App");
            CatalogItemDTO padded = new CatalogItemDTO("  app-1  ", "  Message App  ");

            assertThat(trimmed).isEqualTo(padded);
        }

        @Test
        void equals_returnsTrue_whenComparingTheSameInstance() {
            CatalogItemDTO dto = CatalogItemDTO.create("app-1", "Message App");

            assertThat(dto).isEqualTo(dto);
        }

        @Test
        void equals_returnsFalse_whenAnyFieldDiffersOrComparedValueIsNotACatalogItem() {
            CatalogItemDTO dto = CatalogItemDTO.create("app-1", "Message App");

            assertAll(
                    () -> assertThat(dto).isNotEqualTo(CatalogItemDTO.create("app-2", "Message App")),
                    () -> assertThat(dto).isNotEqualTo(CatalogItemDTO.create("app-1", "Other App")),
                    () -> assertThat(dto).isNotEqualTo(null),
                    () -> assertThat(dto).isNotEqualTo("app-1"));
        }
    }

    @Nested
    class JsonSerialization {

        @Test
        void toJson_usesJavaBeanPropertyNames() {
            String json = new UtilMapperJson().execute(CatalogItemDTO.create("app-1", "Message App"))
                    .orElseThrow();

            assertThat(json).contains("\"id\":\"app-1\"", "\"name\":\"Message App\"");
        }
    }
}
