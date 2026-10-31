package co.edu.uco.application.secondaryports;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class ErrorResponseTest {

    @Nested
    class CompactConstructor {

        @Test
        void keepsProvidedDataAndErrors() {
            ErrorDetail detail = new ErrorDetail("FUN_001", "Invalid request");
            ErrorResponse response = new ErrorResponse(List.of("payload"), List.of(detail),
                    "2026-09-26T10:00:00Z", "/messages");

            assertAll(
                    () -> assertThat(response.data()).first().isEqualTo("payload"),
                    () -> assertThat(response.errors()).containsExactly(detail),
                    () -> assertThat(response.timestamp()).isEqualTo("2026-09-26T10:00:00Z"),
                    () -> assertThat(response.path()).isEqualTo("/messages"));
        }

        @Test
        void replacesNullDataAndErrorsWithEmptyLists() {
            ErrorResponse response = new ErrorResponse(null, null, "2026-09-26T10:00:00Z", "/messages");

            assertAll(
                    () -> assertThat(response.data()).isEmpty(),
                    () -> assertThat(response.errors()).isEmpty());
        }
    }

    @Nested
    class FactoryMethod {

        @Test
        void of_buildsResponseWithEmptyDataAndInformedErrors() {
            ErrorDetail detail = new ErrorDetail("FUN_002", "Not found");

            ErrorResponse response = ErrorResponse.of(List.of(detail), "2026-09-26T10:00:00Z", "/messages");

            assertAll(
                    () -> assertThat(response.data()).isEmpty(),
                    () -> assertThat(response.errors()).containsExactly(detail),
                    () -> assertThat(response.timestamp()).isEqualTo("2026-09-26T10:00:00Z"),
                    () -> assertThat(response.path()).isEqualTo("/messages"));
        }
    }
}
