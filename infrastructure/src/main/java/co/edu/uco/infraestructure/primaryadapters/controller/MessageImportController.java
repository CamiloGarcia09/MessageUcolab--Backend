package co.edu.uco.infraestructure.primaryadapters.controller;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.MessageImportUseCase;
import co.edu.uco.application.usecase.domain.message.importing.MessageImportPreview;
import co.edu.uco.application.usecase.domain.message.importing.MessageImportReceipt;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import co.edu.uco.infraestructure.primaryadapters.ReactiveHttpResponse;
import co.edu.uco.infraestructure.primaryadapters.importing.MessageImportWorkbookAdapter;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.*;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.util.List;
import java.util.UUID;
import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.IMP_001;
import static co.edu.uco.infraestructure.config.InfrastructureConstant.APPLICATION_ID_ATTRIBUTE;

@RestController
@RequestMapping("${crosswords.api.path.messages}/imports")
public final class MessageImportController {
    private static final String EXCEL = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String UUID_PATH_VARIABLE = "{importId:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}";
    private final MessageImportUseCase imports;
    private final MessageImportWorkbookAdapter workbook;
    private final CatalogPort catalog;

    public record Data<T>(T data) { }
    public record Confirmation(boolean productionConfirmed) { }

    public MessageImportController(MessageImportUseCase imports, MessageImportWorkbookAdapter workbook, CatalogPort catalog) {
        this.imports = imports;
        this.workbook = workbook;
        this.catalog = catalog;
    }

    @GetMapping(value = "/template", produces = EXCEL)
    public Mono<ResponseEntity<byte[]>> template(ServerWebExchange exchange) {
        return ReactiveHttpResponse.blocking(() -> {
            var context = context(exchange);
            return download(workbook.template(imports.references(context), context.applicationId()), "plantilla-mensajes.xlsx");
        });
    }

    @PostMapping(value = "/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Data<MessageImportPreview>> validate(@RequestPart("file") FilePart file,
            @RequestParam List<String> environmentIds, ServerWebExchange exchange) {
        exchange.getResponse().getHeaders().setCacheControl("no-store");
        return DataBufferUtils.join(file.content(), MessageImportWorkbookAdapter.MAX_BYTES)
                .switchIfEmpty(Mono.defer(() -> Mono.error(invalidFile())))
                .map(buffer -> {
                    try {
                        var bytes = new byte[buffer.readableByteCount()];
                        buffer.read(bytes);
                        return bytes;
                    } finally {
                        DataBufferUtils.release(buffer);
                    }
                })
                .flatMap(bytes -> ReactiveHttpResponse.blocking(() -> {
                    var context = context(exchange);
                    var rows = workbook.read(bytes, file.filename(), context.applicationId());
                    return new Data<>(imports.validate(rows, environmentIds, context));
                }))
                .onErrorMap(DataBufferLimitException.class, error -> invalidFile());
    }

    @GetMapping(value = "/" + UUID_PATH_VARIABLE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Data<MessageImportPreview>> preview(@PathVariable UUID importId, ServerWebExchange exchange) {
        exchange.getResponse().getHeaders().setCacheControl("no-store");
        return ReactiveHttpResponse.blocking(() -> new Data<>(imports.preview(importId.toString(), context(exchange))));
    }

    @GetMapping(value = "/" + UUID_PATH_VARIABLE + "/errors", produces = EXCEL)
    public Mono<ResponseEntity<byte[]>> report(@PathVariable UUID importId, ServerWebExchange exchange) {
        return ReactiveHttpResponse.blocking(() -> download(
                workbook.report(imports.preview(importId.toString(), context(exchange))), "errores-importacion.xlsx"));
    }

    @GetMapping(value = "/" + UUID_PATH_VARIABLE + "/result", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Data<MessageImportReceipt>> result(@PathVariable UUID importId, ServerWebExchange exchange) {
        exchange.getResponse().getHeaders().setCacheControl("no-store");
        return ReactiveHttpResponse.blocking(() -> new Data<>(imports.result(importId.toString(), context(exchange)).orElse(null)));
    }

    @PostMapping(value = "/" + UUID_PATH_VARIABLE + "/confirm", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Data<MessageImportReceipt>> confirm(@PathVariable UUID importId, @RequestBody Confirmation body,
                                                    ServerWebExchange exchange) {
        exchange.getResponse().getHeaders().setCacheControl("no-store");
        return ReactiveHttpResponse.blocking(() -> new Data<>(imports.confirm(importId.toString(), body.productionConfirmed(), context(exchange))));
    }

    private ResponseEntity<byte[]> download(byte[] bytes, String filename) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(EXCEL))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .cacheControl(CacheControl.noStore()).body(bytes);
    }

    private MessageAccessContext context(ServerWebExchange exchange) {
        return new MessageAccessContext(exchange.getAttribute(APPLICATION_ID_ATTRIBUTE));
    }

    private BusinessRuleException invalidFile() {
        return BusinessRuleException.buildUserException(catalog.getMessage(IMP_001.getCode()));
    }
}
