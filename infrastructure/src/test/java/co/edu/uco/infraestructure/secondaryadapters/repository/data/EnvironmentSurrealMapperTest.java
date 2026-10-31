package co.edu.uco.infraestructure.secondaryadapters.repository.data;

import co.edu.uco.application.secondaryports.entity.EnvironmentData;
import co.edu.uco.infraestructure.secondaryadapters.repository.surreal.model.EnvironmentSurrealModel;
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
class EnvironmentSurrealMapperTest {

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private EnvironmentSurrealMapper mapper;

    @Test
    void mapperData_delegatesToModelMapper() {
        EnvironmentSurrealModel model = EnvironmentSurrealModel.build();
        EnvironmentData expected = EnvironmentData.build();
        when(modelMapper.map(model, EnvironmentData.class)).thenReturn(expected);

        EnvironmentData result = mapper.mapperData(model);

        assertThat(result).isSameAs(expected);
        verify(modelMapper).map(model, EnvironmentData.class);
    }

    @Test
    void mapperModel_delegatesToModelMapper() {
        EnvironmentData data = EnvironmentData.build();
        EnvironmentSurrealModel expected = EnvironmentSurrealModel.build();
        when(modelMapper.map(data, EnvironmentSurrealModel.class)).thenReturn(expected);

        EnvironmentSurrealModel result = mapper.mapperModel(data);

        assertThat(result).isSameAs(expected);
        verify(modelMapper).map(data, EnvironmentSurrealModel.class);
    }
}