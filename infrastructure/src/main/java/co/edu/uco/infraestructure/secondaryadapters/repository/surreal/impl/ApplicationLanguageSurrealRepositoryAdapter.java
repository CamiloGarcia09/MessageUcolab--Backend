package co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.ApplicationLanguageRepository;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.CrossWordsException;
import com.surrealdb.Surreal;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static co.edu.uco.infraestructure.secondaryadapters.repository.surreal.impl.SurrealQLUtil.recordIdLiteral;

@Repository
public class ApplicationLanguageSurrealRepositoryAdapter extends SurrealCatalogSupport
        implements ApplicationLanguageRepository {
    private final CatalogPort catalog;

    public ApplicationLanguageSurrealRepositoryAdapter(Surreal surreal, LoggingPortFactory loggerFactory,
                                                        CatalogPort catalog) {
        super(surreal, loggerFactory.getLogger(ApplicationLanguageSurrealRepositoryAdapter.class));
        this.catalog = catalog;
    }

    @Override
    public Optional<String> findBaseLanguageCode(String applicationId) {
        var sql = "SELECT language_id.code AS code FROM " + recordIdLiteral("application", applicationId) + " LIMIT 1;";
        var error = catalog.getMessage(MessageCatalogCodeEnum.TCH_058.getCode()).formatted(sql);
        try {
            return queryOne(sql, error, row -> stringOf(row.get("code")));
        } catch (CrossWordsException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw CrossWordsException.buildInfrastructure(error, exception);
        }
    }
}
