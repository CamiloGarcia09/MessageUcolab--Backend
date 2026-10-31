package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl;

import co.edu.uco.application.secondaryports.entity.StatusMessageData;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.MessageStateCatalogRepository;
import com.surrealdb.Object;
import com.surrealdb.Surreal;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MessageStateCatalogSurrealAdapter extends SurrealCatalogSupport implements MessageStateCatalogRepository {

    private static final String SURREAL_TABLE_MESSAGE_STATE = "message_state";

    public MessageStateCatalogSurrealAdapter(final Surreal surreal, final LoggingPortFactory loggerFactory) {
        super(surreal, loggerFactory.getLogger(MessageStateCatalogSurrealAdapter.class));
    }

    @Override
    public List<StatusMessageData> findAll() {
        return queryAll(SURREAL_TABLE_MESSAGE_STATE, "Error al consultar estados de mensaje en SurrealDB: ", this::toStatusMessageData);
    }

    private StatusMessageData toStatusMessageData(final Object obj) {
        final StatusMessageData data = StatusMessageData.build();
        data.setId(extractCatalogId(obj.get("id")));
        data.setName(stringOf(obj.get("name")));
        return data;
    }
}