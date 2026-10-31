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

class TranslatedMessageDTOTest {

    @Nested
    class Getters {

        @Test
        void create_returnsTrimmedValues_andMarksDynamicTranslation() {
            TranslatedMessageDTO dto = TranslatedMessageDTO.create(
                    "  CODE  ", "  es  ", "  en  ", "  OrigTitle  ", "  OrigContent  ",
                    "  TransTitle  ", "  TransContent  ", " original info ", " translated info ",
                    " original general ", " translated general ", " original func ", " translated func ",
                    " ollama ", " llama3.2 ", 120L);

            assertAll(
                    () -> assertThat(dto.getCode()).isEqualTo("CODE"),
                    () -> assertThat(dto.getSourceLanguage()).isEqualTo("es"),
                    () -> assertThat(dto.getTargetLanguage()).isEqualTo("en"),
                    () -> assertThat(dto.getOriginalTitle()).isEqualTo("OrigTitle"),
                    () -> assertThat(dto.getOriginalContent()).isEqualTo("OrigContent"),
                    () -> assertThat(dto.getTranslatedTitle()).isEqualTo("TransTitle"),
                    () -> assertThat(dto.getTranslatedContent()).isEqualTo("TransContent"),
                    () -> assertThat(dto.getOriginalType()).isEqualTo("original info"),
                    () -> assertThat(dto.getTranslatedType()).isEqualTo("translated info"),
                    () -> assertThat(dto.getOriginalCategory()).isEqualTo("original general"),
                    () -> assertThat(dto.getTranslatedCategory()).isEqualTo("translated general"),
                    () -> assertThat(dto.getOriginalFunctionality()).isEqualTo("original func"),
                    () -> assertThat(dto.getTranslatedFunctionality()).isEqualTo("translated func"),
                    () -> assertThat(dto.getTranslationProvider()).isEqualTo("ollama"),
                    () -> assertThat(dto.getTranslationModel()).isEqualTo("llama3.2"),
                    () -> assertThat(dto.getTranslationElapsedMs()).isEqualTo(120L),
                    () -> assertThat(dto.isDynamicTranslation()).isTrue());
        }

        @Test
        void canonicalConstructor_keepsDynamicTranslationFlagAndElapsedTime() {
            TranslatedMessageDTO dto = new TranslatedMessageDTO(
                    "CODE", "es", "en", "T", "C", "TT", "TC", "ot", "tt", "oc", "tc", "of", "tf",
                    "ollama", "m", 10L, false);

            assertAll(
                    () -> assertThat(dto.isDynamicTranslation()).isFalse(),
                    () -> assertThat(dto.getTranslationElapsedMs()).isEqualTo(10L));
        }
    }

    @Nested
    class Immutability {

        @Test
        void class_isNotARecord() {
            assertThat(TranslatedMessageDTO.class.isRecord()).isFalse();
        }

