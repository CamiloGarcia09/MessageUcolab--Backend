package co.edu.uco.infraestructure.secondaryadapters.logging;

import co.edu.uco.application.secondaryports.logging.LoggingPort;
import org.slf4j.Logger;

import static co.edu.uco.crosscutting.helpers.UtilObject.isNullObject;

final class Slf4jLoggingAdapter implements LoggingPort {

    private final Logger logger;

    Slf4jLoggingAdapter(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void debug(String message, Object... arguments) {
        if (hasArguments(arguments)) {
            logger.debug(message, arguments);
        } else {
            logger.debug(message);
        }
    }

    @Override
    public void info(String message, Object... arguments) {
        if (hasArguments(arguments)) {
            logger.info(message, arguments);
        } else {
            logger.info(message);
        }
    }

    @Override
    public void warn(String message, Object... arguments) {
        if (hasArguments(arguments)) {
            logger.warn(message, arguments);
        } else {
            logger.warn(message);
        }
    }

    @Override
    public void error(String message, Object... arguments) {
        if (hasArguments(arguments)) {
            logger.error(message, arguments);
        } else {
            logger.error(message);
        }
    }

    @Override
    public void error(String message, Throwable cause) {
        logger.error(message, cause);
    }

    private static boolean hasArguments(Object[] arguments) {
        return !isNullObject(arguments) && arguments.length > 0;
    }
}
