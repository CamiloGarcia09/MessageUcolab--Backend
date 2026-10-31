package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl;

import co.edu.uco.application.secondaryports.entity.MessageEnvironmentStateData;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.MessageEnvironmentStateCatalogRepository;
import com.surrealdb.Object;
import com.surrealdb.Surreal;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MessageEnvironmentStateCatalogSurrealAdapter extends SurrealCatalogSupport implements MessageEnvironmentStateCatalogRepository {

    private static final String SURREAL_TABLE_MESSAGE_ENVIRONMENT_STATE = "message_environment_state";

    public MessageEnvironmentStateCatalogSurrealAdapter(final Surreal surreal, final LoggingPortFactory loggerFactory) {
        super(surreal, loggerFactory.getLogger(MessageEnvironmentStateCatalogSurrealAdapter.class));
    }

    @Override
    public List<MessageEnvironmentStateData> findAll() {
        return queryAll(SURREAL_TABLE_MESSAGE_ENVIRONMENT_STATE, "Error al consultar estados de entorno de mensaje en SurrealDB: ", this::toMessageEnvironmentStateData);
    }

    private MessageEnvironmentStateData toMessageEnvironmentStateData(final Object obj) {
        final MessageEnvironmentStateData data = MessageEnvironmentStateData.build();
        data.setId(extractCatalogId(obj.get("id")));
        data.setName(stringOf(obj.get("name")));
        return data;
    }
}