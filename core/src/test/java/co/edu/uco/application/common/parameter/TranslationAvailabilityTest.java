package co.edu.uco.application.common.parameter;

import co.edu.uco.application.secondaryports.parameter.ParameterValuePort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TranslationAvailability")
class TranslationAvailabilityTest {

    private static final String PARAMETER = "TRADUCCION_HABILITADA";

    private static TranslationAvailability with(final ParameterValuePort port, final boolean byDefault) {
        return new TranslationAvailability(port, PARAMETER, byDefault);
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "TRUE", " si ", "sí", "1", "on", "yes", "Habilitada"})
    @DisplayName("El parámetro con un valor afirmativo habilita la traducción aunque el defecto sea apagado")
    void parameterEnables(final String value) {
        assertThat(with(name -> Optional.of(value), false).isEnabled()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"false", "FALSE", "no", "0", "off", "deshabilitada"})
    @DisplayName("El parámetro con un valor negativo apaga la traducción aunque el defecto sea encendido")
    void parameterDisables(final String value) {
        assertThat(with(name -> Optional.of(value), true).isEnabled()).isFalse();
    }

    @Test
    @DisplayName("Sin parámetro se usa el valor por defecto")
    void missingParameterUsesDefault() {
        assertThat(with(name -> Optional.empty(), true).isEnabled()).isTrue();
        assertThat(with(name -> Optional.empty(), false).isEnabled()).isFalse();
    }

    @Test
    @DisplayName("Un valor que no se reconoce usa el valor por defecto")
    void unknownValueUsesDefault() {
        assertThat(with(name -> Optional.of("tal vez"), true).isEnabled()).isTrue();
        assertThat(with(name -> Optional.of("tal vez"), false).isEnabled()).isFalse();
    }

    @Test
    @DisplayName("Si el puerto falla, se usa el valor por defecto")
    void portFailureUsesDefault() {
        assertThat(with(name -> { throw new IllegalStateException("caído"); }, true).isEnabled()).isTrue();
        assertThat(with(name -> { throw new IllegalStateException("caído"); }, false).isEnabled()).isFalse();
    }

    @Test
    @DisplayName("Consulta el parámetro por su nombre configurado")
    void asksForConfiguredName() {
        final String[] asked = new String[1];
        with(name -> { asked[0] = name; return Optional.of("true"); }, false).isEnabled();
        assertThat(asked[0]).isEqualTo(PARAMETER);
    }

    @Test
    @DisplayName("Sin nombre de parámetro no consulta y usa el valor por defecto")
    void blankNameUsesDefault() {
        final ParameterValuePort failIfCalled = name -> { throw new AssertionError("no debía consultar"); };
        assertThat(new TranslationAvailability(failIfCalled, " ", true).isEnabled()).isTrue();
        assertThat(new TranslationAvailability(failIfCalled, null, false).isEnabled()).isFalse();
    }
}
