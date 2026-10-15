package co.edu.uco.application.usecase.validator.message.rule;

import co.edu.uco.application.primaryports.dto.message.UpdateMessageDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.RecordExistsCatalogPort;
import co.edu.uco.application.secondaryports.repository.ReferenceCatalog;
import co.edu.uco.application.usecase.validator.rule.RuleValidator;
import co.edu.uco.application.usecase.validator.specification.Specifications;
import co.edu.uco.application.usecase.validator.specification.impl.CatalogReferenceExistsSpecification;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;

import java.util.function.Function;

public final class UpdateMessageCatalogIdExistsRule extends RuleValidator<UpdateMessageDTO> {

    public UpdateMessageCatalogIdExistsRule(CatalogPort catalogPort,
                                            RecordExistsCatalogPort recordExistsCatalogPort,
                                            Function<UpdateMessageDTO, String> idExtractor,
                                            ReferenceCatalog referenceCatalog,
                                            MessageCatalogCodeEnum messageCode) {
        super(catalogPort,
                Specifications.field(idExtractor,
                        new CatalogReferenceExistsSpecification(recordExistsCatalogPort, referenceCatalog)),
                messageCode);
    }
}
