package co.edu.uco.application.secondaryports.logging;

public interface LoggingPortFactory {
    LoggingPort getLogger(Class<?> source);
}
