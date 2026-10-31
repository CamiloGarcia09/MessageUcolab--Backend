package co.edu.uco.infraestructure.primaryadapters;

import co.edu.uco.application.primaryports.dto.message.ConsumerTranslationRequestDTO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RequestMapping("${crosswords.api.path.messages}")
public interface ConsumerMessageTranslationController {
    @PostMapping(value = "/consumer/translations", consumes = "application/json", produces = "application/json")
    Mono<Void> translate(@RequestParam("code") String code, @RequestBody ConsumerTranslationRequestDTO request,
                          ServerWebExchange exchange);
}
