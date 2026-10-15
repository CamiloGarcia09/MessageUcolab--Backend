package co.edu.uco.application.usecase.validator.message.rule;

import co.edu.uco.application.primaryports.dto.message.UpdateMessageDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.application.usecase.validator.specification.Specifications;
import co.edu.uco.application.usecase.validator.specification.impl.TextRequiredSpecification;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;

public final class UpdateMessageContentRequiredRule extends RuleValidator<UpdateMessageDTO> {

    public UpdateMessageContentRequiredRule(CatalogPort catalogPort) {
        super(catalogPort,
                Specifications.field(UpdateMessageDTO::getContent, new TextRequiredSpecification()),
                MessageCatalogCodeEnum.FUN_017);
    }
}
