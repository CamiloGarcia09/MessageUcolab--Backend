package co.edu.uco.application.common.mapper.entity.impl;

import co.edu.uco.application.primaryports.dto.message.MessageDTO;
import co.edu.uco.application.secondaryports.entity.FunctionalityData;
import co.edu.uco.application.secondaryports.entity.MessageCategoryData;
import co.edu.uco.application.secondaryports.entity.MessageData;
import co.edu.uco.application.secondaryports.entity.MessageTypeData;
import co.edu.uco.application.usecase.domain.MessageDomain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static co.edu.uco.crosscutting.helpers.UtilUUID.DEFAULT_UUID;

@ExtendWith(MockitoExtension.class)
class MessageEntityMapperTest {

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private MessageEntityMapper mapper;

    private MessageData messageData() {
        MessageData message = new MessageData();
        message.setCode("CODE");
        message.setTitle("Title");
        message.setContent("Content");
        message.setApplication("app");
        message.setType(MessageTypeData.build("info"));
        message.setCategory(MessageCategoryData.build("general"));
        message.setFunctionality(FunctionalityData.build("func"));
        return message;
    }

    @Test
    void mapperDomain_delegatesToModelMapper() {
        MessageData entity = messageData();
        MessageDomain domain = new MessageDomain();
        when(modelMapper.map(entity, MessageDomain.class)).thenReturn(domain);

        MessageDomain result = mapper.mapperDomain(entity);

        assertThat(result).isSameAs(domain);
        verify(modelMapper).map(entity, MessageDomain.class);
    }

    @Test
    void mapperData_delegatesToModelMapper() {
        MessageDomain domain = new MessageDomain();
        domain.setCode("CODE");
        MessageData entity = new MessageData();
        when(modelMapper.map(domain, MessageData.class)).thenReturn(entity);

        MessageData result = mapper.mapperData(domain);

        assertThat(result).isSameAs(entity);
        verify(modelMapper).map(domain, MessageData.class);
    }

    @Test
    void mapperDTO_buildsDtoFromEntityComposites() {
        MessageData data = messageData();
        data.setCreatedAt(Instant.parse("2025-01-02T03:04:05Z"));
        data.setUpdatedAt(Instant.parse("2025-02-03T04:05:06Z"));
        data.setStatus(new co.edu.uco.application.secondaryports.entity.StatusMessageData(
                UUID.fromString("c9dc13ee-2f0e-4912-946d-e7e4352c6510"), "Inactive"));
        data.setFunctionality(new FunctionalityData(
                UUID.fromString("c2d9f822-36d3-4580-a838-4f79f3c7cff8"), "func", null));
        data.setId(UUID.fromString("7b3e5d91-a2c8-46f0-9d14-5e7a1b6c3f82"));
        MessageDTO dto = mapper.mapperDTO(data);

        assertAll(
                () -> assertThat(dto.getCode()).isEqualTo("CODE"),
                () -> assertThat(dto.getTitle()).isEqualTo("Title"),
                () -> assertThat(dto.getContent()).isEqualTo("Content"),
                () -> assertThat(dto.getType()).isEqualTo("info"),
                () -> assertThat(dto.getCategory()).isEqualTo("general"),
                () -> assertThat(dto.getFunctionality()).isEqualTo("func"),
                () -> assertThat(dto.getCreatedAt()).isEqualTo(Instant.parse("2025-01-02T03:04:05Z")),
                () -> assertThat(dto.getUpdatedAt()).isEqualTo(Instant.parse("2025-02-03T04:05:06Z")),
                () -> assertThat(dto.getStatus()).isEqualTo("Inactive"),
                () -> assertThat(dto.getStatusId()).isEqualTo("c9dc13ee-2f0e-4912-946d-e7e4352c6510"),
                () -> assertThat(dto.getFunctionalityId()).isEqualTo("c2d9f822-36d3-4580-a838-4f79f3c7cff8"));
    }

    @Test
    void mapperDTO_normalizesEmptyCompositeNames() {
        MessageData message = new MessageData();
        message.setCode("CODE");
        message.setTitle("Title");
        message.setContent("Content");
        message.setApplication("app");
        message.setStatus(new co.edu.uco.application.secondaryports.entity.StatusMessageData(DEFAULT_UUID, ""));
        message.setFunctionality(new FunctionalityData(DEFAULT_UUID, "", null));

        MessageDTO dto = mapper.mapperDTO(message);

        assertAll(
                () -> assertThat(dto.getType()).isEmpty(),
                () -> assertThat(dto.getCategory()).isEmpty(),
                () -> assertThat(dto.getFunctionality()).isEmpty(),
                () -> assertThat(dto.getStatusId()).isNull(),
                () -> assertThat(dto.getFunctionalityId()).isNull(),
                () -> assertThat(dto.getUpdatedAt()).isNull());
    }
}
