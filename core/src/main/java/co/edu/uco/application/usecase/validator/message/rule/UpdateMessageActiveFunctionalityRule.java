package co.edu.uco.application.usecase.validator.message.rule;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.FunctionalityCatalogRepository;
import co.edu.uco.application.secondaryports.repository.MessageStateCatalogRepository;
import co.edu.uco.application.usecase.validator.message.UpdateMessageValidationContext;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;

import static co.edu.uco.application.usecase.domain.catalog.CatalogNames.isActive;
import static co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum.FUN_229;
import static co.edu.uco.crosscutting.helpers.UtilUUID.getUUIDFromString;

public final class UpdateMessageActiveFunctionalityRule extends RuleValidator<UpdateMessageValidationContext> {
    public UpdateMessageActiveFunctionalityRule(CatalogPort catalog, FunctionalityCatalogRepository functionalities,
                                                MessageStateCatalogRepository states) {
        super(catalog, context -> permitsState(context, functionalities, states), FUN_229);
    }

    private static boolean permitsState(UpdateMessageValidationContext context,
            FunctionalityCatalogRepository functionalities, MessageStateCatalogRepository states) {
        var statusId = getUUIDFromString(context.dto().getStatusId());
        boolean activating = states.findAll().stream()
                .anyMatch(state -> state != null && statusId.equals(state.getId()) && isActive(state.getName()));
        if (!activating) {
            return true;
        }
        var functionalityId = getUUIDFromString(context.dto().getFunctionalityId());
        return functionalities.findAllByApplicationId(context.applicationId()).stream()
                .anyMatch(item -> item != null && functionalityId.equals(item.getId()) && isActive(item.getState()));
    }
}
