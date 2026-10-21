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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageFilterEnvironmentsUuidRuleTest {

    @Mock
    private CatalogPort catalogPort;

    @Test
    void validate_doesNotThrow_whenFilterIsAbsentOrEmpty() {
        MessageFilterEnvironmentsUuidRule rule = new MessageFilterEnvironmentsUuidRule(catalogPort);

        assertThatCode(() -> rule.validate(null)).doesNotThrowAnyException();
        assertThatCode(() -> rule.validate(new SimplePageRequest())).doesNotThrowAnyException();
        assertThatCode(() -> rule.validate(request(List.of()))).doesNotThrowAnyException();
    }

    @Test
    void validate_doesNotThrow_whenEveryEnvironmentIdIsUuid() {
        assertThatCode(() -> new MessageFilterEnvironmentsUuidRule(catalogPort)
                .validate(request(List.of("7b3e5d91-a2c8-46f0-9d14-5e7a1b6c3f82"))))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_throwsUsingFun038_whenAnyEnvironmentIdIsInvalid() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_038.getCode())).thenReturn("Invalid id");

        assertThatThrownBy(() -> new MessageFilterEnvironmentsUuidRule(catalogPort)
                .validate(request(List.of("7b3e5d91-a2c8-46f0-9d14-5e7a1b6c3f82", "x' OR 1=1"))))
                .isInstanceOf(BusinessRuleException.class);
    }

    private static SimplePageRequest request(List<String> environmentIds) {
        SimplePageRequest request = new SimplePageRequest();
        request.setFilter(MessageFilter.builder().environmentIds(environmentIds).build());
        return request;
    }
}
