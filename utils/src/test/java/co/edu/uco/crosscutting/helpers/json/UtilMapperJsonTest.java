package co.edu.uco.crosscutting.helpers.json;

import co.edu.uco.crosscutting.exceptions.CrossWordsException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UtilMapperJsonTest {

    private final UtilMapperJson mapper = new UtilMapperJson();

    public static class SampleData {
        private String name;
        private int age;

        public SampleData() {}

        public SampleData(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
    }

    @Test
    void execute_serializesObjectToJson() {
        Optional<String> result = mapper.execute(new SampleData("test", 30));
        assertThat(result).isPresent();
        assertThat(result.get()).contains("\"name\":\"test\"", "\"age\":30");
    }

    @Test
    void execute_omitsNullFields_dueToNonNullInclusion() {
        Optional<String> result = mapper.execute(new SampleData(null, 30));
        assertThat(result).isPresent();
        assertThat(result.get()).doesNotContain("name");
    }

    @Test
    void execute_serializesNullAsLiteralNull() {
        Optional<String> result = mapper.execute(null);
        assertThat(result).isPresent();
        assertThat(result.get()).contains("null");
    }

    @Test
    void execute_listDeserializesJsonToList() throws Exception {
        String json = "[{\"name\":\"a\",\"age\":1},{\"name\":\"b\",\"age\":2}]";
        List<SampleData> result = mapper.execute(json, new TypeReference<List<SampleData>>() {});
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("a");
        assertThat(result.get(1).getAge()).isEqualTo(2);
    }

    @Test
    void execute_listDeserialization_throwsCrossWordsException_forInvalidJson() {
        assertThrows(CrossWordsException.class,
                () -> mapper.execute("not-json", new TypeReference<List<SampleData>>() {}));
    }

    @Test
    void execute_singleObjectDeserializesJsonToClass() {
        String json = "{\"name\":\"test\",\"age\":30}";
        Optional<SampleData> result = mapper.execute(json, SampleData.class);
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("test");
        assertThat(result.get().getAge()).isEqualTo(30);
    }

    @Test
    void execute_singleObjectReturnsEmptyOptional_forInvalidJson() {
        Optional<SampleData> result = mapper.execute("not-json", SampleData.class);
        assertThat(result).isEmpty();
    }

    @Test
    void execute_singleObjectIgnoresUnknownProperties() {
        String json = "{\"name\":\"test\",\"age\":30,\"unknown\":123}";
        Optional<SampleData> result = mapper.execute(json, SampleData.class);
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("test");
    }

    @Test
    void executeGson_serializesObjectWithNullFields() {
        Optional<String> result = mapper.executeGson(new SampleData(null, 30));
        assertThat(result).isPresent();
        assertThat(result.get()).contains("\"name\":null");
    }

    @Test
    void executeGson_serializesLocalDateTime() {
        Map<String, LocalDateTime> value = Map.of("date", LocalDateTime.of(2023, 6, 15, 10, 30, 0));
        Optional<String> result = mapper.executeGson(value);
        assertThat(result).isPresent();
        assertThat(result.get()).contains("2023-06-15T10:30:00");
    }
}