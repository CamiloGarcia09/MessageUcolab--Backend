package co.edu.uco.application.common.mapper.dto.impl;

import co.edu.uco.application.primaryports.dto.message.MessageCodeDTO;
import co.edu.uco.application.common.mapper.dto.DTOMapper;
import co.edu.uco.application.usecase.domain.MessageCodeDomain;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public final class MessageCodeDTOMapper implements DTOMapper<MessageCodeDTO, MessageCodeDomain> {
    private final ModelMapper modelMapper;
    public MessageCodeDTOMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }
    @Override
    public MessageCodeDomain mapperDomain(MessageCodeDTO dto) {
        return modelMapper.map(dto, MessageCodeDomain.class);
    }
    @Override
    public MessageCodeDTO mapperDTO(MessageCodeDomain domain) {
        return modelMapper.map(domain, MessageCodeDTO.class);
    }
}