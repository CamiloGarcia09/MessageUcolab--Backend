package co.edu.uco.application.primaryports.facade.message.impl;

import co.edu.uco.application.primaryports.dto.message.MessageDTO;
import co.edu.uco.application.primaryports.dto.page.PageRequestDTO;
import co.edu.uco.application.primaryports.facade.message.FindMessagesByApplicationUseCaseFacade;
import co.edu.uco.application.primaryports.facade.page.impl.SimplePageFacadeImpl;
import co.edu.uco.application.secondaryports.repository.SimplePage;
import co.edu.uco.application.usecase.handling.HandlingFindMessagesByApplicationPort;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;

public final class FindMessagesByApplicationUseCaseFacadeImpl implements FindMessagesByApplicationUseCaseFacade {
    private final HandlingFindMessagesByApplicationPort handlingFindMessagesByApplicationPort;
    private final SimplePageFacadeImpl simplePageFacadeImpl;
    public FindMessagesByApplicationUseCaseFacadeImpl(
            HandlingFindMessagesByApplicationPort handlingFindMessagesByApplicationPort,
            SimplePageFacadeImpl simplePageFacadeImpl) {
        this.handlingFindMessagesByApplicationPort = handlingFindMessagesByApplicationPort;
        this.simplePageFacadeImpl = simplePageFacadeImpl;
    }
    public SimplePage<MessageDTO> execute(MessageAccessContext context, PageRequestDTO pageDTO) {
        var simplePageRequest = simplePageFacadeImpl.execute(pageDTO);
        return handlingFindMessagesByApplicationPort.execute(context, simplePageRequest);
    }
}
