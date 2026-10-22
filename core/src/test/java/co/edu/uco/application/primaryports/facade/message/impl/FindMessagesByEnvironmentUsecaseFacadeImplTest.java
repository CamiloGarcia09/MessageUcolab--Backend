package co.edu.uco.application.primaryports.facade.message.impl;

import co.edu.uco.application.primaryports.dto.message.MessageDTO;
import co.edu.uco.application.primaryports.dto.page.PageRequestDTO;
import co.edu.uco.application.primaryports.facade.page.impl.SimplePageFacadeImpl;
import co.edu.uco.application.secondaryports.repository.SimplePage;
import co.edu.uco.application.secondaryports.repository.SimplePageRequest;
import co.edu.uco.application.usecase.handling.HandlingFindMessageEnvironmentPort;
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
class FindMessagesByEnvironmentUsecaseFacadeImplTest {

    @Mock
    private HandlingFindMessageEnvironmentPort handlingFindMessageEnvironmentPort;
    @Mock
    private SimplePageFacadeImpl simplePageFacadeImpl;

    @InjectMocks
    private FindMessagesByEnvironmentUsecaseFacadeImpl facade;

    @Test
    void execute_buildsPageRequestAndDelegates() {
        PageRequestDTO pageDTO = new PageRequestDTO();
        SimplePageRequest request = new SimplePageRequest();
        MessageAccessContext context = new MessageAccessContext("app-1");
        when(simplePageFacadeImpl.execute(pageDTO)).thenReturn(request);

        SimplePage<MessageDTO> page = SimplePage.of(java.util.List.of(), 1, 10, 0, 0);
        when(handlingFindMessageEnvironmentPort.execute(context, request)).thenReturn(page);

        SimplePage<MessageDTO> result = facade.execute(context, pageDTO);

        assertThat(result).isSameAs(page);
        verify(simplePageFacadeImpl).execute(pageDTO);
        verify(handlingFindMessageEnvironmentPort).execute(context, request);
    }
}
