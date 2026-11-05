package co.edu.uco.application.usecase.domain.aggregate.entities.valueobject;

import co.edu.uco.application.common.catalog.CatalogPortStaticRef;
import co.edu.uco.application.crosscutting.exceptions.SizeTitleLessThanTenException;
import co.edu.uco.application.crosscutting.exceptions.TitleExceedsMaximumLengthException;
import co.edu.uco.application.crosscutting.exceptions.TitleCanNotBeEmptyException;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TitleVOTest {

    @BeforeEach
    void setUp() {
        CatalogPort catalogPort = mock(CatalogPort.class);
        when(catalogPort.getMessage(org.mockito.ArgumentMatchers.anyString())).thenReturn("mensaje");
        when(catalogPort.getTitle(org.mockito.ArgumentMatchers.anyString())).thenReturn("titulo");
        CatalogPortStaticRef.set(catalogPort);
    }

    @Test
    void constructor_acceptsValidTitle() {
        TitleVO vo = new TitleVO("Titulo valido");

        assertThat(vo.getTitle()).isEqualTo("Titulo valido");
    }

    @Test
    void constructor_acceptsTenCharacterTitle() {
        TitleVO vo = new TitleVO("1234567890");

        assertThat(vo.getTitle()).hasSize(10);
    }

    @Test
    void constructor_acceptsTwoHundredCharacterTitle() {
        String title = "a".repeat(200);

        TitleVO vo = new TitleVO(title);

        assertThat(vo.getTitle()).hasSize(200);
    }

    @Test
    void constructor_rejectsEmptyTitle() {
        assertThatThrownBy(() -> new TitleVO(""))
                .isInstanceOf(TitleCanNotBeEmptyException.class);
    }

    @Test
    void constructor_rejectsNullTitle() {
        assertThatThrownBy(() -> new TitleVO(null))
                .isInstanceOf(TitleCanNotBeEmptyException.class);
    }

    @Test
    void constructor_rejectsTitleShorterThanTen() {
        assertThatThrownBy(() -> new TitleVO("corta"))
                .isInstanceOf(SizeTitleLessThanTenException.class);
    }

    @Test
    void constructor_rejectsTitleLongerThanTwoHundred() {
        String title = "a".repeat(201);
        assertThatThrownBy(() -> new TitleVO(title))
                .isInstanceOf(TitleExceedsMaximumLengthException.class);
    }

    @Test
    void setTitle_updatesValueWhenValid() {
        TitleVO vo = new TitleVO("Titulo valido");
        vo.setTitle("Otro titulo");

        assertThat(vo.getTitle()).isEqualTo("Otro titulo");
    }
}
