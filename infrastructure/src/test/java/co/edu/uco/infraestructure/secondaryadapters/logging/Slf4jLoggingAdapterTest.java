package co.edu.uco.infraestructure.secondaryadapters.logging;

import co.edu.uco.application.secondaryports.logging.LoggingPort;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class Slf4jLoggingAdapterTest {

    @Test
    void info_delegates_to_slf4j_logger() {
        Logger mockLogger = mock(Logger.class);
        LoggingPort adapter = new Slf4jLoggingAdapter(mockLogger);

        adapter.info("test info message");

        verify(mockLogger).info("test info message");
    }

    @Test
    void warn_delegates_to_slf4j_logger() {
        Logger mockLogger = mock(Logger.class);
        LoggingPort adapter = new Slf4jLoggingAdapter(mockLogger);

        adapter.warn("test warn message");

        verify(mockLogger).warn("test warn message");
    }

    @Test
    void error_without_cause_delegates_to_slf4j_logger() {
        Logger mockLogger = mock(Logger.class);
        LoggingPort adapter = new Slf4jLoggingAdapter(mockLogger);

        adapter.error("test error message");

        verify(mockLogger).error("test error message");
    }

    @Test
    void error_with_cause_delegates_to_slf4j_logger() {
        Logger mockLogger = mock(Logger.class);
        LoggingPort adapter = new Slf4jLoggingAdapter(mockLogger);
        Throwable cause = new RuntimeException("cause");

        adapter.error("test error message", cause);

        verify(mockLogger).error("test error message", cause);
    }

    @Test
    void debug_with_arguments_delegates_to_slf4j_logger() {
        Logger mockLogger = mock(Logger.class);
        LoggingPort adapter = new Slf4jLoggingAdapter(mockLogger);

        adapter.debug("debug msg {} {}", "arg1", "arg2");

        verify(mockLogger).debug("debug msg {} {}", new Object[]{"arg1", "arg2"});
    }

    @Test
    void info_with_arguments_delegates_to_slf4j_logger() {
        Logger mockLogger = mock(Logger.class);
        LoggingPort adapter = new Slf4jLoggingAdapter(mockLogger);

        adapter.info("info msg {}", "arg1");

        verify(mockLogger).info("info msg {}", new Object[]{"arg1"});
    }

    @Test
    void warn_with_arguments_delegates_to_slf4j_logger() {
        Logger mockLogger = mock(Logger.class);
        LoggingPort adapter = new Slf4jLoggingAdapter(mockLogger);

        adapter.warn("warn msg {}", "arg1");

        verify(mockLogger).warn("warn msg {}", new Object[]{"arg1"});
    }

    @Test
    void error_with_arguments_delegates_to_slf4j_logger() {
        Logger mockLogger = mock(Logger.class);
        LoggingPort adapter = new Slf4jLoggingAdapter(mockLogger);

        adapter.error("error msg {}", "arg1");

        verify(mockLogger).error("error msg {}", new Object[]{"arg1"});
    }
}
