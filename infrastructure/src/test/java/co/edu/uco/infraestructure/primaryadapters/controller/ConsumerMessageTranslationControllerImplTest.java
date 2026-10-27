package co.edu.uco.infraestructure.primaryadapters.controller;

import co.edu.uco.application.primaryports.dto.message.ConsumerTranslatedMessageDTO;
import co.edu.uco.application.primaryports.dto.message.ConsumerTranslationRequestDTO;
import co.edu.uco.application.primaryports.dto.message.ResolvedMessageDTO;
import co.edu.uco.application.primaryports.facade.message.TranslateConsumerMessageUseCaseFacade;
import co.edu.uco.application.secondaryports.entity.MessageTranslationResponseData;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;
import co.edu.uco.infraestructure.secondaryadapters.presenter.serializer.SerializerRegistry;
import co.edu.uco.infraestructure.secondaryadapters.presenter.serializer.impl.json.JsonSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

import java.time.Instant;

import static co.edu.uco.infraestructure.config.InfrastructureConstant.APPLICATION_ID_ATTRIBUTE;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.ENVIRONMENT_ID_ATTRIBUTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsumerMessageTranslationControllerImplTest {
    @Mock private TranslateConsumerMessageUseCaseFacade facade;
    @Mock private SerializerRegistry serializers;

    private ConsumerMessageTranslationControllerImpl controller;

    @BeforeEach
    void setUp() {
        controller = new ConsumerMessageTranslationControllerImpl(facade, serializers);
    }

    @Test
    void translate_passesSessionContextAndWritesExactUncachedJson() {
        var serializer = new JsonSerializer();
        when(serializers.getSerializerForMediaType("application/json")).thenReturn(serializer);
        var original = new ResolvedMessageDTO("MSG", "original", "original body", "type", "category",
                "functionality", Instant.parse("2025-01-02T03:04:05Z"), Instant.parse("2025-02-03T04:05:06Z"));
        var translation = MessageTranslationResponseData.create("translated", "translated body", "tipo",
                "categoría", "funcionalidad", "hidden provider", "hidden model", 2L);
        var result = new ConsumerTranslatedMessageDTO(original, "en", "es", translation);
        when(facade.execute(eq("MSG_WELCOME"), eq("env-id"), any(MessageAccessContext.class), eq("es")))
                .thenReturn(result);
        var requestBody = new ConsumerTranslationRequestDTO();
        requestBody.setTargetLanguage(" es ");
        var exchange = exchange();

        StepVerifier.create(controller.translate("MSG_WELCOME", requestBody, exchange)).verifyComplete();

        assertThat(exchange.getResponse().getBodyAsString().block()).isEqualTo(
                "{\"code\":\"MSG\",\"sourceLanguage\":\"en\",\"targetLanguage\":\"es\","
                        + "\"title\":\"translated\",\"content\":\"translated body\",\"type\":\"tipo\","
                        + "\"category\":\"categoría\",\"functionality\":\"funcionalidad\","
                        + "\"created_at\":\"2025-01-02T03:04:05Z\",\"updated_at\":\"2025-02-03T04:05:06Z\"}");
        assertThat(exchange.getResponse().getHeaders().getCacheControl()).isEqualTo("no-store");
        ArgumentCaptor<MessageAccessContext> context = ArgumentCaptor.forClass(MessageAccessContext.class);
        verify(facade).execute(eq("MSG_WELCOME"), eq("env-id"), context.capture(), eq("es"));
        assertThat(context.getValue().applicationId()).isEqualTo("app-id");
    }

    private static MockServerWebExchange exchange() {
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/messageucolab/v1/messages/consumer/translations?code=MSG_WELCOME")
                        .header("Content-Type", "application/json").build());
        exchange.getAttributes().put(APPLICATION_ID_ATTRIBUTE, "app-id");
        exchange.getAttributes().put(ENVIRONMENT_ID_ATTRIBUTE, "env-id");
        return exchange;
    }
}
