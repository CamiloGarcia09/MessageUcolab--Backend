package co.edu.uco.infraestructure.secondaryadapters.repository.data;

import co.edu.uco.application.secondaryports.entity.MessageData;
import co.edu.uco.infraestructure.secondaryadapters.repository.surreal.model.MessageSurrealModel;
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
class MessageSurrealMapperTest {

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private MessageSurrealMapper mapper;

    @Test
    void mapperData_delegatesToModelMapper() {
        MessageSurrealModel model = MessageSurrealModel.build();
        MessageData expected = MessageData.build();
        when(modelMapper.map(model, MessageData.class)).thenReturn(expected);

        MessageData result = mapper.mapperData(model);

        assertThat(result).isSameAs(expected);
        verify(modelMapper).map(model, MessageData.class);
    }

    @Test
    void mapperModel_delegatesToModelMapper() {
        MessageData data = MessageData.build();
        MessageSurrealModel expected = MessageSurrealModel.build();
        when(modelMapper.map(data, MessageSurrealModel.class)).thenReturn(expected);

        MessageSurrealModel result = mapper.mapperModel(data);

        assertThat(result).isSameAs(expected);
        verify(modelMapper).map(data, MessageSurrealModel.class);
    }
}