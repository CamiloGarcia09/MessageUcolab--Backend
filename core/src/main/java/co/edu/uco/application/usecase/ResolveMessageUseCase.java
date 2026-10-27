package co.edu.uco.application.usecase;

import co.edu.uco.application.primaryports.dto.message.ResolvedMessageDTO;
import co.edu.uco.application.primaryports.facade.message.ResolveMessageUseCaseFacade;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.ResolveMessageRepository;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;
import co.edu.uco.application.usecase.security.MessageEnvironmentResolver;
import co.edu.uco.application.usecase.validator.message.FindMessageCodeValidator;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.NotFoundException;

public final class ResolveMessageUseCase implements ResolveMessageUseCaseFacade {

    private final FindMessageCodeValidator codeValidator;
    private final MessageEnvironmentResolver environmentResolver;
    private final ResolveMessageRepository repository;
    private final CatalogPort catalogPort;

    public ResolveMessageUseCase(FindMessageCodeValidator codeValidator,
                                 MessageEnvironmentResolver environmentResolver,
                                 ResolveMessageRepository repository, CatalogPort catalogPort) {
        this.codeValidator = codeValidator;
        this.environmentResolver = environmentResolver;
        this.repository = repository;
        this.catalogPort = catalogPort;
    }

    @Override
    public ResolvedMessageDTO execute(String code, String requestedEnvironmentId, MessageAccessContext context) {
        codeValidator.validate(code);
        var environmentId = environmentResolver.resolve(context, requestedEnvironmentId);
        var message = repository.findAvailableByCodeAndEnvironment(code, environmentId)
                .orElseThrow(() -> NotFoundException.buildUserException(
                        catalogPort.getMessage(MessageCatalogCodeEnum.FUN_012.getCode())
                                .formatted(code, environmentId)));
        return new ResolvedMessageDTO(message.getCode(), message.getTitle(), message.getContent());
    }
}
