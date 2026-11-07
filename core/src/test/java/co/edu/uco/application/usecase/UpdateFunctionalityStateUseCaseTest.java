package co.edu.uco.application.usecase;

import co.edu.uco.application.common.catalog.strategy.MessageCatalogStrategy;
import co.edu.uco.application.primaryports.dto.functionality.UpdateFunctionalityStateDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.entity.StatusMessageData;
import co.edu.uco.application.secondaryports.repository.MessageStateCatalogRepository;
import co.edu.uco.application.secondaryports.repository.UpdateFunctionalityStateRepository;
import co.edu.uco.application.usecase.validator.functionality.UpdateFunctionalityStateCompositeValidator;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateFunctionalityStateUseCaseTest {
    private static final String FUNCTIONALITY = "123e4567-e89b-12d3-a456-426614175801";
    private static final String APPLICATION = "123e4567-e89b-12d3-a456-426614175802";
    private static final String ACTIVE = "123e4567-e89b-12d3-a456-426614175803";
    private static final String INACTIVE = "123e4567-e89b-12d3-a456-426614175804";

    @Mock private UpdateFunctionalityStateRepository repository;
    @Mock private MessageStateCatalogRepository messageStates;
    @Mock private UpdateFunctionalityStateCompositeValidator validator;
    @Mock private MessageCatalogStrategy messageCatalog;
    @Mock private CatalogPort catalog;

    private UpdateFunctionalityStateUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateFunctionalityStateUseCase(repository, messageStates, validator, messageCatalog, catalog);
    }

    @Test
    void updateState_activatesWithoutChangingMessageStates() {
        when(repository.findStateNameById(ACTIVE)).thenReturn(Optional.of("Active"));
        when(repository.updateState(FUNCTIONALITY, APPLICATION, ACTIVE, null)).thenReturn(List.of());

        useCase.updateState(FUNCTIONALITY, new UpdateFunctionalityStateDTO(APPLICATION, ACTIVE));

        verify(repository).updateState(FUNCTIONALITY, APPLICATION, ACTIVE, null);
        verifyNoInteractions(messageStates);
    }

    @Test
    void updateState_inactivatesAndEvictsOnlyChangedMessageCodes() {
        when(repository.findStateNameById(INACTIVE)).thenReturn(Optional.of("Inactivo"));
        when(messageStates.findAll()).thenReturn(List.of(new StatusMessageData(UUID.fromString(INACTIVE), "Inactivo")));
        when(repository.updateState(FUNCTIONALITY, APPLICATION, INACTIVE, INACTIVE)).thenReturn(List.of("M-1", "M-2"));

        useCase.updateState(FUNCTIONALITY, new UpdateFunctionalityStateDTO(APPLICATION, INACTIVE));

        verify(messageCatalog).evictMessage("M-1");
        verify(messageCatalog).evictMessage("M-2");
    }

    @Test
    void updateState_throwsTechnicalDomainExceptionWithoutWriting_whenNoInactiveMessageStateExists() {
        when(repository.findStateNameById(INACTIVE)).thenReturn(Optional.of("Inactive"));
        when(messageStates.findAll()).thenReturn(List.of());
        when(catalog.getMessage(MessageCatalogCodeEnum.FUN_228.getCode())).thenReturn("inactive state unavailable");

        assertThatThrownBy(() -> useCase.updateState(FUNCTIONALITY,
                new UpdateFunctionalityStateDTO(APPLICATION, INACTIVE))).isInstanceOf(BusinessException.class);

        verify(repository).findStateNameById(INACTIVE);
        verifyNoInteractions(messageCatalog);
    }
}
