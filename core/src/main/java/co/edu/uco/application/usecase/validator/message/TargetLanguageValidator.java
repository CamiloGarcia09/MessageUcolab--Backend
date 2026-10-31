package co.edu.uco.application.usecase.validator.message;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.validator.CompositeValidator;
import co.edu.uco.application.usecase.validator.message.rule.TargetLanguageFormatRule;
import co.edu.uco.application.usecase.validator.message.rule.TargetLanguageRequiredRule;
import org.springframework.stereotype.Component;

import java.util.List;

import static co.edu.uco.crosscutting.helpers.UtilText.trim;

@Component
public final class TargetLanguageValidator extends CompositeValidator<String> {

    public TargetLanguageValidator(CatalogPort catalogPort) {
        super(List.of(
                new TargetLanguageRequiredRule(catalogPort),
                new TargetLanguageFormatRule(catalogPort)
        ), catalogPort);
    }

    @Override
    public void validate(String data) {
        super.validate(trim(data));
    }
}
