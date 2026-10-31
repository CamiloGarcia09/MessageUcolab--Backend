package co.edu.uco.infraestructure.secondaryadapters.repository.data;

import co.edu.uco.application.secondaryports.entity.MessageData;
import co.edu.uco.infraestructure.secondaryadapters.repository.surreal.model.MessageSurrealModel;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public final class MessageSurrealMapper implements DataMapper<MessageData, MessageSurrealModel> {
    private final ModelMapper modelMapper;

    public MessageSurrealMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    @Override
    public MessageData mapperData(MessageSurrealModel model) {
        return modelMapper.map(model, MessageData.class);
    }

    @Override
    public MessageSurrealModel mapperModel(MessageData data) {
        return modelMapper.map(data, MessageSurrealModel.class);
    }
}
