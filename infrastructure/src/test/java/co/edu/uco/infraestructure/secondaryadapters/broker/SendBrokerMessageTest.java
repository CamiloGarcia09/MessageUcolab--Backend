package co.edu.uco.infraestructure.secondaryadapters.broker;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.logging.*;
import co.edu.uco.application.usecase.domain.MessageCodeDomain;
import co.edu.uco.crosscutting.exceptions.CrossWordsException;
import co.edu.uco.crosscutting.helpers.json.UtilMapperJson;
import co.edu.uco.infraestructure.config.PulsarProperties;
import org.apache.pulsar.client.api.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class SendBrokerMessageTest {
    @Mock PulsarClient client; @Mock UtilMapperJson mapper; @Mock CatalogPort catalog;
    @Mock LoggingPortFactory factory; @Mock LoggingPort log; @Mock ProducerBuilder<String> builder;
    @Mock Producer<String> producer;
    PulsarProperties properties = new PulsarProperties(); SendBrokerMessage adapter;
    @BeforeEach void setUp() { when(factory.getLogger(SendBrokerMessage.class)).thenReturn(log);
        properties.setTopicName("topic-name"); adapter = new SendBrokerMessage(client, mapper, properties, catalog, factory); }
    private MessageCodeDomain domain() { return MessageCodeDomain.create("MSG-001"); }
    private void chain() throws Exception { when(client.newProducer(Schema.STRING)).thenReturn(builder);
        when(builder.topic("topic-name")).thenReturn(builder); when(builder.create()).thenReturn(producer); }
    @Test void nullMessage_isRejected() {
        when(catalog.getMessage("TCH_038")).thenReturn("tech"); when(catalog.getMessage("FUN_023")).thenReturn("user");
        assertThatThrownBy(() -> adapter.execute(null, null)).isInstanceOf(CrossWordsException.class);
    }
    @Test void nullClient_isRejected() {
        SendBrokerMessage noClient = new SendBrokerMessage(null, mapper, properties, catalog, factory);
        when(catalog.getMessage("TCH_002")).thenReturn("tech"); when(catalog.getMessage("FUN_023")).thenReturn("user");
        assertThatThrownBy(() -> noClient.execute(domain(), null)).isInstanceOf(CrossWordsException.class);
    }
    @Test void serializedMessage_isSent() throws Exception {
        chain(); when(mapper.execute(any())).thenReturn(Optional.of("json"));
        adapter.execute(domain(), null); verify(producer).send("json"); verify(producer).close();
    }
    @Test void emptySerialization_isRejected() throws Exception {
        chain(); when(mapper.execute(any())).thenReturn(Optional.empty());
        when(catalog.getMessage("TCH_039")).thenReturn("tech"); when(catalog.getMessage("FUN_023")).thenReturn("user");
        assertThatThrownBy(() -> adapter.execute(domain(), null)).isInstanceOf(CrossWordsException.class);
    }
}
