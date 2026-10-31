package co.edu.uco.init;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class CrossWordApplicationTest {

    private TimeZone previousTimeZone;
    private String previousTimeZoneProperty;

    @BeforeEach
    void setUp() {
        previousTimeZone = TimeZone.getDefault();
        previousTimeZoneProperty = System.getProperty("user.timezone");
        TimeZone.setDefault(TimeZone.getTimeZone("America/Bogota"));
        System.setProperty("user.timezone", "America/Bogota");
    }

    @AfterEach
    void tearDown() {
        TimeZone.setDefault(previousTimeZone);
        if (previousTimeZoneProperty == null) {
            System.clearProperty("user.timezone");
        } else {
            System.setProperty("user.timezone", previousTimeZoneProperty);
        }
    }

    @Test
    void configureUtcTimeZone_setsDefaultTimeZoneAndSystemProperty() {
        CrossWordApplication.configureUtcTimeZone();

        assertAll(
                () -> assertThat(TimeZone.getDefault().getID()).isEqualTo("UTC"),
                () -> assertThat(System.getProperty("user.timezone")).isEqualTo("UTC")
        );
    }
}
