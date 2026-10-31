package co.edu.uco.application.usecase.validator.page.rule;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.validator.page.SimplePageRequestValidationContext;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;

import java.lang.reflect.Field;
import java.util.Arrays;

public final class SortColumnRule extends RuleValidator<SimplePageRequestValidationContext> {

    public SortColumnRule(CatalogPort catalogPort) {
        super(catalogPort,
                context -> context != null && context.request() != null && context.modelClass() != null
                        && context.request().getColumnSort() != null
                        && Arrays.stream(context.modelClass().getDeclaredFields())
                        .map(Field::getName)
                        .anyMatch(context.request().getColumnSort()::equals),
                MessageCatalogCodeEnum.FUN_030,
                BusinessRuleException::buildUserException,
                (message, context) -> String.format(message,
                        context == null || context.request() == null ? null : context.request().getColumnSort()));
    }
}
