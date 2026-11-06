package co.edu.uco.application.usecase.domain.aggregate.entities.valueobject;

import co.edu.uco.application.common.catalog.CatalogPortStaticRef;
import co.edu.uco.application.crosscutting.exceptions.ContentCanNotBeEmptyException;
import co.edu.uco.application.crosscutting.exceptions.ContentBelowMinimumLengthException;
import co.edu.uco.application.crosscutting.exceptions.ContentExceedsMaximumLengthException;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContentVOTest {

    @BeforeEach
    void setUp() {
        CatalogPort catalogPort = mock(CatalogPort.class);
        when(catalogPort.getMessage(org.mockito.ArgumentMatchers.anyString())).thenReturn("mensaje");
        when(catalogPort.getTitle(org.mockito.ArgumentMatchers.anyString())).thenReturn("titulo");
        CatalogPortStaticRef.set(catalogPort);
    }

    @Test
    void constructor_acceptsValidContent() {
        ContentVO vo = new ContentVO("Contenido valido");

        assertThat(vo.getContent()).isEqualTo("Contenido valido");
    }

    @Test
    void constructor_acceptsFiveCharacterContent() {
        ContentVO vo = new ContentVO("12345");

        assertThat(vo.getContent()).hasSize(5);
    }

    @Test
    void constructor_acceptsTwoThousandCharacterContent() {
        String content = "a".repeat(2000);

        ContentVO vo = new ContentVO(content);

        assertThat(vo.getContent()).hasSize(2000);
    }

    @Test
    void constructor_rejectsEmptyContent() {
        assertThatThrownBy(() -> new ContentVO(""))
                .isInstanceOf(ContentCanNotBeEmptyException.class);
    }

    @Test
    void constructor_rejectsNullContent() {
        assertThatThrownBy(() -> new ContentVO(null))
                .isInstanceOf(ContentCanNotBeEmptyException.class);
    }

    @Test
    void constructor_rejectsContentShorterThanFive() {
        assertThatThrownBy(() -> new ContentVO("abcd"))
                .isInstanceOf(ContentBelowMinimumLengthException.class);
    }

    @Test
    void constructor_rejectsContentLongerThanTwoThousand() {
        assertThatThrownBy(() -> new ContentVO("a".repeat(2001)))
                .isInstanceOf(ContentExceedsMaximumLengthException.class);
    }

    @Test
    void setContent_updatesValueWhenValid() {
        ContentVO vo = new ContentVO("Contenido valido");
        vo.setContent("Otro contenido");

        assertThat(vo.getContent()).isEqualTo("Otro contenido");
    }
}
