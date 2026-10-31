package co.edu.uco.infraestructure.config;

import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.crosscutting.catalog.BootstrapMessageCatalogEnum;
import com.surrealdb.Surreal;
import com.surrealdb.signin.RootCredential;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SurrealDBConfig {

    private final LoggingPort log;
    private final SurrealDBProperties surrealDBProperties;

    public SurrealDBConfig(SurrealDBProperties surrealDBProperties, LoggingPortFactory loggerFactory) {
        this.log = loggerFactory.getLogger(SurrealDBConfig.class);
        this.surrealDBProperties = surrealDBProperties;
    }

    @Bean
    public Surreal surreal() throws Exception {

        String connectionUrl = String.format(
                "ws://%s:%d/rpc",
                surrealDBProperties.getHost(),
                surrealDBProperties.getPort()
        );

        log.info(BootstrapMessageCatalogEnum.TCH_085.getContent(), connectionUrl);

        Surreal surreal = new Surreal();

        // Conectar
        surreal.connect(connectionUrl);

        // Autenticación root
        surreal.signin(
                new RootCredential(
                        surrealDBProperties.getUsername(),
                        surrealDBProperties.getPassword()
                )
        );

        log.info(BootstrapMessageCatalogEnum.TCH_086.getContent());

        // Seleccionar namespace
        surreal.useNs(
                surrealDBProperties.getNamespace()
        );

        // Seleccionar database
        surreal.useDb(
                surrealDBProperties.getDatabase()
        );

        log.info(
                BootstrapMessageCatalogEnum.TCH_104.getContent(),
                surrealDBProperties.getNamespace(),
                surrealDBProperties.getDatabase()
        );

        return surreal;
    }
}
