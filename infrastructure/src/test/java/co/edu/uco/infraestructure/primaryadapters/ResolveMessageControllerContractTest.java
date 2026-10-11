package co.edu.uco.infraestructure.primaryadapters;

import org.junit.jupiter.api.Test;
import co.edu.uco.application.primaryports.dto.message.ResolvedMessageDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import static org.assertj.core.api.Assertions.assertThat;

class ResolveMessageControllerContractTest {
    @Test
    void resolve_declaresConsumerGetRouteAndRequiredCodeParameter() throws NoSuchMethodException {
        var method = ResolveMessageController.class.getMethod("resolve", String.class,
                jakarta.servlet.http.HttpServletRequest.class, jakarta.servlet.http.HttpServletResponse.class);
        var route = method.getAnnotation(GetMapping.class);
        var code = method.getParameters()[0].getAnnotation(RequestParam.class);

        assertThat(ResolveMessageController.class.getAnnotation(RequestMapping.class).value())
                .containsExactly("${crosswords.api.path.messages}");
        assertThat(route.value()).containsExactly("/resolve");
        assertThat(route.produces()).containsExactly("application/json");
        assertThat(code.value()).isEqualTo("code");
        assertThat(code.required()).isTrue();
        assertThat(method.getReturnType().getSimpleName()).isEqualTo("ResolvedMessageDTO");
    }

    @Test
    void dto_serializesAsExactlyThreeTopLevelConsumerFields() throws com.fasterxml.jackson.core.JsonProcessingException {
        var json = new ObjectMapper().writeValueAsString(new ResolvedMessageDTO("CODE", "Title", "Body"));

        assertThat(json).isEqualTo("{\"code\":\"CODE\",\"title\":\"Title\",\"content\":\"Body\"}");
    }
}
