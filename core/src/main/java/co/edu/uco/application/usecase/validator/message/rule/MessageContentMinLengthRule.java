package co.edu.uco.application.usecase.validator.message.rule;

import co.edu.uco.application.primaryports.dto.message.CreateMessageDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.application.usecase.validator.specification.Specifications;
import co.edu.uco.application.usecase.validator.specification.impl.TextMinLengthSpecification;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;

public final class MessageContentMinLengthRule extends RuleValidator<CreateMessageDTO> {

    private static final int CONTENT_MIN_LENGTH = 10;

    public MessageContentMinLengthRule(CatalogPort catalogPort) {
        super(catalogPort,
                Specifications.field(CreateMessageDTO::getContent, new TextMinLengthSpecification(CONTENT_MIN_LENGTH)),
                MessageCatalogCodeEnum.FUN_018);
    }
}