package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class SurrealQLUtilTest {

    @Test
    void quote_returnsNone_whenValueIsNull() {
        assertThat(SurrealQLUtil.quote(null)).isEqualTo("NONE");
    }

    @Test
    void quote_returnsSingleQuotedValue() {
        assertThat(SurrealQLUtil.quote("hello")).isEqualTo("'hello'");
    }

    @Test
    void quote_escapesSingleQuotes() {
        assertThat(SurrealQLUtil.quote("O'Brien")).isEqualTo("'O\\'Brien'");
    }

    @Test
    void quote_escapesBackslashes() {
        assertThat(SurrealQLUtil.quote("a\\b")).isEqualTo("'a\\\\b'");
    }

    @Test
    void recordIdLiteral_removesBackticks() {
        assertThat(SurrealQLUtil.recordIdLiteral("token", "id`1")).isEqualTo("token:`id1`");
    }

    @Test
    void recordIdLiteral_usesEmptyIdWhenNull() {
        assertThat(SurrealQLUtil.recordIdLiteral("token", null)).isEqualTo("token:``");
    }

    @Test
    void datetime_formatsUtcIso() {
        String result = SurrealQLUtil.datetime(LocalDateTime.of(2025, 1, 1, 10, 30, 0));

        assertThat(result).isEqualTo("d'2025-01-01T10:30:00Z'");
    }

    @Test
    void datetime_usesNow_whenNull() {
        String before = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1)
                .atOffset(ZoneOffset.UTC)
                .format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        String result = SurrealQLUtil.datetime(null);
        String after = LocalDateTime.now(ZoneOffset.UTC).plusMinutes(1)
                .atOffset(ZoneOffset.UTC)
                .format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        assertThat(result).startsWith("d'").endsWith("'");
        String inner = result.substring(2, result.length() - 1);
        java.time.OffsetDateTime parsed = java.time.OffsetDateTime.parse(inner);
        assertThat(parsed.toInstant()).isAfter(java.time.OffsetDateTime.parse(before).toInstant());
        assertThat(parsed.toInstant()).isBefore(java.time.OffsetDateTime.parse(after).toInstant());
    }
}