package co.edu.uco.application.usecase.validator.message.rule;

import co.edu.uco.application.primaryports.dto.message.CreateMessageDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.application.usecase.validator.specification.Specifications;
import co.edu.uco.application.usecase.validator.specification.impl.TextMaxLengthSpecification;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;

import static co.edu.uco.crosscutting.helpers.UtilMessageCode.MAX_SUFFIX_LENGTH;

public final class MessageCodeMaxLengthRule extends RuleValidator<CreateMessageDTO> {

    public MessageCodeMaxLengthRule(CatalogPort catalogPort) {
        super(catalogPort,
                Specifications.field(CreateMessageDTO::getCode,
                        new TextMaxLengthSpecification(MAX_SUFFIX_LENGTH)),
                MessageCatalogCodeEnum.FUN_198);
    }
}
