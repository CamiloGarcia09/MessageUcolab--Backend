package co.edu.uco.application.common.parameter;

import co.edu.uco.application.secondaryports.parameter.ParameterValuePort;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Decide si la traducción con IA está habilitada.
 *
 * <p>Manda el parámetro del componente de Parámetros (por ejemplo {@code TRADUCCION_HABILITADA}); así cada
 * equipo la enciende o la apaga sin reiniciar MessageUcoLab. Si el parámetro no existe, está inactivo,
 * tiene un valor que no se reconoce o el componente no responde, se usa el valor por defecto de la
 * configuración local ({@code translation.ai.enabled}).</p>
 */
public final class TranslationAvailability {

    private static final Set<String> ENABLED = Set.of("true", "si", "sí", "1", "on", "yes", "habilitada", "habilitado");
    private static final Set<String> DISABLED = Set.of("false", "no", "0", "off", "deshabilitada", "deshabilitado");

    private final ParameterValuePort parameters;
    private final String parameterName;
    private final boolean enabledByDefault;

    public TranslationAvailability(final ParameterValuePort parameters, final String parameterName,
                                   final boolean enabledByDefault) {
        this.parameters = parameters;
        this.parameterName = parameterName;
        this.enabledByDefault = enabledByDefault;
    }

    public boolean isEnabled() {
        return valueOf(readParameter());
    }

    private Optional<String> readParameter() {
        if (parameterName == null || parameterName.isBlank()) {
            return Optional.empty();
        }
        try {
            final var value = parameters.findValue(parameterName.trim());
            return value == null ? Optional.empty() : value;
        } catch (RuntimeException exception) {
            // El puerto no debería fallar; si lo hace, la traducción conserva su valor por defecto.
            return Optional.empty();
        }
    }

    private boolean valueOf(final Optional<String> parameter) {
        if (parameter.isEmpty()) {
            return enabledByDefault;
        }
        final var value = parameter.get().trim().toLowerCase(Locale.ROOT);
        if (ENABLED.contains(value)) {
            return true;
        }
        if (DISABLED.contains(value)) {
            return false;
        }
        return enabledByDefault;
    }
}
