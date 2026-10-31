package co.edu.uco.application.usecase.validator.page.rule;

import co.edu.uco.application.primaryports.dto.page.PageRequestDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;

import java.util.function.Function;

import static co.edu.uco.crosscutting.helpers.UtilObject.isNullObject;
import static co.edu.uco.crosscutting.helpers.UtilText.validMatch;

public final class PageRequestAttributeFormatRule extends RuleValidator<PageRequestDTO> {

    public PageRequestAttributeFormatRule(CatalogPort catalogPort, Function<PageRequestDTO, String> extractor,
                                          String pattern, String attribute, MessageCatalogCodeEnum catalogCode) {
        super(catalogPort,
                data -> data != null && isValid(extractor.apply(data), pattern),
                catalogCode,
                BusinessRuleException::buildUserException,
                (message, data) -> String.format(message, attribute));
    }

    private static boolean isValid(String value, String pattern) {
        return isNullObject(value) || value.isEmpty() || validMatch(value, pattern);
    }
}
