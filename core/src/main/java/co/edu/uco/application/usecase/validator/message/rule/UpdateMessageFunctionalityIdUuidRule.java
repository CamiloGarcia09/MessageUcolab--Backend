package co.edu.uco.application.usecase.validator.message.rule;

import co.edu.uco.application.primaryports.dto.message.UpdateMessageDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.application.usecase.validator.specification.Specifications;
import co.edu.uco.application.usecase.validator.specification.impl.ValidUuidSpecification;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;

public final class UpdateMessageFunctionalityIdUuidRule extends RuleValidator<UpdateMessageDTO> {

    public UpdateMessageFunctionalityIdUuidRule(CatalogPort catalogPort) {
        super(catalogPort,
                Specifications.field(UpdateMessageDTO::getFunctionalityId, new ValidUuidSpecification()),
                MessageCatalogCodeEnum.FUN_038);
    }
}
