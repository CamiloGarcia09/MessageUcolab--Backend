package co.edu.uco.crosscutting.helpers;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import co.edu.uco.crosscutting.exceptions.BusinessException;
import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionLocation;
import static co.edu.uco.crosscutting.catalog.BootstrapMessageCatalogEnum.*;

import static co.edu.uco.crosscutting.helpers.UtilText.isEmptyOrNull;

public final class PropertiesHelper {

    private PropertiesHelper() {
    }

    public static String getValue(final String propertiesFileName, final String key) {
        if (isEmptyOrNull(propertiesFileName)) {
            throw new IllegalArgumentException(TCH_080.getContent());
        }

        if (isEmptyOrNull(key)) {
            throw new IllegalArgumentException(TCH_081.getContent().formatted(propertiesFileName));
        }

        try (InputStream inputStream = PropertiesHelper.class.getClassLoader()
                .getResourceAsStream(propertiesFileName)) {

            if (inputStream == null) {
                throw BusinessException.buildTechnicalException(TCH_082.getContent().formatted(propertiesFileName));
            }

            var properties = new Properties();
            properties.load(inputStream);
            var value = properties.getProperty(key);

            if (isEmptyOrNull(value)) {
                throw BusinessException.buildTechnicalException(TCH_083.getContent().formatted(key, propertiesFileName));
            }

            return value;

        } catch (final RuntimeException exception) {
            throw exception;
        } catch (final IOException exception) {
            throw BusinessException.buildTechnicalException(TCH_084.getContent().formatted(propertiesFileName),
                    exception, ExceptionLocation.GENERAL);
        }
    }
}
