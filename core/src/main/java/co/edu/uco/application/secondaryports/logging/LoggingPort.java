package co.edu.uco.application.secondaryports.logging;

public interface LoggingPort {
    void debug(String message, Object... arguments);
    void info(String message, Object... arguments);
    void warn(String message, Object... arguments);
    void error(String message, Object... arguments);
    void error(String message, Throwable cause);
}
