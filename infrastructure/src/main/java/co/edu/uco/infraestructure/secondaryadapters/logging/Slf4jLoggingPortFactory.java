package co.edu.uco.infraestructure.secondaryadapters.logging;

import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class Slf4jLoggingPortFactory implements LoggingPortFactory {

    @Override
    public LoggingPort getLogger(Class<?> source) {

        return new Slf4jLoggingAdapter(LoggerFactory.getLogger(source));
    }
}
