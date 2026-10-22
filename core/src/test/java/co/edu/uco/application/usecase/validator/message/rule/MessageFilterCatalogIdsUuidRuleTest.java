package co.edu.uco.application.usecase.validator.message.rule;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.MessageFilter;
import co.edu.uco.application.secondaryports.repository.SimplePageRequest;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageFilterCatalogIdsUuidRuleTest {

    @Mock
    private CatalogPort catalogPort;

    @Test
    void validate_acceptsAbsentFiltersAndValidCatalogIdentifiers() {
        MessageFilterCatalogIdsUuidRule rule = new MessageFilterCatalogIdsUuidRule(catalogPort);

        assertThatCode(() -> rule.validate(null)).doesNotThrowAnyException();
        assertThatCode(() -> rule.validate(new SimplePageRequest())).doesNotThrowAnyException();
        assertThatCode(() -> rule.validate(request(
                "7b3e5d91-a2c8-46f0-9d14-5e7a1b6c3f82", "c9dc13ee-2f0e-4912-946d-e7e4352c6510")))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_acceptsBlankOptionalCatalogIdentifiers() {
        assertThatCode(() -> new MessageFilterCatalogIdsUuidRule(catalogPort)
                .validate(request(" ", ""))).doesNotThrowAnyException();
    }

    @Test
    void validate_rejectsMalformedOrDefaultCatalogIdentifiersWithFun050() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_050.getCode())).thenReturn("Invalid catalog filter");
        MessageFilterCatalogIdsUuidRule rule = new MessageFilterCatalogIdsUuidRule(catalogPort);

        assertThatThrownBy(() -> rule.validate(request("not-a-uuid", null)))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> rule.validate(request(null, "00000000-0000-0000-0000-000000000000")))
                .isInstanceOf(BusinessRuleException.class);
    }

    private static SimplePageRequest request(String statusId, String functionalityId) {
        SimplePageRequest request = new SimplePageRequest();
        request.setFilter(MessageFilter.builder().statusId(statusId).functionalityId(functionalityId).build());
        return request;
    }
}
