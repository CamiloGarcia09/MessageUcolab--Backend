package co.edu.uco.infraestructure.secondaryadapters.repository.data;

import co.edu.uco.application.secondaryports.entity.EnvironmentData;
import co.edu.uco.infraestructure.secondaryadapters.repository.surreal.model.EnvironmentSurrealModel;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public final class EnvironmentSurrealMapper implements DataMapper<EnvironmentData, EnvironmentSurrealModel> {
    private final ModelMapper modelMapper;

    public EnvironmentSurrealMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    @Override
    public EnvironmentData mapperData(EnvironmentSurrealModel model) {
        return modelMapper.map(model, EnvironmentData.class);
    }

    @Override
    public EnvironmentSurrealModel mapperModel(EnvironmentData data) {
        return modelMapper.map(data, EnvironmentSurrealModel.class);
    }
}
