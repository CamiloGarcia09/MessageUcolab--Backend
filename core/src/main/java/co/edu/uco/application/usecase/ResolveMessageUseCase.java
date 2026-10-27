package co.edu.uco.application.usecase;

import co.edu.uco.application.primaryports.dto.message.ResolvedMessageDTO;
import co.edu.uco.application.common.catalog.strategy.MessageCatalogStrategy;
import co.edu.uco.application.primaryports.facade.message.ResolveMessageUseCaseFacade;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;
import co.edu.uco.application.usecase.security.MessageEnvironmentResolver;
import co.edu.uco.application.usecase.validator.message.FindMessageCodeValidator;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.NotFoundException;

public final class ResolveMessageUseCase implements ResolveMessageUseCaseFacade {

    private final FindMessageCodeValidator codeValidator;
    private final MessageEnvironmentResolver environmentResolver;
    private final MessageCatalogStrategy messageCatalogStrategy;
    private final CatalogPort catalogPort;

    public ResolveMessageUseCase(FindMessageCodeValidator codeValidator,
                                 MessageEnvironmentResolver environmentResolver,
                                  MessageCatalogStrategy messageCatalogStrategy, CatalogPort catalogPort) {
        this.codeValidator = codeValidator;
        this.environmentResolver = environmentResolver;
        this.messageCatalogStrategy = messageCatalogStrategy;
        this.catalogPort = catalogPort;
    }

    @Override
    public ResolvedMessageDTO execute(String code, String requestedEnvironmentId, MessageAccessContext context) {
        codeValidator.validate(code);
        var environmentId = environmentResolver.resolve(context, requestedEnvironmentId);
        var message = messageCatalogStrategy.getAvailableMessageByCodeAndEnvironment(
                        code, context.applicationId(), environmentId)
                .orElseThrow(() -> NotFoundException.buildUserException(
                        catalogPort.getMessage(MessageCatalogCodeEnum.FUN_218.getCode())
                                .formatted(code, context.applicationId(), environmentId)));
        return new ResolvedMessageDTO(message.getCode(), message.getTitle(), message.getContent(),
                message.getType().getName(), message.getCategory().getName(),
                message.getFunctionality().getName(), message.getCreatedAt(), message.getUpdatedAt());
    }
}
