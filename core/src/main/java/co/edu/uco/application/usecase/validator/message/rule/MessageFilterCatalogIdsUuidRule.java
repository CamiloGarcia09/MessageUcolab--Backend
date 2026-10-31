package co.edu.uco.application.usecase.validator.message.rule;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.SimplePageRequest;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.application.usecase.validator.specification.impl.ValidUuidSpecification;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;

import java.util.stream.Stream;

public final class MessageFilterCatalogIdsUuidRule extends RuleValidator<SimplePageRequest> {

    public MessageFilterCatalogIdsUuidRule(final CatalogPort catalogPort) {
        super(catalogPort, MessageFilterCatalogIdsUuidRule::validFilters, MessageCatalogCodeEnum.FUN_050);
    }

    private static boolean validFilters(final SimplePageRequest request) {
        if (request == null || request.getFilter() == null) {
            return true;
        }
        final var filter = request.getFilter();
        return Stream.of(filter.getStatusId(), filter.getFunctionalityId())
                .filter(value -> value != null && !value.isBlank())
                .allMatch(new ValidUuidSpecification()::isSatisfiedBy);
    }
}
