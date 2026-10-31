package co.edu.uco.application.usecase.validator.message.rule;

import co.edu.uco.application.primaryports.dto.message.CreateMessageDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.application.usecase.validator.specification.Specifications;
import co.edu.uco.application.usecase.validator.specification.impl.TextMaxLengthSpecification;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;

public final class MessageContentMaxLengthRule extends RuleValidator<CreateMessageDTO> {

    private static final int CONTENT_MAX_LENGTH = 100;

    public MessageContentMaxLengthRule(CatalogPort catalogPort) {
        super(catalogPort,
                Specifications.field(CreateMessageDTO::getContent, new TextMaxLengthSpecification(CONTENT_MAX_LENGTH)),
                MessageCatalogCodeEnum.FUN_019);
    }
}