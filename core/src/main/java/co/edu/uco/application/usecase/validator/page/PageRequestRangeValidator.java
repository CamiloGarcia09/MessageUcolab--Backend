package co.edu.uco.application.usecase.validator.page;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import org.springframework.stereotype.Component;

import static co.edu.uco.crosscutting.helpers.UtilNumeric.isGreaterThan;

@Component
public final class PageRequestRangeValidator extends RuleValidator<PageRequestRangeValidationContext> {

    public PageRequestRangeValidator(CatalogPort catalogPort) {
        super(catalogPort,
                context -> context != null && !isGreaterThan(context.page(), context.totalPages()),
                MessageCatalogCodeEnum.FUN_028,
                BusinessRuleException::buildUserException,
                (message, context) -> String.format(message, context == null ? 0 : context.totalPages()));
    }

    public void validate(int page, int totalPages) {
        super.validate(new PageRequestRangeValidationContext(page, totalPages));
    }
}
