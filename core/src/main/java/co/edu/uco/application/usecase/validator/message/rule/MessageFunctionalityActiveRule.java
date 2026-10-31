package co.edu.uco.application.usecase.validator.message.rule;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.FunctionalityCatalogRepository;
import co.edu.uco.application.usecase.validator.message.CreateMessageValidationContext;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.application.usecase.validator.specification.impl.ActiveFunctionalitySpecification;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;

import static co.edu.uco.crosscutting.helpers.UtilUUID.getUUIDFromString;

public final class MessageFunctionalityActiveRule extends RuleValidator<CreateMessageValidationContext> {

    public MessageFunctionalityActiveRule(CatalogPort catalogPort,
                                         FunctionalityCatalogRepository functionalityRepository) {
        super(catalogPort, context -> isActive(context, functionalityRepository), MessageCatalogCodeEnum.FUN_217);
    }

    private static boolean isActive(CreateMessageValidationContext context,
                                    FunctionalityCatalogRepository repository) {
        final var applicationId = getUUIDFromString(context.applicationId());
        final var functionalityId = getUUIDFromString(context.dto().getFunctionalityId());
        final var functionalities = repository.findAllByApplicationId(applicationId.toString());
        return functionalities != null && functionalities.stream()
                .filter(functionality -> functionality != null && functionalityId.equals(functionality.getId()))
                .filter(functionality -> functionality.getApplication() != null
                        && applicationId.equals(functionality.getApplication().getId()))
                .anyMatch(new ActiveFunctionalitySpecification()::isSatisfiedBy);
    }
}
