package co.edu.uco.infraestructure.secondaryadapters.presenter.serializer.impl.yaml;

import co.edu.uco.application.common.catalog.CatalogPortStaticRef;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.crosscutting.exceptions.CrossWordsException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class YamlSerializerTest {

    private final YamlSerializer serializer = new YamlSerializer();

    @AfterEach
    void tearDown() {
        CatalogPortStaticRef.set(null);
    }

    @Test
    void getSupportedContentType_returnsApplicationYaml() {
        assertThat(serializer.getSupportedContentType()).isEqualTo("application/yaml");
    }

    @Test
    void isDefault_returnsFalse() {
        assertThat(serializer.isDefault()).isFalse();
    }

    @Test
    void serialize_serializesDataToYaml() throws Exception {
        record Sample(String code, String title) {
        }

        String yaml = serializer.serialize(new Sample("CODE", "Titulo"));

        assertThat(yaml)
                .contains("code: \"CODE\"")
                .contains("title: \"Titulo\"");
    }

    @Test
    void serialize_throwsCrossWordsException_onSerializationError() {
        CatalogPort catalogPort = mock(CatalogPort.class);
        when(catalogPort.getMessage("TCH_018")).thenReturn("yaml error");
        CatalogPortStaticRef.set(catalogPort);

        Object cyclic = new Object() {
            @SuppressWarnings("unused")
            public Object self = this;
        };

        assertThatThrownBy(() -> serializer.serialize(cyclic))
                .isInstanceOf(CrossWordsException.class)
                .satisfies(ex -> assertThat(((CrossWordsException) ex).getTechnicalMessage()).isEqualTo("yaml error"));
    }
}