package co.edu.uco.application.primaryports.facade.organization.impl;

import co.edu.uco.application.primaryports.dto.organization.CreateOrganizationDTO;
import co.edu.uco.application.usecase.handling.HandlingCreateOrganizationPort;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CreateOrganizationUseCaseFacadeImplTest {

    @Mock
    private HandlingCreateOrganizationPort handlingCreateOrganizationPort;

    private CreateOrganizationUseCaseFacadeImpl facade;

    @BeforeEach
    void setUp() {
        facade = new CreateOrganizationUseCaseFacadeImpl(handlingCreateOrganizationPort);
    }

    @Test
    void execute_delegatesDtoToHandlingPort() {
        CreateOrganizationDTO dto = CreateOrganizationDTO.builder().name("UCO").build();

        facade.execute(dto);

        verify(handlingCreateOrganizationPort).createOrganization(dto);
    }

    @Test
    void execute_delegatesNullDtoToHandlingPort() {
        facade.execute(null);

        verify(handlingCreateOrganizationPort).createOrganization(null);
    }

    @Test
    void execute_propagatesDomainExceptionFromHandlingPort() {
        CreateOrganizationDTO dto = CreateOrganizationDTO.builder().name("UCO").build();
        BusinessRuleException expected = BusinessRuleException.buildUserException("invalid organization");
        doThrow(expected).when(handlingCreateOrganizationPort).createOrganization(dto);

        assertThatThrownBy(() -> facade.execute(dto)).isSameAs(expected);
    }
}
