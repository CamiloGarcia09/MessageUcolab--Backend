package co.edu.uco.application.usecase.validator.message.rule;

import co.edu.uco.application.primaryports.dto.message.UpdateMessageDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.application.usecase.validator.specification.Specifications;
import co.edu.uco.application.usecase.validator.specification.impl.TextRequiredSpecification;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;

import java.util.function.Function;

public final class UpdateMessageCatalogIdRequiredRule extends RuleValidator<UpdateMessageDTO> {

    public UpdateMessageCatalogIdRequiredRule(CatalogPort catalogPort,
                                              Function<UpdateMessageDTO, String> idExtractor,
                                              MessageCatalogCodeEnum messageCode) {
        super(catalogPort,
                Specifications.field(idExtractor, new TextRequiredSpecification()),
                messageCode);
    }
}
