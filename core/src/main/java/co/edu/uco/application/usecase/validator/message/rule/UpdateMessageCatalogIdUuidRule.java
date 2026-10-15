package co.edu.uco.application.usecase.validator.message.rule;

import co.edu.uco.application.primaryports.dto.message.UpdateMessageDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.application.usecase.validator.specification.Specifications;
import co.edu.uco.application.usecase.validator.specification.impl.ValidUuidSpecification;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;

import java.util.function.Function;

public final class UpdateMessageCatalogIdUuidRule extends RuleValidator<UpdateMessageDTO> {

    public UpdateMessageCatalogIdUuidRule(CatalogPort catalogPort,
                                          Function<UpdateMessageDTO, String> idExtractor) {
        super(catalogPort,
                Specifications.field(idExtractor, new ValidUuidSpecification()),
                MessageCatalogCodeEnum.FUN_038);
    }
}
