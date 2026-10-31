package co.edu.uco.application.usecase.validator.page.rule;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.validator.page.SimplePageRequestValidationContext;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;

import static co.edu.uco.application.CrosswordsConstant.REQUEST_PAGE_DEFAULT;

public final class PageNumberRule extends RuleValidator<SimplePageRequestValidationContext> {

    public PageNumberRule(CatalogPort catalogPort) {
        super(catalogPort,
                context -> context != null && context.request() != null
                        && context.request().getPage() >= REQUEST_PAGE_DEFAULT,
                MessageCatalogCodeEnum.FUN_032);
    }
}
