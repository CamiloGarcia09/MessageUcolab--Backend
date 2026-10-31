package co.edu.uco.infraestructure.secondaryadapters.logging;

import co.edu.uco.application.secondaryports.logging.LoggingPort;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Slf4jLoggingPortFactoryTest {

    @Test
    void getLogger_returns_non_null_port() {
        var factory = new Slf4jLoggingPortFactory();

        LoggingPort port = factory.getLogger(Slf4jLoggingPortFactoryTest.class);

        assertThat(port).isNotNull();
    }

    @Test
    void getLogger_returns_distinct_instances_per_class() {
        var factory = new Slf4jLoggingPortFactory();

        LoggingPort portA = factory.getLogger(String.class);
        LoggingPort portB = factory.getLogger(Integer.class);

        assertThat(portA).isNotSameAs(portB);
    }

    @Test
    void getLogger_port_can_log_without_exception() {
        var factory = new Slf4jLoggingPortFactory();
        LoggingPort port = factory.getLogger(Slf4jLoggingPortFactoryTest.class);

        port.info("info message");
        port.warn("warn message");
        port.error("error message");
        port.error("error with cause", new RuntimeException("test"));
    }
}
