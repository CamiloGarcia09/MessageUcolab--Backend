package co.edu.uco.application.usecase.validator.message.rule;

import co.edu.uco.application.primaryports.dto.message.UpdateMessageDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.application.usecase.validator.specification.Specifications;
import co.edu.uco.application.usecase.validator.specification.impl.TextMaxLengthSpecification;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;

public final class UpdateMessageTitleMaxLengthRule extends RuleValidator<UpdateMessageDTO> {

    private static final int TITLE_MAX_LENGTH = 50;

    public UpdateMessageTitleMaxLengthRule(CatalogPort catalogPort) {
        super(catalogPort,
                Specifications.field(UpdateMessageDTO::getTitle, new TextMaxLengthSpecification(TITLE_MAX_LENGTH)),
                MessageCatalogCodeEnum.FUN_021);
    }
}
