package co.edu.uco.application.usecase.validator.message.rule;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.SimplePageRequest;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.application.usecase.validator.specification.impl.ValidUuidSpecification;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;

/**
 * El filtro de ambientes es opcional; cuando se envia, cada valor debe ser un UUID valido.
 */
public final class MessageFilterEnvironmentsUuidRule extends RuleValidator<SimplePageRequest> {

    public MessageFilterEnvironmentsUuidRule(final CatalogPort catalogPort) {
        super(catalogPort,
                request -> request == null || request.getFilter() == null
                        || request.getFilter().getEnvironmentIds() == null
                        || request.getFilter().getEnvironmentIds().stream()
                        .allMatch(new ValidUuidSpecification()::isSatisfiedBy),
                MessageCatalogCodeEnum.FUN_038);
    }
}
