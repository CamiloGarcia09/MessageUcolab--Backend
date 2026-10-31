package co.edu.uco.infraestructure.primaryadapters.controller;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.MessageImportUseCase;
import co.edu.uco.application.usecase.domain.message.importing.*;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import co.edu.uco.infraestructure.primaryadapters.importing.MessageImportWorkbookAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.APPLICATION_ID_ATTRIBUTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageImportControllerTest {
    private static final String APP = "123e4567-e89b-12d3-a456-426614175106";
    private static final UUID IMPORT_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614175107");
    @Mock private MessageImportUseCase imports;
    @Mock private MessageImportWorkbookAdapter workbook;
    @Mock private CatalogPort catalog;
    @Mock private FilePart file;
    private MessageImportController controller;

    @BeforeEach
    void setUp() {
        controller = new MessageImportController(imports, workbook, catalog);
    }

    @Test
    void requestMappings_exposeMultipartValidationAndJsonConfirmationRoutes() throws Exception {
        var base = MessageImportController.class.getAnnotation(RequestMapping.class);
        var validate = MessageImportController.class.getMethod("validate", FilePart.class, List.class,
                org.springframework.web.server.ServerWebExchange.class).getAnnotation(PostMapping.class);
        var confirm = MessageImportController.class.getMethod("confirm", UUID.class,
                MessageImportController.Confirmation.class, org.springframework.web.server.ServerWebExchange.class)
                .getAnnotation(PostMapping.class);

        assertThat(base.value()).contains("${crosswords.api.path.messages}/imports");
        assertThat(validate.consumes()).contains("multipart/form-data");
        assertThat(confirm.consumes()).contains("application/json");
    }

    @Test
    void validate_bindsFileAndEnvironmentIdsAndDisablesCaching() {
        var exchange = exchange("POST", "/imports/validate");
        var bytes = new byte[]{1, 2, 3};
        var row = new ImportRow(2, "CODE", "function", "type", "category", "state", "A title", "A content value");
        var preview = new MessageImportPreview("preview", APP, List.of(), List.of(), Instant.now().plusSeconds(100));
        when(file.filename()).thenReturn("messages.xlsx");
        when(file.content()).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(bytes)));
        when(workbook.read(bytes, "messages.xlsx", APP)).thenReturn(List.of(row));
        when(imports.validate(List.of(row), List.of("environment-id"), new MessageAccessContext(APP))).thenReturn(preview);

        var response = controller.validate(file, List.of("environment-id"), exchange).block();

        assertThat(response).isNotNull();
        assertThat(response.data()).isSameAs(preview);
        assertThat(exchange.getResponse().getHeaders().getCacheControl()).isEqualTo("no-store");
        verify(workbook).read(bytes, "messages.xlsx", APP);
    }

    @Test
    void confirm_forwardsUuidAndProductionConsent() {
        var exchange = exchange("POST", "/imports/" + IMPORT_ID + "/confirm");
        var receipt = new MessageImportReceipt(IMPORT_ID.toString(), 2, List.of(), Instant.now());
        when(imports.confirm(IMPORT_ID.toString(), true, new MessageAccessContext(APP))).thenReturn(receipt);

        var response = controller.confirm(IMPORT_ID,
                new MessageImportController.Confirmation(true), exchange).block();

        assertThat(response.data()).isSameAs(receipt);
        assertThat(exchange.getResponse().getHeaders().getCacheControl()).isEqualTo("no-store");
        verify(imports).confirm(IMPORT_ID.toString(), true, new MessageAccessContext(APP));
    }

    @Test
    void preview_rejectsUnknownOrExpiredUuidThroughUseCaseDomainError() {
        var exchange = exchange("GET", "/imports/" + IMPORT_ID);
        when(imports.preview(IMPORT_ID.toString(), new MessageAccessContext(APP)))
                .thenThrow(BusinessRuleException.buildUserException("Preview expired"));

        assertThatThrownBy(() -> controller.preview(IMPORT_ID, exchange).block())
                .isInstanceOf(BusinessRuleException.class);
    }

    private static MockServerWebExchange exchange(String method, String path) {
        var builder = switch (method) {
            case "POST" -> MockServerHttpRequest.post(path);
            default -> MockServerHttpRequest.get(path);
        };
        var exchange = MockServerWebExchange.from(builder.build());
        exchange.getAttributes().put(APPLICATION_ID_ATTRIBUTE, APP);
        return exchange;
    }
}
