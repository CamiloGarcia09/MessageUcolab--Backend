package co.edu.uco.infraestructure.secondaryadapters.parameter.dummy;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.parameter.CatalogParameterPort;
import co.edu.uco.crosscutting.helpers.PropertiesHelper;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.CrossWordsException;
import org.springframework.stereotype.Component;

import static co.edu.uco.infraestructure.config.InfrastructureConstant.PARAMETER_PROPERTIES_FILE;
import static co.edu.uco.crosscutting.helpers.UtilText.isEmptyOrNull;
import static co.edu.uco.crosscutting.helpers.UtilText.trim;

@Component
public class CatalogParameterAdapter implements CatalogParameterPort {

    private final LoggingPort log;
    private final CatalogPort catalogPort;

    public CatalogParameterAdapter(LoggingPortFactory loggerFactory, CatalogPort catalogPort) {
        this.log = loggerFactory.getLogger(CatalogParameterAdapter.class);
        this.catalogPort = catalogPort;
    }

    @Override
    public String consultarParametro(String codigo) {
        if (isEmptyOrNull(codigo)) {
            throw CrossWordsException.buildInfrastructure(
                    catalogPort.getMessage(MessageCatalogCodeEnum.TCH_068.getCode()),
                    catalogPort.getMessage(MessageCatalogCodeEnum.TCH_068.getCode()),
                    co.edu.uco.crosscutting.exceptions.enumeration.ExceptionType.BUSINESS);
        }

        String trimmedCodigo = trim(codigo);

        try {
            String value = PropertiesHelper.getValue(PARAMETER_PROPERTIES_FILE, trimmedCodigo);

            if (isEmptyOrNull(value)) {
                var message = catalogPort.getMessage(MessageCatalogCodeEnum.TCH_071.getCode()).formatted(trimmedCodigo);
                log.error(message);
                throw CrossWordsException.buildInfrastructure(message, message,
                        co.edu.uco.crosscutting.exceptions.enumeration.ExceptionType.TECHNICAL);
            }

            return value;

        } catch (CrossWordsException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            var message = catalogPort.getMessage(MessageCatalogCodeEnum.TCH_068.getCode()).formatted(trimmedCodigo);
            log.error(message, ex);
            throw CrossWordsException.buildInfrastructure(message, ex);
        } catch (Exception ex) {
            var message = catalogPort.getMessage(MessageCatalogCodeEnum.TCH_067.getCode()).formatted(trimmedCodigo);
            log.error(message, ex);
            throw CrossWordsException.buildInfrastructure(message, ex);
        }
    }
}
