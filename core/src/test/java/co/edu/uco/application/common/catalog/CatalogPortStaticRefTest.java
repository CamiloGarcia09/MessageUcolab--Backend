package co.edu.uco.application.common.catalog;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CatalogPortStaticRefTest {

    @AfterEach
    void tearDown() {
        CatalogPortStaticRef.set(null);
    }

    @Test
    void getMessage_resolvesBootstrapContent_whenNoInstanceSet() {
        assertThat(CatalogPortStaticRef.getMessage(MessageCatalogCodeEnum.TCH_007.getCode()))
                .isEqualTo("La clave del mensaje es nula.");
    }

    @Test
    void getMessage_withDefault_resolvesBootstrapContent_whenNoInstanceSet() {
        assertThat(CatalogPortStaticRef.getMessage(MessageCatalogCodeEnum.TCH_007.getCode(), "fallback"))
                .isEqualTo("La clave del mensaje es nula.");
    }

    @Test
    void getTitle_resolvesBootstrapTitle_whenNoInstanceSet() {
        assertThat(CatalogPortStaticRef.getTitle(MessageCatalogCodeEnum.TCH_021.getCode()))
                .isEqualTo("Respuesta correcta");
    }

    @Test
    void getMessageModel_resolvesBootstrapModel_whenNoInstanceSet() {
        MessageCatalog model = CatalogPortStaticRef.getMessageModel(MessageCatalogCodeEnum.TCH_021.getCode());

        assertThat(model.code()).isEqualTo("TCH_021");
        assertThat(model.content()).isEqualTo("La respuesta correcta es: {}");
    }

    @Test
    void set_null_restoresBootstrapDefault() {
        CatalogPort catalogPort = mock(CatalogPort.class);
        when(catalogPort.getMessage(MessageCatalogCodeEnum.TCH_007.getCode())).thenReturn("custom");
        CatalogPortStaticRef.set(catalogPort);
        CatalogPortStaticRef.set(null);

        assertThat(CatalogPortStaticRef.getMessage(MessageCatalogCodeEnum.TCH_007.getCode()))
                .isEqualTo("La clave del mensaje es nula.");
    }

    @Test
    void getMessage_delegatesToSetInstance() {
        CatalogPort catalogPort = mock(CatalogPort.class);
        when(catalogPort.getMessage("KEY")).thenReturn("hello");
        CatalogPortStaticRef.set(catalogPort);

        assertThat(CatalogPortStaticRef.getMessage("KEY")).isEqualTo("hello");
    }

    @Test
    void getMessage_withDefault_delegatesToSetInstance() {
        CatalogPort catalogPort = mock(CatalogPort.class);
        when(catalogPort.getMessage("KEY", "fallback")).thenReturn("custom");
        CatalogPortStaticRef.set(catalogPort);

        assertThat(CatalogPortStaticRef.getMessage("KEY", "fallback")).isEqualTo("custom");
    }

    @Test
    void getTitle_delegatesToSetInstance() {
        CatalogPort catalogPort = mock(CatalogPort.class);
        when(catalogPort.getTitle("KEY")).thenReturn("Titulo");
        CatalogPortStaticRef.set(catalogPort);

        assertThat(CatalogPortStaticRef.getTitle("KEY")).isEqualTo("Titulo");
    }

    @Test
    void getMessageModel_delegatesToSetInstance() {
        CatalogPort catalogPort = mock(CatalogPort.class);
        MessageCatalog model = new MessageCatalog("code", "title", "content", "type", "category");
        when(catalogPort.getMessageModel("KEY")).thenReturn(model);
        CatalogPortStaticRef.set(catalogPort);

        assertThat(CatalogPortStaticRef.getMessageModel("KEY")).isSameAs(model);
    }
}
