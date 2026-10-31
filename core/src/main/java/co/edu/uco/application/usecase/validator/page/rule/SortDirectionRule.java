package co.edu.uco.application.usecase.validator.page.rule;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.validator.page.SimplePageRequestValidationContext;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;

import static co.edu.uco.application.CrosswordsConstant.REQUEST_PAGE_SORT_ASC;
import static co.edu.uco.application.CrosswordsConstant.REQUEST_PAGE_SORT_DESC;

public final class SortDirectionRule extends RuleValidator<SimplePageRequestValidationContext> {

    public SortDirectionRule(CatalogPort catalogPort) {
        super(catalogPort,
                context -> context != null && context.request() != null && context.request().getSort() != null
                        && (context.request().getSort().equalsIgnoreCase(REQUEST_PAGE_SORT_ASC)
                        || context.request().getSort().equalsIgnoreCase(REQUEST_PAGE_SORT_DESC)),
                MessageCatalogCodeEnum.FUN_031);
    }
}
