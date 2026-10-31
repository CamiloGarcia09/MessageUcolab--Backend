package co.edu.uco.infraestructure.secondaryadapters.vault.azure;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.vault.VaultPort;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.CrossWordsException;
import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionType;
import com.azure.core.exception.ClientAuthenticationException;
import com.azure.core.exception.HttpResponseException;
import com.azure.core.exception.ResourceNotFoundException;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static co.edu.uco.crosscutting.helpers.UtilObject.isNullObject;
import static co.edu.uco.crosscutting.helpers.UtilText.isEmptyOrNull;
import static co.edu.uco.crosscutting.helpers.UtilText.trim;

@Component
public class AzureKeyVaultAdapter implements VaultPort {

    private final LoggingPort log;
    private final CatalogPort catalogPort;
    private final SecretClient secretClient;

    public AzureKeyVaultAdapter(
            @Value("${azure.keyvault.url:}") String urlVault,
            CatalogPort catalogPort,
            LoggingPortFactory loggerFactory
    ) {
        this.log = loggerFactory.getLogger(AzureKeyVaultAdapter.class);
        this.catalogPort = catalogPort;

        if (isEmptyOrNull(trim(urlVault))) {
            var techMsg = catalogPort.getMessage(MessageCatalogCodeEnum.TCH_041.getCode());
            log.error(techMsg);
            throw CrossWordsException.buildInfrastructure(
                    techMsg,
                    catalogPort.getMessage(MessageCatalogCodeEnum.FUN_023.getCode()),
                    ExceptionType.TECHNICAL
            );
        }
        this.secretClient = buildSecretClient(urlVault);
    }

    private SecretClient buildSecretClient(String urlVault) {
        try {
            return new SecretClientBuilder()
                    .vaultUrl(urlVault)
                    .credential(new DefaultAzureCredentialBuilder().build())
                    .buildClient();
        } catch (Exception ex) {
            var techMsg = catalogPort.getMessage(MessageCatalogCodeEnum.TCH_042.getCode()).formatted(urlVault);
            log.error(techMsg, ex);
            throw CrossWordsException.buildInfrastructure(
                    techMsg,
                    catalogPort.getMessage(MessageCatalogCodeEnum.FUN_023.getCode()),
                    ex,
                    ExceptionType.TECHNICAL
            );
        }
    }

    @Override
    public String getSecretValue(String secretName) {
        if (isEmptyOrNull(trim(secretName))) {
            var techMsg = catalogPort.getMessage(MessageCatalogCodeEnum.TCH_043.getCode());
            log.error(techMsg);
            throw CrossWordsException.buildInfrastructure(
                    techMsg,
                    catalogPort.getMessage(MessageCatalogCodeEnum.FUN_023.getCode()),
                    ExceptionType.TECHNICAL
            );
        }

        try {
            KeyVaultSecret secret = secretClient.getSecret(secretName);

            if (isNullObject(secret) || isEmptyOrNull(secret.getValue())) {
                var techMsg = catalogPort.getMessage(MessageCatalogCodeEnum.TCH_045.getCode()).formatted(secretName);
                log.error(techMsg);
                throw CrossWordsException.buildInfrastructure(
                        techMsg,
                        catalogPort.getMessage(MessageCatalogCodeEnum.FUN_023.getCode()),
                        ExceptionType.TECHNICAL
                );
            }

            if (!isNullObject(secret.getProperties()) && Boolean.FALSE.equals(secret.getProperties().isEnabled())) {
                var techMsg = catalogPort.getMessage(MessageCatalogCodeEnum.TCH_046.getCode()).formatted(secretName);
                log.error(techMsg);
                throw CrossWordsException.buildInfrastructure(
                        techMsg,
                        catalogPort.getMessage(MessageCatalogCodeEnum.FUN_023.getCode()),
                        ExceptionType.TECHNICAL
                );
            }

            return secret.getValue();

        } catch (ResourceNotFoundException ex) {
            var techMsg = catalogPort.getMessage(MessageCatalogCodeEnum.TCH_047.getCode()).formatted(secretName);
            log.error(techMsg, ex);
            throw CrossWordsException.buildInfrastructure(
                    techMsg,
                    catalogPort.getMessage(MessageCatalogCodeEnum.FUN_023.getCode()),
                    ex,
                    ExceptionType.TECHNICAL
            );

        } catch (ClientAuthenticationException ex) {
            var techMsg = catalogPort.getMessage(MessageCatalogCodeEnum.TCH_048.getCode()).formatted(secretName);
            log.error(techMsg, ex);
            throw CrossWordsException.buildInfrastructure(
                    techMsg,
                    catalogPort.getMessage(MessageCatalogCodeEnum.FUN_023.getCode()),
                    ex,
                    ExceptionType.TECHNICAL
            );

        } catch (HttpResponseException ex) {
            var techMsg = catalogPort.getMessage(MessageCatalogCodeEnum.TCH_049.getCode()).formatted(ex.getResponse().getStatusCode());
            log.error(techMsg, ex);
            throw CrossWordsException.buildInfrastructure(
                    techMsg,
                    catalogPort.getMessage(MessageCatalogCodeEnum.FUN_023.getCode()),
                    ex,
                    ExceptionType.TECHNICAL
            );

        } catch (CrossWordsException ex) {
            throw ex;
        } catch (Exception ex) {
            var techMsg = catalogPort.getMessage(MessageCatalogCodeEnum.TCH_050.getCode()).formatted(secretName);
            log.error(techMsg, ex);
            throw CrossWordsException.buildInfrastructure(
                    techMsg,
                    catalogPort.getMessage(MessageCatalogCodeEnum.FUN_023.getCode()),
                    ex,
                    ExceptionType.TECHNICAL
            );
        }
    }
}
