package co.edu.uco.infraestructure.secondaryadapters.repository.data;

import co.edu.uco.application.secondaryports.entity.FunctionalityData;
import co.edu.uco.application.secondaryports.entity.MessageCategoryData;
import co.edu.uco.application.secondaryports.entity.MessageData;
import co.edu.uco.application.secondaryports.entity.MessageTypeData;
import co.edu.uco.infraestructure.secondaryadapters.repository.redis.MessageRedis;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static co.edu.uco.application.CrosswordsConstant.SINGLETON_SCOPE;
import static co.edu.uco.crosscutting.helpers.UtilText.EMPTY;

@Component
@Scope(SINGLETON_SCOPE)
public final class MessageDataCacheMapper implements DataMapper<MessageData, MessageRedis> {
    @Override
    public MessageData mapperData(MessageRedis model) {
        var data = new MessageData(model.getId(), model.getCode(), model.getTitle(),
                model.getContent(), MessageTypeData.build(model.getType()),
                MessageCategoryData.build(model.getCategory()), model.getApplication(), FunctionalityData.build(model.getFunctionality()));
        data.setCreatedAt(model.getCreatedAt());
        data.setUpdatedAt(model.getUpdatedAt());
        return data;
    }
    @Override
    public MessageRedis mapperModel(MessageData data) {
        var model = new MessageRedis(data.getId(), data.getCode(), data.getTitle(), data.getContent(),
                data.getType().getName(),
                data.getCategory().getName(), data.getStatus().getName(), data.getApplication(),
                data.getFunctionality().getName(), EMPTY);
        model.setCreatedAt(data.getCreatedAt());
        model.setUpdatedAt(data.getUpdatedAt());
        return model;
    }
}
