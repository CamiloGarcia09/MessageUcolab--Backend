package co.edu.uco.application.usecase.validator.message.rule;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.entity.FunctionalityData;
import co.edu.uco.application.secondaryports.repository.FunctionalityCatalogRepository;
import co.edu.uco.application.usecase.validator.message.CreateMessageValidationContext;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.ForbiddenException;

import java.util.List;
import java.util.UUID;

import static co.edu.uco.crosscutting.helpers.UtilObject.isNullObject;
import static co.edu.uco.crosscutting.helpers.UtilUUID.getStringFromUUID;
import static co.edu.uco.crosscutting.helpers.UtilUUID.getUUIDFromString;
import static co.edu.uco.crosscutting.helpers.UtilUUID.isEqual;

public final class MessageFunctionalityBelongsApplicationRule
        extends RuleValidator<CreateMessageValidationContext> {

    public MessageFunctionalityBelongsApplicationRule(CatalogPort catalogPort,
                                                       FunctionalityCatalogRepository functionalityRepository) {
        super(catalogPort,
                context -> belongsToApplication(context, functionalityRepository),
                MessageCatalogCodeEnum.FUN_146,
                ForbiddenException::buildUserException);
    }

    private static boolean belongsToApplication(CreateMessageValidationContext context,
                                                FunctionalityCatalogRepository functionalityRepository) {
        UUID applicationId = getUUIDFromString(context.applicationId());
        UUID functionalityId = getUUIDFromString(context.dto().getFunctionalityId());
        List<FunctionalityData> functionalities =
                functionalityRepository.findAllByApplicationId(getStringFromUUID(applicationId));
        return !isNullObject(functionalities) && functionalities.stream()
                .anyMatch(functionality -> belongsToApplication(functionality, functionalityId, applicationId));
    }

    private static boolean belongsToApplication(FunctionalityData functionality, UUID functionalityId,
                                                UUID applicationId) {
        return !isNullObject(functionality)
                && isEqual(functionalityId, functionality.getId())
                && !isNullObject(functionality.getApplication())
                && isEqual(applicationId, functionality.getApplication().getId());
    }
}
