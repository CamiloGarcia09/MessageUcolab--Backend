package co.edu.uco.application.usecase.validator.page.rule;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.validator.page.SimplePageRequestValidationContext;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;

import static co.edu.uco.application.CrosswordsConstant.REQUEST_PAGE_DEFAULT;
import static co.edu.uco.crosscutting.helpers.UtilNumeric.isBetweenIncludingRanges;

public final class PageSizeRule extends RuleValidator<SimplePageRequestValidationContext> {

    private static final int MAX_PAGE_SIZE = 100;

    public PageSizeRule(CatalogPort catalogPort) {
        super(catalogPort,
                context -> context != null && context.request() != null && isBetweenIncludingRanges(
                        context.request().getSize(), REQUEST_PAGE_DEFAULT, MAX_PAGE_SIZE),
                MessageCatalogCodeEnum.FUN_029,
                BusinessRuleException::buildUserException,
                (message, context) -> String.format(message, MAX_PAGE_SIZE));
    }
}
