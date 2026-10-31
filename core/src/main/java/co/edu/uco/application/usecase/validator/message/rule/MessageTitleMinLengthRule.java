package co.edu.uco.application.usecase.validator.message.rule;

import co.edu.uco.application.primaryports.dto.message.CreateMessageDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.application.usecase.validator.specification.Specifications;
import co.edu.uco.application.usecase.validator.specification.impl.TextMinLengthSpecification;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;

public final class MessageTitleMinLengthRule extends RuleValidator<CreateMessageDTO> {

    private static final int TITLE_MIN_LENGTH = 10;

    public MessageTitleMinLengthRule(CatalogPort catalogPort) {
        super(catalogPort,
                Specifications.field(CreateMessageDTO::getTitle, new TextMinLengthSpecification(TITLE_MIN_LENGTH)),
                MessageCatalogCodeEnum.FUN_020);
    }
}