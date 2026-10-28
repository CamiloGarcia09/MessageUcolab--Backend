package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl;

import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.MessageCodeQueryPort;
import com.surrealdb.Surreal;
import org.springframework.stereotype.Repository;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;
import co.edu.uco.application.common.catalog.CatalogPortStaticRef;
import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.TCH_078;

import static co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl.SurrealQLUtil.quote;
import static co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl.SurrealQLUtil.recordIdLiteral;

@Repository
public class MessageCodeSurrealAdapter extends SurrealCatalogSupport implements MessageCodeQueryPort {

    private static final String TABLE_MESSAGE = "message";
    private static final String TABLE_APPLICATION = "application";

    public MessageCodeSurrealAdapter(Surreal surreal, LoggingPortFactory loggerFactory) {
        super(surreal, loggerFactory.getLogger(MessageCodeSurrealAdapter.class));
    }

    @Override
    public boolean existsByCodeAndApplicationId(String code, String applicationId) {
        String sql = "SELECT id FROM " + TABLE_MESSAGE
                + " WHERE code = " + quote(code)
                + " AND application_id = " + recordIdLiteral(TABLE_APPLICATION, applicationId)
                + " LIMIT 1;";
        return queryOne(sql, "Error al validar el código del mensaje en SurrealDB: ", row -> row).isPresent();
    }

    @Override
    public Set<String> findExistingCodes(Set<String> codes, String applicationId) {
        if (codes.isEmpty()) {
            return Set.of();
        }
        var sql = "SELECT code FROM message WHERE application_id = " + recordIdLiteral(TABLE_APPLICATION, applicationId)
                + " AND code IN [" + codes.stream().map(SurrealQLUtil::quote).collect(Collectors.joining(",")) + "];";
        return new HashSet<>(query(sql, CatalogPortStaticRef.getMessage(TCH_078.getCode()), row -> stringOf(row.get("code"))));
    }
}
