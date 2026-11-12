package co.edu.uco.application.usecase;

import co.edu.uco.application.primaryports.dto.serviceidentity.RegisterServiceIdentityDTO;
import co.edu.uco.application.primaryports.dto.serviceidentity.ServiceIdentityDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.entity.ServiceIdentityData;
import co.edu.uco.application.secondaryports.repository.ServiceIdentityRepository;
import co.edu.uco.application.usecase.validator.serviceidentity.ServiceIdentityClientIdValidator;
import co.edu.uco.application.usecase.validator.serviceidentity.ServiceIdentityEnvironmentCompositeValidator;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import co.edu.uco.crosscutting.exceptions.ConflictException;
import co.edu.uco.crosscutting.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ManageServiceIdentitiesUseCase")
class ManageServiceIdentitiesUseCaseTest {

    private static final String APP = "123e4567-e89b-12d3-a456-426614175802";
    private static final String ENV = "123E4567-E89B-12D3-A456-426614175804";

    @Mock private ServiceIdentityRepository repository;
    @Mock private ServiceIdentityEnvironmentCompositeValidator environmentValidator;
    @Mock private CatalogPort catalog;
    private ManageServiceIdentitiesUseCase useCase;

    @BeforeEach
    void setUp() {
        lenient().when(catalog.getMessage(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        useCase = new ManageServiceIdentitiesUseCase(repository, environmentValidator,
                new ServiceIdentityClientIdValidator(catalog), catalog);
    }

    @Test
    @DisplayName("Lista las identidades de la aplicación")
    void listsIdentities() {
        when(repository.findByApplicationId(APP)).thenReturn(List.of(
                new ServiceIdentityData("inventario-dev", APP, "env-1", "develop", "2026-11-10T20:00:00Z")));

        assertThat(useCase.list(APP)).containsExactly(
                new ServiceIdentityDTO("inventario-dev", "env-1", "develop", "2026-11-10T20:00:00Z"));
    }

    @Test
    @DisplayName("Registra el client_id sin espacios con el ambiente normalizado")
    void registersIdentity() {
        when(repository.findByClientId("inventario-dev")).thenReturn(Optional.empty());

        useCase.register(APP, new RegisterServiceIdentityDTO(ENV, " inventario-dev "));

        verify(environmentValidator).validate(APP, ENV);
        verify(repository).register("inventario-dev", APP, ENV.toLowerCase());
    }

    @Test
    @DisplayName("Rechaza con conflicto un client_id ya asociado a otro ambiente")
    void rejectsDuplicatedClientId() {
        when(repository.findByClientId("inventario-dev")).thenReturn(Optional.of(
                new ServiceIdentityData("inventario-dev", "otra-app", "otro-env", "testing", "")));

        assertThatThrownBy(() -> useCase.register(APP, new RegisterServiceIdentityDTO(ENV, "inventario-dev")))
                .isInstanceOf(ConflictException.class).hasFieldOrPropertyWithValue("userMessage", "FUN_235");
        verify(repository, never()).register(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("No registra si el ambiente no pertenece a la aplicación")
    void stopsWhenEnvironmentIsInvalid() {
        doThrow(ConflictException.buildUserException("ambiente ajeno")).when(environmentValidator).validate(APP, ENV);

        assertThatThrownBy(() -> useCase.register(APP, new RegisterServiceIdentityDTO(ENV, "inventario-dev")))
                .isInstanceOf(ConflictException.class);
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Valida el cuerpo vacío como ambiente y client_id ausentes")
    void handlesNullRequest() {
        assertThatThrownBy(() -> useCase.register(APP, null)).isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("userMessage", "FUN_233");
        verify(environmentValidator).validate(APP, null);
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Retira la identidad de la aplicación")
    void removesIdentity() {
        when(repository.remove("inventario-dev", APP)).thenReturn(true);

        useCase.remove(APP, "inventario-dev");

        verify(repository).remove("inventario-dev", APP);
    }

    @Test
    @DisplayName("Responde no encontrado si la identidad no estaba asociada")
    void rejectsUnknownIdentityOnRemove() {
        when(repository.remove("otra", APP)).thenReturn(false);

        assertThatThrownBy(() -> useCase.remove(APP, "otra"))
                .isInstanceOf(NotFoundException.class).hasFieldOrPropertyWithValue("userMessage", "FUN_237");
    }

    @Test
    @DisplayName("No consulta el repositorio con un client_id inválido")
    void rejectsInvalidClientIdOnRemove() {
        assertThatThrownBy(() -> useCase.remove(APP, "con espacio"))
                .isInstanceOf(BusinessRuleException.class).hasFieldOrPropertyWithValue("userMessage", "FUN_234");
        verifyNoInteractions(repository);
    }
}
