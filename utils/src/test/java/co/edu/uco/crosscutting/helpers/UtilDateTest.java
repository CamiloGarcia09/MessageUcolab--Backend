package co.edu.uco.crosscutting.helpers;

import co.edu.uco.crosscutting.exceptions.CrossWordsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UtilDateTest {

    @Test
    void isNull_returnsTrue_whenDateIsNull() {
        assertThat(UtilDate.isNull(null)).isTrue();
    }

    @Test
    void isNull_returnsFalse_whenDateIsNotNull() {
        assertThat(UtilDate.isNull(new Date())).isFalse();
    }

    @Test
    void getDefaultIsNull_returnsDefault_whenValueIsNull() {
        assertThat(UtilDate.getDefaultIsNull(null)).isNotNull();
    }

    @Test
    void getDefaultIsNull_returnsSameValue_whenNotNull() {
        Date value = new Date(123456789L);
        assertThat(UtilDate.getDefaultIsNull(value)).isEqualTo(value);
    }

    @Test
    void getDefaultTime_returnsDefault_whenValueIsNull() {
        LocalDateTime defaultValue = LocalDateTime.of(2020, 1, 1, 10, 0);
        assertThat(UtilDate.getDefaultTime(null, defaultValue)).isEqualTo(defaultValue);
    }

    @Test
    void getDefaultTime_returnsValue_whenNotNull() {
        LocalDateTime value = LocalDateTime.of(2021, 5, 5, 10, 0);
        assertThat(UtilDate.getDefaultTime(value, LocalDateTime.MIN)).isEqualTo(value);
    }

    @Test
    void getDefaultTimeIfNull_returnsTime_whenValueIsNull() {
        LocalDateTime before = LocalDateTime.now(ZoneOffset.UTC);

        LocalDateTime result = UtilDate.getDefaultTimeIfNull(null);

        LocalDateTime after = LocalDateTime.now(ZoneOffset.UTC);
        assertThat(result).isBetween(before, after);
    }

    @Test
    void nowUtc_usesUtcInstantFromInjectedClock() {
        Clock clock = Clock.fixed(Instant.parse("2025-01-15T15:30:45Z"), ZoneOffset.ofHours(-5));

        LocalDateTime result = UtilDate.nowUtc(clock);

        assertThat(result).isEqualTo(LocalDateTime.of(2025, 1, 15, 15, 30, 45));
    }

    @Test
    void nowUtc_returnsCurrentUtcTime() {
        LocalDateTime before = LocalDateTime.now(ZoneOffset.UTC);

        LocalDateTime result = UtilDate.nowUtc();

        assertThat(result).isBetween(before, LocalDateTime.now(ZoneOffset.UTC));
    }

    @Test
    void isBetween_returnsTrue_whenDateIsStrictlyBetween() {
        Date init = new Date(1000L);
        Date end = new Date(3000L);
        Date middle = new Date(2000L);
        assertThat(UtilDate.isBetween(middle, init, end)).isTrue();
    }

    @Test
    void isBetween_returnsFalse_whenDateEqualsInit() {
        Date init = new Date(1000L);
        assertThat(UtilDate.isBetween(init, init, new Date(3000L))).isFalse();
    }

    @Test
    void isBetween_returnsFalse_whenDateEqualsEnd() {
        Date end = new Date(3000L);
        assertThat(UtilDate.isBetween(end, new Date(1000L), end)).isFalse();
    }

    @Test
    void isBetween_returnsFalse_whenOutside() {
        assertThat(UtilDate.isBetween(new Date(0L), new Date(1000L), new Date(3000L))).isFalse();
    }

    @Test
    void isBefore_returnsTrue_whenCompareIsBefore() {
        assertThat(UtilDate.isBefore(new Date(1000L), new Date(2000L))).isTrue();
    }

    @Test
    void isBefore_returnsFalse_whenCompareIsAfter() {
        assertThat(UtilDate.isBefore(new Date(2000L), new Date(1000L))).isFalse();
    }

    @Test
    void isBetweenIncludingInit_returnsTrue_whenDateEqualsInit() {
        Date init = new Date(1000L);
        assertThat(UtilDate.isBetweenIncludingInit(init, init, new Date(3000L))).isTrue();
    }

    @Test
    void isBetweenIncludingEnd_returnsTrue_whenDateEqualsEnd() {
        Date end = new Date(3000L);
        assertThat(UtilDate.isBetweenIncludingEnd(end, new Date(1000L), end)).isTrue();
    }

    @Test
    void isBetweenIncludingRanges_returnsTrue_onBothBoundaries() {
        Date init = new Date(1000L);
        Date end = new Date(3000L);
        assertThat(UtilDate.isBetweenIncludingRanges(init, init, end)).isTrue();
        assertThat(UtilDate.isBetweenIncludingRanges(end, init, end)).isTrue();
        assertThat(UtilDate.isBetweenIncludingRanges(new Date(2000L), init, end)).isTrue();
    }

    @Test
    void isBetweenIncludingRanges_returnsFalse_outside() {
        assertThat(UtilDate.isBetweenIncludingRanges(new Date(0L), new Date(1000L), new Date(3000L))).isFalse();
    }

    @Test
    void currentDate_returnsToday() {
        assertThat(UtilDate.currentDate()).isEqualTo(LocalDate.now(ZoneOffset.UTC));
    }

    @Test
    void getLocalDateADate_convertsCorrectly() {
        LocalDate localDate = LocalDate.of(2023, 6, 15);
        Date converted = UtilDate.getLocalDateADate(localDate);
        assertThat(converted.toInstant()).isEqualTo(Instant.parse("2023-06-15T00:00:00Z"));
    }

    @Test
    void getDateALocalDate_convertsCorrectly() {
        LocalDate localDate = LocalDate.of(2023, 6, 15);
        Date date = Date.from(Instant.parse("2023-06-15T23:30:00Z"));
        assertThat(UtilDate.getDateALocalDate(date)).isEqualTo(localDate);
    }

    @Test
    void getLocalDataTimeADate_convertsCorrectly() {
        LocalDateTime dateTime = LocalDateTime.of(2023, 6, 15, 10, 30);
        Date converted = UtilDate.getLocalDataTimeADate(dateTime);
        assertThat(converted.toInstant()).isEqualTo(Instant.parse("2023-06-15T10:30:00Z"));
        assertThat(UtilDate.getDateALocalDateTime(converted)).isEqualTo(dateTime);
    }

    @Test
    void parseDate_parsesValidDate() {
        LocalDateTime result = UtilDate.parseDate("2023-06-15T10:30:00");
        assertThat(result).isEqualTo(LocalDateTime.of(2023, 6, 15, 10, 30, 0));
    }

    @Test
    void parseDate_parsesDateWithFractionalSeconds() {
        LocalDateTime result = UtilDate.parseDate("2023-06-15T10:30:00.123");
        assertThat(result.getNano()).isEqualTo(123_000_000);
    }

    @ParameterizedTest
    @CsvSource({
            "2023-06-15T10:30:00Z, 2023-06-15T10:30:00",
            "2023-06-15T10:30:00-05:00, 2023-06-15T15:30:00"
    })
    void parseDate_normalizesOffsetDateTimeToUtc(String input, String expected) {
        assertThat(UtilDate.parseDate(input)).isEqualTo(LocalDateTime.parse(expected));
    }

    @Test
    void parseDate_throwsCrossWordsException_forInvalidDate() {
        assertThatThrownBy(() -> UtilDate.parseDate("not-a-date"))
                .isInstanceOf(CrossWordsException.class)
                .satisfies(ex -> assertThat(((CrossWordsException) ex).getTechnicalMessage())
                        .isEqualTo("La fecha que se convertirá no tiene un formato válido."));
    }

    @Test
    void parseDate_withCustomPattern_parsesCorrectly() {
        LocalDateTime result = UtilDate.parseDate("15/06/2023 10:30", "dd/MM/yyyy HH:mm");
        assertThat(result).isEqualTo(LocalDateTime.of(2023, 6, 15, 10, 30));
    }
}
