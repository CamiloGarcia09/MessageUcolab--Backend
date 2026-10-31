package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl;

import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.RecordExistsCatalogPort;
import co.edu.uco.application.secondaryports.repository.ReferenceCatalog;
import com.surrealdb.Surreal;
import org.springframework.stereotype.Repository;

import static co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl.SurrealQLUtil.recordIdLiteral;

@Repository
public class RecordExistsCatalogSurrealAdapter extends SurrealCatalogSupport implements RecordExistsCatalogPort {

    public RecordExistsCatalogSurrealAdapter(final Surreal surreal, final LoggingPortFactory loggerFactory) {
        super(surreal, loggerFactory.getLogger(RecordExistsCatalogSurrealAdapter.class));
    }

    @Override
    public boolean exists(final ReferenceCatalog reference, final String id) {
        final String sql = "SELECT * FROM " + recordIdLiteral(reference.getTable(), id) + " LIMIT 1;";
        return queryOne(sql, "Error al validar el registro de catálogo en SurrealDB: " + sql, obj -> obj).isPresent();
    }
}