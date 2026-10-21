package co.edu.uco.application.common.catalog;

import co.edu.uco.application.crosscutting.exceptions.MessageKeyCanNotBeEmptyException;
import co.edu.uco.application.crosscutting.exceptions.MessageKeyCanNotBeNullException;
import co.edu.uco.application.crosscutting.exceptions.MessageNotFoundException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BootstrapMessageCatalogPortTest {

    private final BootstrapMessageCatalogPort catalog = new BootstrapMessageCatalogPort();

    @Test
    void getMessageModel_returnsBootstrapModel_whenKeyExists() {
        MessageCatalog model = catalog.getMessageModel("TCH_021");

        assertThat(model.code()).isEqualTo("TCH_021");
        assertThat(model.title()).isEqualTo("Response success");
        assertThat(model.content()).isEqualTo("The successful response is: {}");
        assertThat(model.type()).isEqualTo("TECHNICAL");
        assertThat(model.category()).isEqualTo("INFORMATION");
    }

    @Test
    void getMessageModel_throws_whenKeyIsNull() {
        assertThatThrownBy(() -> catalog.getMessageModel(null))
                .isInstanceOf(MessageKeyCanNotBeNullException.class);
    }

    @Test
    void getMessageModel_throws_whenKeyIsBlank() {
        assertThatThrownBy(() -> catalog.getMessageModel(" "))
                .isInstanceOf(MessageKeyCanNotBeEmptyException.class);
    }

    @Test
    void getMessageModel_throws_whenKeyNotInBootstrapCatalog() {
        assertThatThrownBy(() -> catalog.getMessageModel("TCH_010"))
                .isInstanceOf(MessageNotFoundException.class);
    }

    @Test
    void getMessage_returnsContent_whenKeyExists() {
        assertThat(catalog.getMessage("TCH_007")).isEqualTo("The message key is null");
    }

    @Test
    void getMessage_returnsKey_whenKeyNotInBootstrapCatalog() {
        assertThat(catalog.getMessage("TCH_010")).isEqualTo("TCH_010");
    }

    @Test
    void getMessage_returnsEmpty_whenKeyIsBlank() {
        assertThat(catalog.getMessage(" ")).isEmpty();
        assertThat(catalog.getMessage(null)).isEmpty();
    }

    @Test
    void getMessage_withDefault_returnsContent_whenKeyExists() {
        assertThat(catalog.getMessage("TCH_007", "default")).isEqualTo("The message key is null");
    }

    @Test
    void getMessage_withDefault_returnsDefault_whenKeyNotInBootstrapCatalog() {
        assertThat(catalog.getMessage("TCH_010", "default")).isEqualTo("default");
    }

    @Test
    void getMessage_withDefault_returnsDefault_whenKeyIsBlank() {
        assertThat(catalog.getMessage(" ", "default")).isEqualTo("default");
        assertThat(catalog.getMessage(null, "default")).isEqualTo("default");
    }

    @Test
    void getTitle_returnsTitle_whenKeyExists() {
        assertThat(catalog.getTitle("TCH_021")).isEqualTo("Response success");
    }

    @Test
    void getTitle_returnsEmpty_whenKeyNotInBootstrapCatalog() {
        assertThat(catalog.getTitle("TCH_010")).isEmpty();
    }

    @Test
    void getTitle_returnsEmpty_whenKeyIsBlank() {
        assertThat(catalog.getTitle(" ")).isEmpty();
        assertThat(catalog.getTitle(null)).isEmpty();
    }

    @Test
    void setMessage_isNoOp() {
        catalog.setMessage("TCH_007", new MessageCatalog("c", "t", "x", "y", "z"));

        assertThat(catalog.getMessage("TCH_007")).isEqualTo("The message key is null");
    }
}
