package co.edu.uco.application.secondaryports.presenter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("RenderedResponse")
class RenderedResponseTest {

    @Test
    @DisplayName("Expone el cuerpo y el content-type recibidos")
    void exposesBodyAndContentType() {
        RenderedResponse response = new RenderedResponse("{\"data\":[]}", "application/json");

        assertAll(
                () -> assertThat(response.body()).isEqualTo("{\"data\":[]}"),
                () -> assertThat(response.contentType()).isEqualTo("application/json"));
    }

    @Test
    @DisplayName("Es un valor inmutable comparable por contenido")
    void equalityIsValueBased() {
        RenderedResponse response = new RenderedResponse("body", "application/json");

        assertThat(response)
                .isEqualTo(new RenderedResponse("body", "application/json"))
                .hasSameHashCodeAs(new RenderedResponse("body", "application/json"))
                .isNotEqualTo(new RenderedResponse("body", "text/plain"));
    }
}
