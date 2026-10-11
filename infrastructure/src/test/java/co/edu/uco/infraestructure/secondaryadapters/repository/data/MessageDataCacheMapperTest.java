package co.edu.uco.infraestructure.secondaryadapters.repository.data;

import co.edu.uco.application.secondaryports.entity.FunctionalityData;
import co.edu.uco.application.secondaryports.entity.MessageCategoryData;
import co.edu.uco.application.secondaryports.entity.MessageData;
import co.edu.uco.application.secondaryports.entity.MessageTypeData;
import co.edu.uco.infraestructure.secondaryadapters.repository.redis.MessageRedis;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class MessageDataCacheMapperTest {

    private final MessageDataCacheMapper mapper = new MessageDataCacheMapper();

    @Nested
    class MapperData {

        @Test
        void mapperData_mapsEachRedisFieldToItsMatchingDataField() {
            UUID id = UUID.randomUUID();
            MessageRedis model = new MessageRedis();
            model.setId(id);
            model.setCode("CODE");
            model.setTitle("Title");
            model.setContent("Content");
            model.setType("TYPE");
            model.setCategory("CATEGORY");
            model.setApplication("APP");
            model.setFunctionality("FUNC");
            model.setCreatedAt(Instant.parse("2025-01-02T03:04:05Z"));

            MessageData data = mapper.mapperData(model);

            assertAll(
                    () -> assertThat(data.getId()).isEqualTo(id),
                    () -> assertThat(data.getCode()).isEqualTo("CODE"),
                    () -> assertThat(data.getTitle()).isEqualTo("Title"),
                    () -> assertThat(data.getContent()).isEqualTo("Content"),
                    () -> assertThat(data.getType().getName()).isEqualTo("TYPE"),
                    () -> assertThat(data.getCategory().getName()).isEqualTo("CATEGORY"),
                    () -> assertThat(data.getApplication()).isEqualTo("APP"),
                    () -> assertThat(data.getFunctionality().getName()).isEqualTo("FUNC"),
                    () -> assertThat(data.getCreatedAt()).isEqualTo(Instant.parse("2025-01-02T03:04:05Z")));
        }

        @Test
        void mapperData_emptyModel_producesEmptyTypeAndCategoryNames() {
            MessageData data = mapper.mapperData(new MessageRedis());

            assertAll(
                    () -> assertThat(data.getType().getName()).isEmpty(),
                    () -> assertThat(data.getCategory().getName()).isEmpty());
        }
    }

    @Nested
    class MapperModel {

        @Test
        void mapperModel_mapsTypeAndCategoryWithoutSwappingThem() {
            UUID id = UUID.randomUUID();
            MessageData data = new MessageData(id, "CODE", "Title", "Content",
                    MessageTypeData.build("Functional"), MessageCategoryData.build("Information"),
                    "APP", FunctionalityData.build("FUNC"));
            data.setCreatedAt(Instant.parse("2025-01-02T03:04:05Z"));

            MessageRedis model = mapper.mapperModel(data);

            assertAll(
                    () -> assertThat(model.getId()).isEqualTo(id),
                    () -> assertThat(model.getCode()).isEqualTo("CODE"),
                    () -> assertThat(model.getTitle()).isEqualTo("Title"),
                    () -> assertThat(model.getContent()).isEqualTo("Content"),
                    () -> assertThat(model.getType()).isEqualTo("Functional"),
                    () -> assertThat(model.getCategory()).isEqualTo("Information"),
                    () -> assertThat(model.getApplication()).isEqualTo("APP"),
                    () -> assertThat(model.getFunctionality()).isEqualTo("FUNC"),
                    () -> assertThat(model.getEnvironmentId()).isEmpty(),
                    () -> assertThat(model.getCreatedAt()).isEqualTo(Instant.parse("2025-01-02T03:04:05Z")));
        }

        @Test
        void mapperModel_defaultMessageData_producesEmptyRedisFields() {
            MessageRedis model = mapper.mapperModel(MessageData.build());

            assertAll(
                    () -> assertThat(model.getType()).isEmpty(),
                    () -> assertThat(model.getCategory()).isEmpty(),
                    () -> assertThat(model.getStatus()).isEmpty());
        }
    }

    @Nested
    class RoundTrip {

        @Test
        void roundTrip_preservesTypeFunctionalAndCategoryInformation_withoutInvertingThem() {
            UUID id = UUID.randomUUID();
            MessageData original = new MessageData(id, "CODE", "Title", "Content",
                    MessageTypeData.build("Functional"), MessageCategoryData.build("Information"),
                    "APP", FunctionalityData.build("FUNC"));

            MessageRedis cached = mapper.mapperModel(original);
            MessageData recovered = mapper.mapperData(cached);

            assertAll(
                    () -> assertThat(cached.getType()).isEqualTo("Functional"),
                    () -> assertThat(cached.getCategory()).isEqualTo("Information"),
                    () -> assertThat(recovered.getType().getName()).isEqualTo("Functional"),
                    () -> assertThat(recovered.getCategory().getName()).isEqualTo("Information"),
                    () -> assertThat(recovered.getId()).isEqualTo(original.getId()),
                    () -> assertThat(recovered.getCode()).isEqualTo(original.getCode()),
                    () -> assertThat(recovered.getTitle()).isEqualTo(original.getTitle()),
                    () -> assertThat(recovered.getContent()).isEqualTo(original.getContent()),
                    () -> assertThat(recovered.getApplication()).isEqualTo(original.getApplication()),
                    () -> assertThat(recovered.getFunctionality().getName())
                            .isEqualTo(original.getFunctionality().getName()));
        }
    }
}
