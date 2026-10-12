package co.edu.uco.application.primaryports.dto.message;

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

class MessageDTOTest {

    @Nested
    class Getters {

        @Test
        void create_returnsTrimmedValues() {
            MessageDTO dto = MessageDTO.create("  CODE  ", "  Title  ", "  Content  ", " info ", " general ", " func ");

            assertAll(
                    () -> assertThat(dto.getCode()).isEqualTo("CODE"),
                    () -> assertThat(dto.getTitle()).isEqualTo("Title"),
                    () -> assertThat(dto.getContent()).isEqualTo("Content"),
                    () -> assertThat(dto.getType()).isEqualTo("info"),
                    () -> assertThat(dto.getCategory()).isEqualTo("general"),
                    () -> assertThat(dto.getFunctionality()).isEqualTo("func"));
        }

        @Test
        void create_returnsUnchangedValues_whenNothingMustBeTrimmed() {
            MessageDTO dto = MessageDTO.create("CODE", "Title", "Content", "info", "general", "func");

            assertAll(
                    () -> assertThat(dto.getCode()).isEqualTo("CODE"),
                    () -> assertThat(dto.getTitle()).isEqualTo("Title"),
                    () -> assertThat(dto.getContent()).isEqualTo("Content"));
        }

        @Test
        void create_returnsEmptyStrings_whenValuesAreNull() {
            MessageDTO dto = MessageDTO.create(null, null, null, null, null, null);

            assertAll(
                    () -> assertThat(dto.getCode()).isEmpty(),
                    () -> assertThat(dto.getTitle()).isEmpty(),
                    () -> assertThat(dto.getContent()).isEmpty(),
                    () -> assertThat(dto.getType()).isEmpty(),
                    () -> assertThat(dto.getCategory()).isEmpty(),
                    () -> assertThat(dto.getFunctionality()).isEmpty());
        }
    }

    @Nested
    class Immutability {

        @Test
        void class_isNotARecord() {
            assertThat(MessageDTO.class.isRecord()).isFalse();
        }

        @Test
        void classAndDeclaredFields_areFinal_andClassExposesNoSetters() {
            List<Field> fields = List.of(MessageDTO.class.getDeclaredFields());
            List<String> setters = Arrays.stream(MessageDTO.class.getMethods())
                    .map(Method::getName)
                    .filter(name -> name.startsWith("set"))
                    .toList();

            assertAll(
                    () -> assertThat(Modifier.isFinal(MessageDTO.class.getModifiers())).isTrue(),
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
            MessageDTO first = MessageDTO.create("CODE", "Title", "Content", "info", "general", "func");
            MessageDTO second = MessageDTO.create("CODE", "Title", "Content", "info", "general", "func");

            assertAll(
                    () -> assertThat(first).isEqualTo(second),
                    () -> assertThat(first).hasSameHashCodeAs(second));
        }

        @Test
        void equals_returnsTrue_whenComparingTheSameInstance() {
            MessageDTO dto = MessageDTO.create("CODE", "Title", "Content", "info", "general", "func");

            assertThat(dto).isEqualTo(dto);
        }

        @Test
        void equals_returnsFalse_whenAnyFieldDiffersOrComparedValueIsNotAMessage() {
            MessageDTO dto = MessageDTO.create("CODE", "Title", "Content", "info", "general", "func");

            assertAll(
                    () -> assertThat(dto).isNotEqualTo(
                            MessageDTO.create("OTHER", "Title", "Content", "info", "general", "func")),
                    () -> assertThat(dto).isNotEqualTo(
                            MessageDTO.create("CODE", "Title", "Content", "error", "general", "func")),
                    () -> assertThat(dto).isNotEqualTo(null),
                    () -> assertThat(dto).isNotEqualTo("CODE"));
        }
    }

    @Nested
    class JsonSerialization {

        @Test
        void toJson_usesJavaBeanPropertyNames() {
            String json = new UtilMapperJson().execute(
                    MessageDTO.create("CODE", "Title", "Content", "info", "general", "func"))
                    .orElseThrow();

            assertThat(json).contains(
                    "\"code\":\"CODE\"",
                    "\"title\":\"Title\"",
                    "\"content\":\"Content\"",
                    "\"type\":\"info\"",
                    "\"category\":\"general\"",
                    "\"functionality\":\"func\"");
            assertThat(json).doesNotContain("application");
        }
    }
}
