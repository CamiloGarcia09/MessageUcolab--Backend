package co.edu.uco.application.usecase.validator.serviceidentity;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("ServiceIdentityClientIdValidator")
class ServiceIdentityClientIdValidatorTest {

    private final CatalogPort catalog = mock(CatalogPort.class);
    private final ServiceIdentityClientIdValidator validator = new ServiceIdentityClientIdValidator(catalog);

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"inventario-dev", "svc.origen_a:destino@uco", "a", "A1-b2"})
    @DisplayName("Acepta client_id con letras, números y . _ : @ -")
    void acceptsValidClientIds(String clientId) {
        assertThat(ServiceIdentityClientIdValidator.isValid(clientId)).isTrue();
        assertThat(validator.validate("  " + clientId + " ")).isEqualTo(clientId);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"-inicia-con-guion", "con espacio", "comilla'", "barra/", "llave{x}"})
    @DisplayName("Rechaza client_id con caracteres no permitidos")
    void rejectsInvalidClientIds(String clientId) {
        when(catalog.getMessage("FUN_234")).thenReturn("Client ID inválido");

        assertThat(ServiceIdentityClientIdValidator.isValid(clientId)).isFalse();
        assertThatThrownBy(() -> validator.validate(clientId))
                .isInstanceOf(BusinessRuleException.class).hasFieldOrPropertyWithValue("userMessage", "Client ID inválido");
    }

    @Test
    @DisplayName("Rechaza un client_id de más de 255 caracteres")
    void rejectsTooLongClientId() {
        assertThat(ServiceIdentityClientIdValidator.isValid("a".repeat(255))).isTrue();
        assertThat(ServiceIdentityClientIdValidator.isValid("a".repeat(256))).isFalse();
    }

    @Test
    @DisplayName("Exige el client_id")
    void requiresClientId() {
        when(catalog.getMessage("FUN_233")).thenReturn("Client ID obligatorio");

        assertThat(ServiceIdentityClientIdValidator.isValid(null)).isFalse();
        assertThatThrownBy(() -> validator.validate(null)).hasFieldOrPropertyWithValue("userMessage", "Client ID obligatorio");
        assertThatThrownBy(() -> validator.validate("   ")).hasFieldOrPropertyWithValue("userMessage", "Client ID obligatorio");
    }
}
