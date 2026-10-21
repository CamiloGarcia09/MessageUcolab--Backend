package co.edu.uco.application.usecase.validator.message;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.entity.MessageData;
import co.edu.uco.application.secondaryports.repository.SimplePageRequest;
import co.edu.uco.application.usecase.validator.message.rule.MessageFilterEnvironmentsUuidRule;
import co.edu.uco.application.usecase.validator.page.SimplePageRequestCompositeValidator;
import org.springframework.stereotype.Component;

@Component
public final class ListMessageValidator {
    private final SimplePageRequestCompositeValidator pageRequestValidator;
    private final MessageFilterEnvironmentsUuidRule environmentFilterRule;

    public ListMessageValidator(SimplePageRequestCompositeValidator pageRequestValidator, CatalogPort catalogPort) {
        this.pageRequestValidator = pageRequestValidator;
        this.environmentFilterRule = new MessageFilterEnvironmentsUuidRule(catalogPort);
    }

    public void validate(SimplePageRequest data) {
        pageRequestValidator.validate(data, MessageData.class);
        environmentFilterRule.validate(data);
    }
}
