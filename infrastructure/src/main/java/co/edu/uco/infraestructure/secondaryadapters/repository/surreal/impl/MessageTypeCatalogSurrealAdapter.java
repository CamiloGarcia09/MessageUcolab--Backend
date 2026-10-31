package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl;

import co.edu.uco.application.secondaryports.entity.MessageTypeData;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.MessageTypeCatalogRepository;
import com.surrealdb.Object;
import com.surrealdb.Surreal;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MessageTypeCatalogSurrealAdapter extends SurrealCatalogSupport implements MessageTypeCatalogRepository {

    private static final String SURREAL_TABLE_MESSAGE_TYPE = "message_type";

    public MessageTypeCatalogSurrealAdapter(final Surreal surreal, final LoggingPortFactory loggerFactory) {
        super(surreal, loggerFactory.getLogger(MessageTypeCatalogSurrealAdapter.class));
    }

    @Override
    public List<MessageTypeData> findAll() {
        return queryAll(SURREAL_TABLE_MESSAGE_TYPE, "Error al consultar tipos de mensaje en SurrealDB: ", this::toMessageTypeData);
    }

    private MessageTypeData toMessageTypeData(final Object obj) {
        final MessageTypeData data = MessageTypeData.build();
        data.setId(extractCatalogId(obj.get("id")));
        data.setName(stringOf(obj.get("name")));
        return data;
    }
}