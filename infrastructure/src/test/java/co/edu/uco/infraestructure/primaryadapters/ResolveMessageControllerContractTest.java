package co.edu.uco.infraestructure.primaryadapters;
import com.fasterxml.jackson.databind.ObjectMapper;
import co.edu.uco.application.primaryports.dto.message.ResolvedMessageDTO;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.*;
import static org.assertj.core.api.Assertions.assertThat;
class ResolveMessageControllerContractTest {
 @Test void route_usesReactiveExchangeContract() throws Exception {
  var method=ResolveMessageController.class.getMethod("resolve",String.class,org.springframework.web.server.ServerWebExchange.class);
  assertThat(method.getAnnotation(GetMapping.class).value()).containsExactly("/resolve");
  assertThat(method.getParameterTypes()[1]).isEqualTo(org.springframework.web.server.ServerWebExchange.class);
  assertThat(method.getReturnType()).isEqualTo(reactor.core.publisher.Mono.class);
 }
 @Test void dtoSerialization_isStable() throws Exception {
  assertThat(new ObjectMapper().writeValueAsString(new ResolvedMessageDTO("CODE","Title","Body")))
   .isEqualTo("{\"code\":\"CODE\",\"title\":\"Title\",\"content\":\"Body\"}");
 }
}
