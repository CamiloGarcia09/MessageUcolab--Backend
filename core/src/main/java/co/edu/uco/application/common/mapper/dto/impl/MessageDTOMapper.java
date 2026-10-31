package co.edu.uco.application.common.mapper.dto.impl;

import co.edu.uco.application.primaryports.dto.message.MessageDTO;
import co.edu.uco.application.common.mapper.dto.DTOMapper;
import co.edu.uco.application.usecase.domain.MessageDomain;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public final class MessageDTOMapper implements DTOMapper<MessageDTO, MessageDomain> {

    private final ModelMapper modelMapper;

    public MessageDTOMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    @Override
    public MessageDomain mapperDomain(MessageDTO dto) {
        return modelMapper.map(dto, MessageDomain.class);
    }

    @Override
    public MessageDTO mapperDTO(MessageDomain domain) {
        return modelMapper.map(domain, MessageDTO.class);
    }
}