package co.edu.uco.application.usecase.validator.message.environment.rule;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.validator.message.environment.MessageEnvironmentSelectionContext;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;

import static co.edu.uco.crosscutting.helpers.UtilUUID.getUUIDFromString;

/**
 * Se ejecuta despues de la regla UUID: compara los identificadores normalizados.
 */
public final class MessageEnvironmentsDuplicatedRule extends RuleValidator<MessageEnvironmentSelectionContext> {

    public MessageEnvironmentsDuplicatedRule(final CatalogPort catalogPort) {
        super(catalogPort,
                context -> context != null && context.environmentIds() != null
                        && context.environmentIds().stream().map(id -> getUUIDFromString(id)).distinct().count()
                        == context.environmentIds().size(),
                MessageCatalogCodeEnum.FUN_213);
    }
}
