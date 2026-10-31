package co.edu.uco.infraestructure.config;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class TimeConfigTest {

    @Test
    void utcClock_returnsClockWithUtcZone() {
        Clock clock = new TimeConfig().utcClock();

        assertThat(clock.getZone()).isEqualTo(ZoneOffset.UTC);
    }
}
