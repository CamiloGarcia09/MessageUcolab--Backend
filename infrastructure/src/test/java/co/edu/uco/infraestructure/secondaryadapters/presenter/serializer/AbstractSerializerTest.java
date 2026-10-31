package co.edu.uco.infraestructure.secondaryadapters.presenter.serializer;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractSerializerTest {

    private static class TestSerializer extends AbstractSerializer {
        private final boolean defaultValue;

        TestSerializer(String contentType) {
            this(contentType, false);
        }

        TestSerializer(String contentType, boolean defaultValue) {
            super(contentType);
            this.defaultValue = defaultValue;
        }

        @Override
        public boolean isDefault() {
            return defaultValue;
        }

        @Override
        public <T> String serialize(T data) {
            return "serialized";
        }
    }

    @Test
    void getSupportedContentType_returnsConfiguredType() {
        AbstractSerializer serializer = new TestSerializer("application/test");

        assertThat(serializer.getSupportedContentType()).isEqualTo("application/test");
    }

    @Test
    void supports_returnsTrueForExactMatchIgnoringCase() {
        AbstractSerializer serializer = new TestSerializer("application/json");

        assertThat(serializer.supports("application/json")).isTrue();
        assertThat(serializer.supports("Application/JSON")).isTrue();
    }

    @Test
    void supports_returnsTrueForWildcardOnlyWhenDefault() {
        AbstractSerializer nonDefault = new TestSerializer("application/json");
        AbstractSerializer defaultValue = new TestSerializer("application/json", true);

        assertThat(nonDefault.supports("*/*")).isFalse();
        assertThat(defaultValue.supports("*/*")).isTrue();
    }

    @Test
    void supports_returnsFalseForUnsupportedMediaType() {
        AbstractSerializer serializer = new TestSerializer("application/json");

        assertThat(serializer.supports("text/html")).isFalse();
    }

    @Test
    void isDefault_returnsFalseByDefault() {
        AbstractSerializer serializer = new TestSerializer("application/json");

        assertThat(serializer.isDefault()).isFalse();
    }
}