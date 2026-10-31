package co.edu.uco.infraestructure.secondaryadapters.vault.azure;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.crosscutting.exceptions.CrossWordsException;
import com.azure.core.exception.ClientAuthenticationException;
import com.azure.core.exception.HttpResponseException;
import com.azure.core.exception.ResourceNotFoundException;
import com.azure.core.http.HttpResponse;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import com.azure.security.keyvault.secrets.models.SecretProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AzureKeyVaultAdapterTest {

    @Mock
    private CatalogPort catalogPort;
    @Mock
    private LoggingPortFactory loggerFactory;
    @Mock
    private LoggingPort log;
    @Mock
    private SecretClient secretClient;

    private AzureKeyVaultAdapter adapter;

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(AzureKeyVaultAdapter.class)).thenReturn(log);
        adapter = new AzureKeyVaultAdapter("https://fake.vault.azure.net", catalogPort, loggerFactory);
    }

    private void injectSecretClient() throws Exception {
        Field field = AzureKeyVaultAdapter.class.getDeclaredField("secretClient");
        field.setAccessible(true);
        field.set(adapter, secretClient);
    }

    private void assertInfrastructureError(Throwable ex, String technicalMessage, String userMessage) {
        CrossWordsException cwe = (CrossWordsException) ex;
        assertThat(cwe.getTechnicalMessage()).isEqualTo(technicalMessage);
        assertThat(cwe.getUserMessage()).isEqualTo(userMessage);
    }

    @Test
    void constructor_buildsClient_whenUrlProvided() {
        lenient().when(catalogPort.getMessage("TCH_042")).thenReturn("failed %s");

        AzureKeyVaultAdapter built = new AzureKeyVaultAdapter("https://fake.vault.azure.net", catalogPort, loggerFactory);

        assertThat(built).isNotNull();
    }

    @Test
    void getSecretValue_throws_whenSecretNameIsBlank() {
        when(catalogPort.getMessage("TCH_043")).thenReturn("tech");
        when(catalogPort.getMessage("FUN_023")).thenReturn("user");

        assertThatThrownBy(() -> adapter.getSecretValue("  "))
                .isInstanceOf(CrossWordsException.class)
                .satisfies(ex -> assertInfrastructureError(ex, "tech", "user"));
    }

    @Test
    void getSecretValue_returnsValue_whenSecretFound() throws Exception {
        injectSecretClient();
        KeyVaultSecret secret = mock(KeyVaultSecret.class);
        when(secretClient.getSecret("secret-1")).thenReturn(secret);
        when(secret.getValue()).thenReturn("secret-value");

        assertThat(adapter.getSecretValue("secret-1")).isEqualTo("secret-value");
    }

    @Test
    void getSecretValue_throws_whenSecretIsNull() throws Exception {
        injectSecretClient();
        when(secretClient.getSecret("secret-1")).thenReturn(null);
        when(catalogPort.getMessage("TCH_045")).thenReturn("missing %s");
        when(catalogPort.getMessage("FUN_023")).thenReturn("user");

        assertThatThrownBy(() -> adapter.getSecretValue("secret-1"))
                .isInstanceOf(CrossWordsException.class)
                .satisfies(ex -> assertThat(((CrossWordsException) ex).getTechnicalMessage()).isEqualTo("missing secret-1"));
    }

    @Test
    void getSecretValue_throws_whenSecretDisabled() throws Exception {
        injectSecretClient();
        KeyVaultSecret secret = mock(KeyVaultSecret.class);
        SecretProperties properties = mock(SecretProperties.class);
        when(secretClient.getSecret("secret-1")).thenReturn(secret);
        when(secret.getValue()).thenReturn("secret-value");
        when(secret.getProperties()).thenReturn(properties);
        when(properties.isEnabled()).thenReturn(Boolean.FALSE);
        when(catalogPort.getMessage("TCH_046")).thenReturn("disabled %s");
        when(catalogPort.getMessage("FUN_023")).thenReturn("user");

        assertThatThrownBy(() -> adapter.getSecretValue("secret-1"))
                .isInstanceOf(CrossWordsException.class)
                .satisfies(ex -> assertThat(((CrossWordsException) ex).getTechnicalMessage()).isEqualTo("disabled secret-1"));
    }

    @Test
    void getSecretValue_throws_whenResourceNotFound() throws Exception {
        injectSecretClient();
        when(secretClient.getSecret("secret-1")).thenThrow(mock(ResourceNotFoundException.class));
        when(catalogPort.getMessage("TCH_047")).thenReturn("not found %s");
        when(catalogPort.getMessage("FUN_023")).thenReturn("user");

        assertThatThrownBy(() -> adapter.getSecretValue("secret-1"))
                .isInstanceOf(CrossWordsException.class)
                .satisfies(ex -> assertThat(((CrossWordsException) ex).getTechnicalMessage()).isEqualTo("not found secret-1"));
    }

    @Test
    void getSecretValue_throws_whenClientAuthenticationFails() throws Exception {
        injectSecretClient();
        when(secretClient.getSecret("secret-1")).thenThrow(mock(ClientAuthenticationException.class));
        when(catalogPort.getMessage("TCH_048")).thenReturn("auth %s");
        when(catalogPort.getMessage("FUN_023")).thenReturn("user");

        assertThatThrownBy(() -> adapter.getSecretValue("secret-1"))
                .isInstanceOf(CrossWordsException.class)
                .satisfies(ex -> assertThat(((CrossWordsException) ex).getTechnicalMessage()).isEqualTo("auth secret-1"));
    }

    @Test
    void getSecretValue_throws_whenHttpError() throws Exception {
        injectSecretClient();
        HttpResponse httpResponse = mock(HttpResponse.class);
        when(httpResponse.getStatusCode()).thenReturn(500);
        when(secretClient.getSecret("secret-1")).thenThrow(new HttpResponseException("http error", httpResponse));
        when(catalogPort.getMessage("TCH_049")).thenReturn("http %s");
        when(catalogPort.getMessage("FUN_023")).thenReturn("user");

        assertThatThrownBy(() -> adapter.getSecretValue("secret-1"))
                .isInstanceOf(CrossWordsException.class)
                .satisfies(ex -> assertThat(((CrossWordsException) ex).getTechnicalMessage()).isEqualTo("http 500"));
    }

    @Test
    void getSecretValue_throws_whenGenericError() throws Exception {
        injectSecretClient();
        when(secretClient.getSecret("secret-1")).thenThrow(new IllegalStateException("boom"));
        when(catalogPort.getMessage("TCH_050")).thenReturn("generic %s");
        when(catalogPort.getMessage("FUN_023")).thenReturn("user");

        assertThatThrownBy(() -> adapter.getSecretValue("secret-1"))
                .isInstanceOf(CrossWordsException.class)
                .satisfies(ex -> assertThat(((CrossWordsException) ex).getTechnicalMessage()).isEqualTo("generic secret-1"));
    }

    @Test
    void getSecretValue_rethrowsCrossWordsException() throws Exception {
        injectSecretClient();
        CrossWordsException original = CrossWordsException.buildInfrastructure("tech", "user", co.edu.uco.crosscutting.exceptions.enumeration.ExceptionType.TECHNICAL);
        when(secretClient.getSecret("secret-1")).thenThrow(original);

        assertThatThrownBy(() -> adapter.getSecretValue("secret-1"))
                .isSameAs(original);
    }
}
