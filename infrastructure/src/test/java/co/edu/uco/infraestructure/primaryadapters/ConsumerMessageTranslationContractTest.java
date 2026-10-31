package co.edu.uco.infraestructure.primaryadapters;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import static org.assertj.core.api.Assertions.assertThat;

class ConsumerMessageTranslationContractTest {
    @Test
    void endpoint_declaresConsumerTranslationPostAndJsonContract() throws NoSuchMethodException {
        var mapping = ConsumerMessageTranslationController.class.getAnnotation(RequestMapping.class);
        var method = ConsumerMessageTranslationController.class.getMethod("translate", String.class,
                co.edu.uco.application.primaryports.dto.message.ConsumerTranslationRequestDTO.class,
                org.springframework.web.server.ServerWebExchange.class);
        var post = method.getAnnotation(PostMapping.class);

        assertThat(mapping.value()).containsExactly("${crosswords.api.path.messages}");
        assertThat(post.value()).containsExactly("/consumer/translations");
        assertThat(post.consumes()).containsExactly("application/json");
        assertThat(post.produces()).containsExactly("application/json");
        assertThat(method.getParameters()[0].getAnnotation(RequestParam.class).value()).isEqualTo("code");
    }
}
