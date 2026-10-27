package co.edu.uco.application.primaryports.facade.message.impl;

import co.edu.uco.application.primaryports.dto.message.MessageDTO;
import co.edu.uco.application.usecase.handling.HandlingFindMessageByCodeAndEnvironmentPort;
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
class FindMessageByCodeAndEnvironmentUseCaseFacadeImplTest {

    @Mock
    private HandlingFindMessageByCodeAndEnvironmentPort handlingPort;

    @InjectMocks
    private FindMessageByCodeAndEnvironmentUseCaseFacadeImpl facade;

    @Test
    void execute_delegatesToHandlingPortAndReturnsResult() {
        MessageDTO expected = MessageDTO.create("CODE001", "title", "content", "type", "category", "func");
        MessageAccessContext context = new MessageAccessContext("app-1");
        when(handlingPort.execute("CODE001", context)).thenReturn(expected);

        MessageDTO result = facade.execute("CODE001", context);

        assertThat(result).isSameAs(expected);
        verify(handlingPort).execute("CODE001", context);
    }
}
