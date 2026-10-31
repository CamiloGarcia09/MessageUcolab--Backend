package co.edu.uco.application.common.mapper.dto.impl;

import co.edu.uco.application.primaryports.dto.message.MessageDTO;
import co.edu.uco.application.usecase.domain.MessageDomain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageDTOMapperTest {

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private MessageDTOMapper mapper;

    @Test
    void mapperDomain_delegatesToModelMapper() {
        MessageDTO dto = MessageDTO.create("CODE", "Title", "Content", "info", "general", "func");
        MessageDomain domain = new MessageDomain();
        when(modelMapper.map(dto, MessageDomain.class)).thenReturn(domain);

        MessageDomain result = mapper.mapperDomain(dto);

        assertThat(result).isSameAs(domain);
        verify(modelMapper).map(dto, MessageDomain.class);
    }

    @Test
    void mapperDTO_delegatesToModelMapper() {
        MessageDomain domain = new MessageDomain();
        domain.setCode("CODE");
        MessageDTO dto = MessageDTO.create("CODE", "Title", "Content", "info", "general", "func");
        when(modelMapper.map(domain, MessageDTO.class)).thenReturn(dto);

        MessageDTO result = mapper.mapperDTO(domain);

        assertThat(result).isSameAs(dto);
        verify(modelMapper).map(domain, MessageDTO.class);
    }
}