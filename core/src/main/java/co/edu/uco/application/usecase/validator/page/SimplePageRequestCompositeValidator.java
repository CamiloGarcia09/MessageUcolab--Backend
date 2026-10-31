package co.edu.uco.application.usecase.validator.page;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.SimplePageRequest;
import co.edu.uco.application.usecase.validator.CompositeValidator;
import co.edu.uco.application.usecase.validator.page.rule.PageNumberRule;
import co.edu.uco.application.usecase.validator.page.rule.PageSizeRule;
import co.edu.uco.application.usecase.validator.page.rule.SortColumnRule;
import co.edu.uco.application.usecase.validator.page.rule.SortDirectionRule;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public final class SimplePageRequestCompositeValidator
        extends CompositeValidator<SimplePageRequestValidationContext> {

    public SimplePageRequestCompositeValidator(CatalogPort catalogPort) {
        super(List.of(
                new PageNumberRule(catalogPort),
                new PageSizeRule(catalogPort),
                new SortDirectionRule(catalogPort),
                new SortColumnRule(catalogPort)
        ), catalogPort);
    }

    public void validate(SimplePageRequest data, Class<?> modelClass) {
        super.validate(data == null ? null : new SimplePageRequestValidationContext(data, modelClass));
    }
}
