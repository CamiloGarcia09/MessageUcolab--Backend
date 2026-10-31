package co.edu.uco.infraestructure.config;

import org.apache.pulsar.client.api.PulsarClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BrokerConfigTest {

    @Mock
    private PulsarProperties pulsarProperties;
    @Mock
    private PulsarClient pulsarClient;

    @Test
    void pulsarClient_returnsCreatedClient() throws Exception {
        BrokerConfig config = new BrokerConfig(pulsarProperties);

        java.lang.reflect.Field clientField = BrokerConfig.class.getDeclaredField("client");
        clientField.setAccessible(true);
        clientField.set(config, pulsarClient);

        assertThat(config.pulsarClient()).isSameAs(pulsarClient);
    }

    @Test
    void cleanup_closesClient_whenClientIsNotNull() throws Exception {
        BrokerConfig config = new BrokerConfig(pulsarProperties);

        java.lang.reflect.Field clientField = BrokerConfig.class.getDeclaredField("client");
        clientField.setAccessible(true);
        clientField.set(config, pulsarClient);

        config.cleanup();

        verify(pulsarClient).close();
    }

    @Test
    void cleanup_doesNothing_whenClientIsNull() {
        BrokerConfig config = new BrokerConfig(pulsarProperties);

        assertThatCode(config::cleanup).doesNotThrowAnyException();
    }
}
