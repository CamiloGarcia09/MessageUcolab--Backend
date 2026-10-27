package co.edu.uco.infraestructure.primaryadapters;

import co.edu.uco.application.primaryports.dto.message.CreateMessageDTO;
import co.edu.uco.application.primaryports.dto.message.TranslateMessageDTO;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class MessageEndpointsRequestMappingTest {

    @Nested
    class MessagesControllerEndpoints {

        @Test
        void classMapping_isAnchoredToTheV1MessagesResource() {
            RequestMapping mapping = MessagesController.class.getAnnotation(RequestMapping.class);

            assertThat(mapping).isNotNull();
            assertThat(mapping.value()).containsExactly("${crosswords.api.path.messages}");
        }

        @Test
        void listEndpoint_isGetWithoutCodeParameter() throws NoSuchMethodException {
            Method method = MessagesController.class.getMethod("findMessagesByApplication",
                    String.class, String.class, String.class, String.class,
                    String.class, String.class, String.class, String.class, String.class, String.class,
                    String.class, String.class, String.class, ServerWebExchange.class);
            GetMapping mapping = method.getAnnotation(GetMapping.class);
            RequestParam statusId = parameterAnnotation(method, 11, RequestParam.class);
            RequestParam functionalityId = parameterAnnotation(method, 12, RequestParam.class);

            assertThat(mapping).isNotNull();
            assertThat(statusId).isNotNull();
            assertThat(functionalityId).isNotNull();
            assertAll(
                    () -> assertThat(mapping.params()).containsExactly("!code"),
                    () -> assertThat(mapping.value()).isEmpty(),
                    () -> assertThat(statusId.value()).isEqualTo("filterStatusId"),
                    () -> assertThat(statusId.required()).isFalse(),
                    () -> assertThat(functionalityId.value()).isEqualTo("filterFunctionalityId"),
                    () -> assertThat(functionalityId.required()).isFalse());
        }

        @Test
        void findByCodeEndpoint_isGetBoundToTheCodeQueryParameter() throws NoSuchMethodException {
            Method method = MessagesController.class.getMethod("findMessageByCodeAndApplication",
                    String.class, ServerWebExchange.class);
            GetMapping mapping = method.getAnnotation(GetMapping.class);
            RequestParam codeParameter = parameterAnnotation(method, 0, RequestParam.class);

            assertThat(mapping).isNotNull();
            assertThat(codeParameter).isNotNull();
            assertAll(
                    () -> assertThat(mapping.params()).containsExactly("code"),
                    () -> assertThat(mapping.value()).isEmpty(),
                    () -> assertThat(codeParameter.value()).isEqualTo("code"));
        }

        @Test
        void translateEndpoint_isPostWithTranslateMessageBody() throws NoSuchMethodException {
            Method method = MessagesController.class.getMethod("translateMessageByCodeAndApplication",
                    String.class, TranslateMessageDTO.class, ServerWebExchange.class);
            PostMapping mapping = method.getAnnotation(PostMapping.class);
            RequestBody bodyParameter = parameterAnnotation(method, 1, RequestBody.class);

            assertThat(mapping).isNotNull();
            assertThat(bodyParameter).isNotNull();
            assertAll(
                    () -> assertThat(mapping.value()).containsExactly("/{messageCode}/translations"),
                    () -> assertThat(mapping.consumes()).containsExactly("application/json"),
                    () -> assertThat(method.getParameters()[1].getType()).isEqualTo(TranslateMessageDTO.class));
        }
    }

    @Nested
    class CreateMessageControllerEndpoints {

        @Test
        void classMapping_isAnchoredToTheV1MessagesResource() {
            RequestMapping mapping = CreateMessageController.class.getAnnotation(RequestMapping.class);

            assertThat(mapping).isNotNull();
            assertThat(mapping.value()).containsExactly("${crosswords.api.path.messages}");
        }

        @Test
        void createEndpoint_isPostOnTheCollectionWithCreateMessageBody() throws NoSuchMethodException {
            Method method = CreateMessageController.class.getMethod("createMessage",
                    CreateMessageDTO.class, ServerWebExchange.class);
            PostMapping mapping = method.getAnnotation(PostMapping.class);
            RequestBody bodyParameter = parameterAnnotation(method, 0, RequestBody.class);

            assertThat(mapping).isNotNull();
            assertThat(bodyParameter).isNotNull();
            assertAll(
                    () -> assertThat(mapping.value()).isEmpty(),
                    () -> assertThat(mapping.consumes()).containsExactly("application/json"),
                    () -> assertThat(method.getParameters()[0].getType()).isEqualTo(CreateMessageDTO.class));
        }
    }

    private static <A extends Annotation> A parameterAnnotation(Method method, int parameterIndex,
                                                                Class<A> annotationType) {
        for (Annotation annotation : method.getParameterAnnotations()[parameterIndex]) {
            if (annotationType.isInstance(annotation)) {
                return annotationType.cast(annotation);
            }
        }
        return null;
    }
}
