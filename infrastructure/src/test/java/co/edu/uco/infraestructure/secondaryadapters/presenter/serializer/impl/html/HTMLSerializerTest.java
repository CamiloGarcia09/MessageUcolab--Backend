package co.edu.uco.infraestructure.secondaryadapters.presenter.serializer.impl.html;

import co.edu.uco.application.common.catalog.CatalogPortStaticRef;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.crosscutting.exceptions.CrossWordsException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HTMLSerializerTest {

    private final HTMLSerializer serializer = new HTMLSerializer();

    @AfterEach
    void tearDown() {
        CatalogPortStaticRef.set(null);
    }

    @Test
    void getSupportedContentType_returnsTextHtml() {
        assertThat(serializer.getSupportedContentType()).isEqualTo("text/html");
    }

    @Test
    void isDefault_returnsFalse() {
        assertThat(serializer.isDefault()).isFalse();
    }

    @Test
    void serialize_wrapsJsonInHtmlStructure() {
        record Sample(String code) {
        }

        String html = serializer.serialize(new Sample("CODE"));

        assertThat(html)
                .startsWith("<html><body><pre>")
                .endsWith("</pre></body></html>")
                .contains("\"code\" : \"CODE\"");
    }

    @Test
    void serialize_throwsCrossWordsException_onSerializationError() {
        CatalogPort catalogPort = mock(CatalogPort.class);
        when(catalogPort.getMessage("TCH_018")).thenReturn("html error");
        CatalogPortStaticRef.set(catalogPort);

        Object cyclic = new Object() {
            @SuppressWarnings("unused")
            public Object self = this;
        };

        assertThatThrownBy(() -> serializer.serialize(cyclic))
                .isInstanceOf(CrossWordsException.class)
                .satisfies(ex -> assertThat(((CrossWordsException) ex).getTechnicalMessage()).isEqualTo("html error"));
    }
}