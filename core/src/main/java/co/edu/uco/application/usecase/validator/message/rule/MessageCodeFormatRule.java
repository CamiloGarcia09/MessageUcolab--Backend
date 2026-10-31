package co.edu.uco.application.usecase.validator.message.rule;

import co.edu.uco.application.primaryports.dto.message.CreateMessageDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.application.usecase.validator.specification.Specifications;
import co.edu.uco.application.usecase.validator.specification.impl.RegexMatchSpecification;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;

import static co.edu.uco.crosscutting.helpers.UtilMessageCode.SUFFIX_PATTERN;

public final class MessageCodeFormatRule extends RuleValidator<CreateMessageDTO> {

    public MessageCodeFormatRule(CatalogPort catalogPort) {
        super(catalogPort,
                Specifications.field(CreateMessageDTO::getCode,
                        new RegexMatchSpecification(SUFFIX_PATTERN)),
                MessageCatalogCodeEnum.FUN_199);
    }
}
