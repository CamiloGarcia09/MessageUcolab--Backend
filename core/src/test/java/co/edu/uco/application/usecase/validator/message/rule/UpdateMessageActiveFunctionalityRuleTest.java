package co.edu.uco.application.usecase.validator.message.rule;

import co.edu.uco.application.primaryports.dto.message.UpdateMessageDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.entity.ApplicationData;
import co.edu.uco.application.secondaryports.entity.FunctionalityData;
import co.edu.uco.application.secondaryports.entity.StatusMessageData;
import co.edu.uco.application.secondaryports.repository.FunctionalityCatalogRepository;
import co.edu.uco.application.secondaryports.repository.MessageStateCatalogRepository;
import co.edu.uco.application.usecase.validator.message.UpdateMessageValidationContext;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateMessageActiveFunctionalityRuleTest {
    private static final UUID APP = UUID.fromString("123e4567-e89b-12d3-a456-426614175810");
    private static final UUID FUNCTIONALITY = UUID.fromString("123e4567-e89b-12d3-a456-426614175811");
    private static final UUID ACTIVE = UUID.fromString("123e4567-e89b-12d3-a456-426614175812");
    private static final UUID INACTIVE = UUID.fromString("123e4567-e89b-12d3-a456-426614175813");
    @Mock private CatalogPort catalog;
    @Mock private FunctionalityCatalogRepository functionalities;
    @Mock private MessageStateCatalogRepository states;

    @Test
    void validate_allowsActivatingMessageOnlyWhenItsFunctionalityIsActive() {
        when(states.findAll()).thenReturn(List.of(new StatusMessageData(ACTIVE, "Active")));
        when(functionalities.findAllByApplicationId(APP.toString())).thenReturn(List.of(functionality("Active")));
        UpdateMessageActiveFunctionalityRule rule = new UpdateMessageActiveFunctionalityRule(catalog, functionalities, states);

        assertThatCode(() -> rule.validate(context(ACTIVE))).doesNotThrowAnyException();
    }

    @Test
    void validate_rejectsActivatingMessageForInactiveFunctionality() {
        when(states.findAll()).thenReturn(List.of(new StatusMessageData(ACTIVE, "Active")));
        when(functionalities.findAllByApplicationId(APP.toString())).thenReturn(List.of(functionality("Inactivo")));
        UpdateMessageActiveFunctionalityRule rule = new UpdateMessageActiveFunctionalityRule(catalog, functionalities, states);

        assertThatThrownBy(() -> rule.validate(context(ACTIVE))).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void validate_allowsEditingMessageWhileKeepingItInactive() {
        when(states.findAll()).thenReturn(List.of(new StatusMessageData(INACTIVE, "Inactive")));
        UpdateMessageActiveFunctionalityRule rule = new UpdateMessageActiveFunctionalityRule(catalog, functionalities, states);

        assertThatCode(() -> rule.validate(context(INACTIVE))).doesNotThrowAnyException();
    }

    private static FunctionalityData functionality(String state) {
        FunctionalityData functionality = FunctionalityData.build();
        functionality.setId(FUNCTIONALITY);
        functionality.setState(state);
        functionality.setApplication(ApplicationData.build(APP, "App"));
        return functionality;
    }

    private static UpdateMessageValidationContext context(UUID stateId) {
        UpdateMessageDTO dto = UpdateMessageDTO.builder().statusId(stateId.toString())
                .functionalityId(FUNCTIONALITY.toString()).build();
        return new UpdateMessageValidationContext(dto, APP.toString());
    }
}
