package co.edu.uco.application.usecase.validator.message.rule;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.MessageCodeQueryPort;
import co.edu.uco.application.usecase.validator.message.CreateMessageValidationContext;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.ConflictException;

import static co.edu.uco.crosscutting.helpers.UtilMessageCode.normalize;

public final class MessageCodeDuplicatedRule extends RuleValidator<CreateMessageValidationContext> {

    public MessageCodeDuplicatedRule(CatalogPort catalogPort, MessageCodeQueryPort messageCodeQueryPort) {
        super(catalogPort,
                context -> !messageCodeQueryPort.existsByCodeAndApplicationId(
                        normalize(context.dto().getCode()), context.applicationId()),
                MessageCatalogCodeEnum.FUN_200,
                ConflictException::buildUserException);
    }
}
