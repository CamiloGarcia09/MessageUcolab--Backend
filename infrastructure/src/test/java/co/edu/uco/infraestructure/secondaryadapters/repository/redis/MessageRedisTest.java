package co.edu.uco.infraestructure.secondaryadapters.repository.redis;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class MessageRedisTest {

    @Nested
    class DefaultConstructor {

        @Test
        void defaultConstructor_initializesEmptyFieldsAndNonNullId() {
            MessageRedis redis = new MessageRedis();

            assertAll(
                    () -> assertThat(redis.getId()).isNotNull(),
                    () -> assertThat(redis.getCode()).isEmpty(),
                    () -> assertThat(redis.getTitle()).isEmpty(),
                    () -> assertThat(redis.getContent()).isEmpty(),
                    () -> assertThat(redis.getType()).isEmpty(),
                    () -> assertThat(redis.getCategory()).isEmpty(),
                    () -> assertThat(redis.getStatus()).isEmpty(),
                    () -> assertThat(redis.getApplication()).isEmpty(),
                    () -> assertThat(redis.getFunctionality()).isEmpty(),
                    () -> assertThat(redis.getEnvironmentId()).isEmpty());
        }
    }

    @Nested
    class FullConstructor {

        @Test
        void fullConstructor_typeBeforeCategory_assignsEachValueToItsOwnField() {
            UUID id = UUID.randomUUID();

            MessageRedis redis = new MessageRedis(id, " CODE ", " Title ", " Content ",
                    " Functional ", " Information ", " STATUS ", " APP ", " FUNC ", " ENV ");

            assertAll(
                    () -> assertThat(redis.getId()).isEqualTo(id),
                    () -> assertThat(redis.getCode()).isEqualTo("CODE"),
                    () -> assertThat(redis.getTitle()).isEqualTo("Title"),
                    () -> assertThat(redis.getContent()).isEqualTo("Content"),
                    () -> assertThat(redis.getType()).isEqualTo("Functional"),
                    () -> assertThat(redis.getCategory()).isEqualTo("Information"),
                    () -> assertThat(redis.getStatus()).isEqualTo("STATUS"),
                    () -> assertThat(redis.getApplication()).isEqualTo("APP"),
                    () -> assertThat(redis.getFunctionality()).isEqualTo("FUNC"),
                    () -> assertThat(redis.getEnvironmentId()).isEqualTo("ENV"));
        }

        @Test
        void fullConstructor_nullArguments_appliesDefaultsInsteadOfThrowing() {
            MessageRedis redis = assertDoesNotThrow(() -> new MessageRedis(null, null, null,
                    null, null, null, null, null, null, null));

            assertAll(
                    () -> assertThat(redis.getId()).isNotNull(),
                    () -> assertThat(redis.getCode()).isEmpty(),
                    () -> assertThat(redis.getTitle()).isEmpty(),
                    () -> assertThat(redis.getContent()).isEmpty(),
                    () -> assertThat(redis.getType()).isEmpty(),
                    () -> assertThat(redis.getCategory()).isEmpty(),
                    () -> assertThat(redis.getStatus()).isEmpty());
        }
    }

    @Nested
    class Setters {

        @Test
        void setters_applyTrimAndDefaultId() {
            MessageRedis redis = new MessageRedis();
            redis.setCode("  newcode  ");
            redis.setEnvironmentId(null);
            redis.setId(null);

            assertAll(
                    () -> assertThat(redis.getCode()).isEqualTo("newcode"),
                    () -> assertThat(redis.getEnvironmentId()).isEmpty(),
                    () -> assertThat(redis.getId()).isNotNull());
        }

        @Test
        void setters_typeAndCategory_storeEachValueIndependently() {
            MessageRedis redis = new MessageRedis();
            redis.setType(" Functional ");
            redis.setCategory(" Information ");

            assertAll(
                    () -> assertThat(redis.getType()).isEqualTo("Functional"),
                    () -> assertThat(redis.getCategory()).isEqualTo("Information"));
        }
    }
}
