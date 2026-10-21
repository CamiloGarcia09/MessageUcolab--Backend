package co.edu.uco.application.usecase.validator.session;

import co.edu.uco.application.primaryports.dto.catalog.CatalogItemDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.entity.ApplicationData;
import co.edu.uco.application.secondaryports.repository.ApplicationRepository;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import co.edu.uco.crosscutting.exceptions.ForbiddenException;
import co.edu.uco.crosscutting.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionApplicationAccessCompositeValidatorTest {

    private static final String APPLICATION_ID = "c2d9f822-36d3-4580-a838-4f79f3c7cff8";
    private static final List<CatalogItemDTO> SESSION = List.of(CatalogItemDTO.create(APPLICATION_ID, "MessageUcoLab"));

    @Mock
    private CatalogPort catalogPort;
    @Mock
    private ApplicationRepository applicationRepository;

    private SessionApplicationAccessCompositeValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SessionApplicationAccessCompositeValidator(catalogPort, applicationRepository);
    }

    @Test
    void validate_acceptsAssignedAndRegisteredApplicationWithoutEnvironment() {
        when(applicationRepository.findById(APPLICATION_ID))
                .thenReturn(Optional.of(ApplicationData.build(UUID.fromString(APPLICATION_ID), "MessageUcoLab")));

        assertThatCode(() -> validator.validate(new SessionApplicationValidationContext(SESSION, APPLICATION_ID, null)))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_requiresApplicationBeforeRepositories() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_172.getCode())).thenReturn("Application id required");

        assertThatThrownBy(() -> validator.validate(new SessionApplicationValidationContext(SESSION, "", null)))
                .isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(applicationRepository);
    }

    @Test
    void validate_rejectsWithForbiddenBeforeRepository_whenApplicationIsNotInSession() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_153.getCode())).thenReturn("Permission denied");

        assertThatThrownBy(() -> validator.validate(new SessionApplicationValidationContext(
                SESSION, "72436a46-54d7-4cb5-a42c-af14f92d9416", null)))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(applicationRepository);
    }

    @Test
    void validate_rejectsWithNotFound_whenAssignedApplicationIsNotRegistered() {
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.empty());
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_157.getCode())).thenReturn("Application not found");

        assertThatThrownBy(() -> validator.validate(new SessionApplicationValidationContext(SESSION, APPLICATION_ID, null)))
                .isInstanceOf(NotFoundException.class);
    }
}
