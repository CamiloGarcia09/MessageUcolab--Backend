package co.edu.uco.application.primaryports.facade.message.impl;

import co.edu.uco.application.primaryports.dto.message.TranslatedMessageDTO;
import co.edu.uco.application.usecase.handling.HandlingTranslateMessageByCodeAndApplicationPort;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TranslateMessageByCodeAndApplicationUseCaseFacadeImplTest {

    @Mock
    private HandlingTranslateMessageByCodeAndApplicationPort handlingPort;

    @InjectMocks
    private TranslateMessageByCodeAndApplicationUseCaseFacadeImpl facade;

    @Test
    void execute_delegatesToHandlingPortAndReturnsResult() {
        TranslatedMessageDTO expected = TranslatedMessageDTO.create(
                "CODE001", "en", "es", "title", "content",
                "título", "contenido", "original type", "translated type",
                "original category", "translated category", "original function", "translated function",
                "provider", "model", 100L
        );
        MessageAccessContext context = new MessageAccessContext("app-1");
        when(handlingPort.execute("CODE001", context, "en", "es")).thenReturn(expected);

        TranslatedMessageDTO result = facade.execute("CODE001", context, "en", "es");

        assertThat(result).isSameAs(expected);
        verify(handlingPort).execute("CODE001", context, "en", "es");
    }
}
