package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl;

import co.edu.uco.application.secondaryports.entity.MessageCategoryData;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.MessageCategoryCatalogRepository;
import com.surrealdb.Object;
import com.surrealdb.Surreal;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MessageCategoryCatalogSurrealAdapter extends SurrealCatalogSupport implements MessageCategoryCatalogRepository {

    private static final String SURREAL_TABLE_MESSAGE_CATEGORY = "message_category";

    public MessageCategoryCatalogSurrealAdapter(final Surreal surreal, final LoggingPortFactory loggerFactory) {
        super(surreal, loggerFactory.getLogger(MessageCategoryCatalogSurrealAdapter.class));
    }

    @Override
    public List<MessageCategoryData> findAll() {
        return queryAll(SURREAL_TABLE_MESSAGE_CATEGORY, "Error al consultar categorías de mensaje en SurrealDB: ", this::toMessageCategoryData);
    }

    private MessageCategoryData toMessageCategoryData(final Object obj) {
        final MessageCategoryData data = MessageCategoryData.build();
        data.setId(extractCatalogId(obj.get("id")));
        data.setName(stringOf(obj.get("name")));
        return data;
    }
}