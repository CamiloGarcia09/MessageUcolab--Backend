package co.edu.uco.application.common.mapper.entity.impl;

import co.edu.uco.application.primaryports.dto.message.MessageDTO;
import co.edu.uco.application.secondaryports.entity.MessageData;
import co.edu.uco.application.common.mapper.entity.DataMapper;
import co.edu.uco.application.usecase.domain.MessageDomain;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public final class MessageEntityMapper implements DataMapper<MessageData, MessageDomain, MessageDTO> {
    private final ModelMapper mapper;
    public MessageEntityMapper(ModelMapper mapper) {
        this.mapper = mapper;
    }
    @Override
    public MessageDomain mapperDomain(MessageData entity) {
        return mapper.map(entity, MessageDomain.class);
    }
    @Override
    public MessageData mapperData(MessageDomain domain) { return mapper.map(domain, MessageData.class); }
    @Override
    public MessageDTO mapperDTO(MessageData entity) { return MessageDTO.create(entity.getCode(), entity.getTitle(),
            entity.getContent(), entity.getType().getName(),
            entity.getCategory().getName(), entity.getFunctionality().getName(),
            entity.getCreatedAt()); }
}
