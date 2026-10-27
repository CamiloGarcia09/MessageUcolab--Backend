package co.edu.uco.application.usecase.validator.message.rule;

import co.edu.uco.application.primaryports.dto.message.CreateMessageDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.entity.ApplicationData;
import co.edu.uco.application.secondaryports.entity.FunctionalityData;
import co.edu.uco.application.secondaryports.repository.FunctionalityCatalogRepository;
import co.edu.uco.application.usecase.validator.message.CreateMessageValidationContext;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static co.edu.uco.crosscutting.helpers.UtilUUID.DEFAULT_UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageFunctionalityActiveRuleTest {

    private static final UUID APPLICATION_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614175105");
    private static final UUID FUNCTIONALITY_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614175106");
    private static final UUID ACTIVE_STATE_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614175107");
    private static final String CATALOG_MESSAGE = "Choose an active functionality";

    @Mock private CatalogPort catalogPort;
    @Mock private FunctionalityCatalogRepository repository;

    @Test
    void validate_acceptsActiveFunctionalityInApplication() {
        when(repository.findAllByApplicationId(APPLICATION_ID.toString()))
                .thenReturn(List.of(functionality(APPLICATION_ID, ACTIVE_STATE_ID, "Active")));

        assertThatCode(() -> rule().validate(context())).doesNotThrowAnyException();
        verify(repository).findAllByApplicationId(APPLICATION_ID.toString());
    }

    @Test
    void validate_rejectsInactiveFunctionalityWithCatalogBusinessException() {
        when(repository.findAllByApplicationId(APPLICATION_ID.toString()))
                .thenReturn(List.of(functionality(APPLICATION_ID, ACTIVE_STATE_ID, "Inactive")));
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_217.getCode())).thenReturn(CATALOG_MESSAGE);

        assertThatThrownBy(() -> rule().validate(context()))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(exception -> assertThat(((BusinessRuleException) exception).getUserMessage())
                        .isEqualTo(CATALOG_MESSAGE));
        verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_217.getCode());
    }

    @Test
    void validate_rejectsUnknownStateMissingReferenceAndWrongApplication() {
        when(repository.findAllByApplicationId(APPLICATION_ID.toString())).thenReturn(List.of(
                functionality(APPLICATION_ID, ACTIVE_STATE_ID, "Pending"),
                functionality(APPLICATION_ID, DEFAULT_UUID, "Active"),
                functionality(UUID.randomUUID(), ACTIVE_STATE_ID, "Active")));
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_217.getCode())).thenReturn(CATALOG_MESSAGE);

        assertThatThrownBy(() -> rule().validate(context())).isInstanceOf(BusinessRuleException.class);
        verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_217.getCode());
    }

    private MessageFunctionalityActiveRule rule() {
        return new MessageFunctionalityActiveRule(catalogPort, repository);
    }

    private static CreateMessageValidationContext context() {
        return new CreateMessageValidationContext(CreateMessageDTO.builder()
                .functionalityId(FUNCTIONALITY_ID.toString()).build(), APPLICATION_ID.toString());
    }

    private static FunctionalityData functionality(UUID applicationId, UUID stateId, String state) {
        FunctionalityData functionality = new FunctionalityData(FUNCTIONALITY_ID, "Search",
                ApplicationData.build(applicationId, "Application"));
        functionality.setStateId(stateId);
        functionality.setState(state);
        return functionality;
    }
}
