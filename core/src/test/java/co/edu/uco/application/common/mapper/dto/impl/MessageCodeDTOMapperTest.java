package co.edu.uco.application.common.mapper.dto.impl;

import co.edu.uco.application.primaryports.dto.message.MessageCodeDTO;
import co.edu.uco.application.usecase.domain.MessageCodeDomain;
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
class MessageCodeDTOMapperTest {

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private MessageCodeDTOMapper mapper;

    @Test
    void mapperDomain_delegatesToModelMapper() {
        MessageCodeDTO dto = MessageCodeDTO.create("CODE-001");
        MessageCodeDomain domain = new MessageCodeDomain();
        when(modelMapper.map(dto, MessageCodeDomain.class)).thenReturn(domain);

        MessageCodeDomain result = mapper.mapperDomain(dto);

        assertThat(result).isSameAs(domain);
        verify(modelMapper).map(dto, MessageCodeDomain.class);
    }

    @Test
    void mapperDTO_delegatesToModelMapper() {
        MessageCodeDomain domain = new MessageCodeDomain();
        domain.setCode("CODE-002");
        MessageCodeDTO dto = MessageCodeDTO.create("CODE-002");
        when(modelMapper.map(domain, MessageCodeDTO.class)).thenReturn(dto);

        MessageCodeDTO result = mapper.mapperDTO(domain);

        assertThat(result).isSameAs(dto);
        verify(modelMapper).map(domain, MessageCodeDTO.class);
    }
}