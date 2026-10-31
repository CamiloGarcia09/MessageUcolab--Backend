package co.edu.uco.infraestructure.secondaryadapters.presenter.serializer.impl.text;

import co.edu.uco.application.common.catalog.CatalogPortStaticRef;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.crosscutting.exceptions.CrossWordsException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlainTextSerializerTest {

    private final PlainTextSerializer serializer = new PlainTextSerializer();

    @AfterEach
    void tearDown() {
        CatalogPortStaticRef.set(null);
    }

    @Test
    void getSupportedContentType_returnsTextPlain() {
        assertThat(serializer.getSupportedContentType()).isEqualTo("text/plain");
    }

    @Test
    void isDefault_returnsFalse() {
        assertThat(serializer.isDefault()).isFalse();
    }

    @Test
    void serialize_returnsToString() throws Exception {
        record Sample(String code) {
            @Override
            public String toString() {
                return "plain:" + code;
            }
        }

        assertThat(serializer.serialize(new Sample("CODE"))).isEqualTo("plain:CODE");
    }

    @Test
    void serialize_throwsCrossWordsException_onToStringError() {
        CatalogPort catalogPort = mock(CatalogPort.class);
        when(catalogPort.getMessage("TCH_018")).thenReturn("text error");
        CatalogPortStaticRef.set(catalogPort);

        Object broken = new Object() {
            @Override
            public String toString() {
                throw new IllegalStateException("boom");
            }
        };

        assertThatThrownBy(() -> serializer.serialize(broken))
                .isInstanceOf(CrossWordsException.class)
                .satisfies(ex -> assertThat(((CrossWordsException) ex).getTechnicalMessage()).isEqualTo("text error"));
    }
}