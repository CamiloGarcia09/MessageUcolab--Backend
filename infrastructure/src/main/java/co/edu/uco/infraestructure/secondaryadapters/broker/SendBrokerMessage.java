package co.edu.uco.infraestructure.secondaryadapters.broker;

import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.usecase.domain.MessageCodeDomain;
import co.edu.uco.application.secondaryports.broker.SendMessage;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.infraestructure.config.PulsarProperties;
import co.edu.uco.crosscutting.exceptions.CrossWordsException;
import co.edu.uco.crosscutting.exceptions.enumeration.ExceptionType;
import co.edu.uco.crosscutting.helpers.json.UtilMapperJson;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.Schema;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static co.edu.uco.crosscutting.helpers.UtilObject.isNullObject;

@Component
public final class SendBrokerMessage implements SendMessage {
    private final LoggingPort log;
    private final PulsarClient client;
    private final UtilMapperJson utilMapperJson;
    private final PulsarProperties pulsarProperties;
    private final CatalogPort catalogPort;

    public SendBrokerMessage(
            PulsarClient client,
            UtilMapperJson utilMapperJson,
            PulsarProperties pulsarProperties,
            CatalogPort catalogPort,
            LoggingPortFactory loggerFactory
    ) {
        this.log = loggerFactory.getLogger(SendBrokerMessage.class);
        this.client = client;
        this.utilMapperJson = utilMapperJson;
        this.pulsarProperties = pulsarProperties;
        this.catalogPort = catalogPort;
    }

    @Override
    public void execute(MessageCodeDomain messageDomain, Void ignored) {
        if (isNullObject(messageDomain)) {
            var message = catalogPort.getMessage(MessageCatalogCodeEnum.TCH_038.getCode());
            log.error(message);
            throw CrossWordsException.buildInfrastructure(
                    message,
                    catalogPort.getMessage(MessageCatalogCodeEnum.FUN_023.getCode()),
                    ExceptionType.TECHNICAL
            );
        }

        if (isNullObject(this.client)) {
            var message = catalogPort.getMessage(MessageCatalogCodeEnum.TCH_002.getCode());
            log.error(message);
            throw CrossWordsException.buildInfrastructure(
                    message,
                    catalogPort.getMessage(MessageCatalogCodeEnum.FUN_023.getCode()),
                    ExceptionType.TECHNICAL
            );
        }

        try (Producer<String> stringProducer = this.client
                .newProducer(Schema.STRING)
                .topic(pulsarProperties.getTopicName())
                .create()) {
            Optional<String> message = utilMapperJson.execute(messageDomain);
            if (message.isPresent()) {
                stringProducer.send(message.get());
            } else {
                var techMsg = catalogPort.getMessage(MessageCatalogCodeEnum.TCH_039.getCode());
                log.error(techMsg);
                throw CrossWordsException.buildInfrastructure(
                        techMsg,
                        catalogPort.getMessage(MessageCatalogCodeEnum.FUN_023.getCode()),
                        ExceptionType.TECHNICAL
                );
            }
        } catch (PulsarClientException ex) {
            var message = catalogPort.getMessage(MessageCatalogCodeEnum.TCH_002.getCode());
            log.error(message, ex);
            throw CrossWordsException.buildInfrastructure(
                    message,
                    catalogPort.getMessage(MessageCatalogCodeEnum.FUN_023.getCode()),
                    ex,
                    ExceptionType.TECHNICAL
            );
        } catch (CrossWordsException ex) {
            throw ex;
        } catch (Exception ex) {
            var message = catalogPort.getMessage(MessageCatalogCodeEnum.TCH_002.getCode());
            log.error(message, ex);
            throw CrossWordsException.buildInfrastructure(
                    message,
                    catalogPort.getMessage(MessageCatalogCodeEnum.FUN_023.getCode()),
                    ex,
                    ExceptionType.TECHNICAL
            );
        }
    }
}