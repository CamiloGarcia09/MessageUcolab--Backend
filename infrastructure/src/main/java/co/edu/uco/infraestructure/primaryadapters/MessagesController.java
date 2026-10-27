package co.edu.uco.infraestructure.primaryadapters;

import co.edu.uco.application.primaryports.dto.message.TranslateMessageDTO;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.MediaType;

import static co.edu.uco.infraestructure.config.InfrastructureConstant.MESSAGE_CODE_PARAMETER;

@RequestMapping("${crosswords.api.path.messages}")
public interface MessagesController {

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    reactor.core.publisher.Flux<org.springframework.http.codec.ServerSentEvent<co.edu.uco.application.secondaryports.event.MessageEvent>> events(
            ServerWebExchange exchange);

    @GetMapping(
            params = "!code",
            produces = {"application/json", "application/yaml", "application/xml", "text/plain", "text/html"}
    )
    Mono<Void> findByEnvironmentAndMessage(
            @RequestParam(value = "page", required = false) String page,
            @RequestParam(value = "size", required = false) String size,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "columnSort", required = false) String columnSort,
            @RequestParam(value = "filterCode", required = false) String filterCode,
            @RequestParam(value = "filterTitle", required = false) String filterTitle,
            @RequestParam(value = "filterCategory", required = false) String filterCategory,
            @RequestParam(value = "filterFunctionality", required = false) String filterFunctionality,
            @RequestParam(value = "filterType", required = false) String filterType,
            @RequestParam(value = "filterStatus", required = false) String filterStatus,
            @RequestParam(value = "filterEnvironments", required = false) String filterEnvironments,
            @RequestParam(value = "filterStatusId", required = false) String filterStatusId,
            @RequestParam(value = "filterFunctionalityId", required = false) String filterFunctionalityId,
            ServerWebExchange exchange
    );

    default Mono<Void> findByEnvironmentAndMessage(String page, String size, String sort, String columnSort,
                                                  String filterCode, String filterTitle, String filterCategory,
                                                  String filterFunctionality, String filterType, String filterStatus,
                                                  String filterEnvironments, ServerWebExchange exchange) {
        return findByEnvironmentAndMessage(page, size, sort, columnSort, filterCode, filterTitle,
                filterCategory, filterFunctionality, filterType, filterStatus, filterEnvironments,
                null, null, exchange);
    }

    @GetMapping(
            params = "code",
            produces = {"application/json", "application/yaml", "application/xml", "text/plain", "text/html"}
    )
    Mono<Void> findByCodeMessageAndEnvironment(
            @RequestParam("code") String messageCode,
            ServerWebExchange exchange
    );

    @PostMapping(
            value = "/{messageCode}/translations",
            consumes = "application/json",
            produces = {"application/json", "application/yaml", "application/xml", "text/plain", "text/html"}
    )
    Mono<Void> translateByCodeMessageAndEnvironment(
            @PathVariable(MESSAGE_CODE_PARAMETER) String messageCode,
            @RequestBody TranslateMessageDTO translateMessageDTO,
            ServerWebExchange exchange
    );
}
