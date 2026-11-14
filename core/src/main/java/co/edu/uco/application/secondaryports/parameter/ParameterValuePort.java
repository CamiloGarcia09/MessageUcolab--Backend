package co.edu.uco.application.secondaryports.parameter;

import java.util.Optional;

/**
 * Valores de configuración que administra el componente de Parámetros (Catálogo de Parámetros).
 *
 * <p>La implementación decide de dónde salen los valores; el núcleo solo pregunta por el nombre del
 * parámetro. Nunca lanza excepciones: si el componente no está configurado, no responde o el parámetro
 * no existe o está inactivo, devuelve vacío y quien consulta aplica su valor por defecto.</p>
 */
public interface ParameterValuePort {

    Optional<String> findValue(String parameterName);
}