        @Test
        void classAndDeclaredFields_areFinal_andClassExposesNoSetters() {
            List<Field> fields = List.of(TranslatedMessageDTO.class.getDeclaredFields());
            List<String> setters = Arrays.stream(TranslatedMessageDTO.class.getMethods())
                    .map(Method::getName)
                    .filter(name -> name.startsWith("set"))
                    .toList();

            assertAll(
                    () -> assertThat(Modifier.isFinal(TranslatedMessageDTO.class.getModifiers())).isTrue(),
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
            TranslatedMessageDTO first = TranslatedMessageDTO.create(
                    "CODE", "es", "en", "T", "C", "TT", "TC", "ot", "tt", "oc", "tc", "of", "tf",
                    "ollama", "m", 10L);
            TranslatedMessageDTO second = new TranslatedMessageDTO(
                    "CODE", "es", "en", "T", "C", "TT", "TC", "ot", "tt", "oc", "tc", "of", "tf",
                    "ollama", "m", 10L, true);

            assertAll(
                    () -> assertThat(first).isEqualTo(second),
                    () -> assertThat(first).hasSameHashCodeAs(second));
        }

        @Test
        void equals_returnsTrue_whenComparingTheSameInstance() {
            TranslatedMessageDTO dto = TranslatedMessageDTO.create(
                    "CODE", "es", "en", "T", "C", "TT", "TC", "ot", "tt", "oc", "tc", "of", "tf",
                    "ollama", "m", 10L);

            assertThat(dto).isEqualTo(dto);
        }

        @Test
        void equals_returnsFalse_whenTheElapsedTimeDiffersWithEqualTextFields() {
            TranslatedMessageDTO dto = TranslatedMessageDTO.create(
                    "CODE", "es", "en", "T", "C", "TT", "TC", "ot", "tt", "oc", "tc", "of", "tf",
                    "ollama", "m", 10L);
            TranslatedMessageDTO other = new TranslatedMessageDTO(
                    "CODE", "es", "en", "T", "C", "TT", "TC", "ot", "tt", "oc", "tc", "of", "tf",
                    "ollama", "m", 11L, true);

            assertThat(dto).isNotEqualTo(other);
        }

        @Test
        void equals_returnsFalse_whenAnyFieldDiffersOrComparedValueIsNotATranslatedMessage() {
            TranslatedMessageDTO dto = TranslatedMessageDTO.create(
                    "CODE", "es", "en", "T", "C", "TT", "TC", "ot", "tt", "oc", "tc", "of", "tf",
                    "ollama", "m", 10L);

            assertAll(
                    () -> assertThat(dto).isNotEqualTo(TranslatedMessageDTO.create(
                            "OTHER", "es", "en", "T", "C", "TT", "TC", "ot", "tt", "oc", "tc", "of", "tf",
                            "ollama", "m", 10L)),
                    () -> assertThat(dto).isNotEqualTo(new TranslatedMessageDTO(
                            "CODE", "es", "en", "T", "C", "TT", "TC", "ot", "tt", "oc", "tc", "of", "tf",
                            "ollama", "m", 10L, false)),
                    () -> assertThat(dto).isNotEqualTo(null),
                    () -> assertThat(dto).isNotEqualTo("CODE"));
        }
    }

    @Nested
    class JsonSerialization {

        @Test
        void toJson_usesJavaBeanPropertyNames_andExposesDynamicTranslationBoolean() {
            TranslatedMessageDTO dynamic = TranslatedMessageDTO.create(
                    "CODE", "es", "en", "T", "C", "TT", "TC", "ot", "tt", "oc", "tc", "of", "tf",
                    "ollama", "m", 120L);
            TranslatedMessageDTO cached = new TranslatedMessageDTO(
                    "CODE", "es", "en", "T", "C", "TT", "TC", "ot", "tt", "oc", "tc", "of", "tf",
                    "ollama", "m", 120L, false);

            String dynamicJson = new UtilMapperJson().execute(dynamic).orElseThrow();
            String cachedJson = new UtilMapperJson().execute(cached).orElseThrow();

            assertAll(
                    () -> assertThat(dynamicJson).contains(
                            "\"code\":\"CODE\"",
                            "\"sourceLanguage\":\"es\"",
                            "\"targetLanguage\":\"en\"",
                            "\"originalTitle\":\"T\"",
                            "\"originalContent\":\"C\"",
                            "\"translatedTitle\":\"TT\"",
                            "\"translatedContent\":\"TC\"",
                            "\"originalType\":\"ot\"",
                            "\"translatedType\":\"tt\"",
                            "\"originalCategory\":\"oc\"",
                            "\"translatedCategory\":\"tc\"",
                            "\"originalFunctionality\":\"of\"",
                            "\"translatedFunctionality\":\"tf\"",
                            "\"translationProvider\":\"ollama\"",
                            "\"translationModel\":\"m\"",
                            "\"translationElapsedMs\":120",
                            "\"dynamicTranslation\":true"),
                    () -> assertThat(cachedJson).contains("\"dynamicTranslation\":false"));
        }
    }
}
