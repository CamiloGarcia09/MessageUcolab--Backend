package co.edu.uco.application.usecase.validator.session;

import co.edu.uco.application.primaryports.dto.catalog.CatalogItemDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import co.edu.uco.crosscutting.exceptions.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionApplicationRegistrationCompositeValidatorTest {

    private static final String APPLICATION_ID = "c2d9f822-36d3-4580-a838-4f79f3c7cff8";
    private static final List<CatalogItemDTO> SESSION = List.of(CatalogItemDTO.create(APPLICATION_ID, "MessageUcoLab"));

    @Mock
    private CatalogPort catalogPort;

    private SessionApplicationRegistrationCompositeValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SessionApplicationRegistrationCompositeValidator(catalogPort);
    }

    @Test
    void validate_acceptsApplicationAssignedToSession() {
        assertThatCode(() -> validator.validate(new SessionApplicationValidationContext(SESSION, APPLICATION_ID, null)))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_requiresIdBeforeCheckingFormatOrSession() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_172.getCode())).thenReturn("Application id required");

        assertThatThrownBy(() -> validator.validate(new SessionApplicationValidationContext(SESSION, "", null)))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(exception -> assertThat(((BusinessRuleException) exception).getUserMessage())
                        .isEqualTo("Application id required"));

        verify(catalogPort, never()).getMessage(MessageCatalogCodeEnum.FUN_038.getCode());
        verify(catalogPort, never()).getMessage(MessageCatalogCodeEnum.FUN_153.getCode());
    }

    @Test
    void validate_checksUuidBeforeSession() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_038.getCode())).thenReturn("Invalid id");

        assertThatThrownBy(() -> validator.validate(new SessionApplicationValidationContext(SESSION, "not-a-uuid", null)))
                .isInstanceOf(BusinessRuleException.class)
                .isNotInstanceOf(ForbiddenException.class);

        verify(catalogPort, never()).getMessage(MessageCatalogCodeEnum.FUN_153.getCode());
    }

    @Test
    void validate_rejectsWithForbidden_whenApplicationIsNotAssigned() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_153.getCode())).thenReturn("Permission denied");

        assertThatThrownBy(() -> validator.validate(new SessionApplicationValidationContext(
                SESSION, "72436a46-54d7-4cb5-a42c-af14f92d9416", null)))
                .isInstanceOf(ForbiddenException.class);
    }
}
