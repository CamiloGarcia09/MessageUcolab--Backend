package co.edu.uco.infraestructure.secondaryadapters.presenter.serializer.impl.xml;

import co.edu.uco.application.common.catalog.CatalogPortStaticRef;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.crosscutting.exceptions.CrossWordsException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class XMLSerializerTest {

    private final XMLSerializer serializer = new XMLSerializer();

    @AfterEach
    void tearDown() {
        CatalogPortStaticRef.set(null);
    }

    @Test
    void getSupportedContentType_returnsApplicationXml() {
        assertThat(serializer.getSupportedContentType()).isEqualTo("application/xml");
    }

    @Test
    void isDefault_returnsFalse() {
        assertThat(serializer.isDefault()).isFalse();
    }

    @Test
    void serialize_serializesDataToXml() {
        record Sample(String code) {
        }

        String xml = serializer.serialize(new Sample("CODE"));

        assertThat(xml).contains("<code>CODE</code>");
    }

    @Test
    void serialize_throwsCrossWordsException_onSerializationError() {
        CatalogPort catalogPort = mock(CatalogPort.class);
        when(catalogPort.getMessage("TCH_018")).thenReturn("xml error");
        CatalogPortStaticRef.set(catalogPort);

        Object cyclic = new Object() {
            @SuppressWarnings("unused")
            public Object self = this;
        };

        assertThatThrownBy(() -> serializer.serialize(cyclic))
                .isInstanceOf(CrossWordsException.class)
                .satisfies(ex -> assertThat(((CrossWordsException) ex).getTechnicalMessage()).isEqualTo("xml error"));
    }
}