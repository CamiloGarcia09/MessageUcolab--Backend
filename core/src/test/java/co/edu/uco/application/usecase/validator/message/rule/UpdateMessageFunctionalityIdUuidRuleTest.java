package co.edu.uco.application.usecase.validator.message.rule;

import co.edu.uco.application.primaryports.dto.message.UpdateMessageDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateMessageFunctionalityIdUuidRuleTest {

    private static final String VALID_UUID = "123e4567-e89b-12d3-a456-426614175107";
    private static final String UPPERCASE_UUID = "123E4567-E89B-12D3-A456-426614175107";
    private static final String DEFAULT_UUID = "00000000-0000-0000-0000-000000000000";
    private static final String FUN_038_USER_MESSAGE =
            "El id de la funcionalidad debe ser un UUID válido y distinto del UUID por defecto.";

    @Mock
    private CatalogPort catalogPort;

    private UpdateMessageFunctionalityIdUuidRule rule;

    @BeforeEach
    void setUp() {
        rule = new UpdateMessageFunctionalityIdUuidRule(catalogPort);
    }

    private void stubFun038Message() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_038.getCode()))
                .thenReturn(FUN_038_USER_MESSAGE);
    }

    @ParameterizedTest(name = "functionalityId \"{0}\"")
    @ValueSource(strings = {VALID_UUID, UPPERCASE_UUID})
    void validate_doesNotThrow_whenFunctionalityIdIsAWellFormedNonDefaultUuid(String functionalityId) {
        UpdateMessageDTO dto = UpdateMessageDTO.builder().functionalityId(functionalityId).build();

        assertThatCode(() -> rule.validate(dto))
                .doesNotThrowAnyException();

        verifyNoInteractions(catalogPort);
    }

    @ParameterizedTest(name = "functionalityId \"{0}\" is not a valid UUID")
    @NullAndEmptySource
    @ValueSource(strings = {"not-a-uuid", DEFAULT_UUID})
    void validate_throwsBusinessRuleUsingFun038_whenFunctionalityIdIsNotAnAdmittedUuid(String functionalityId) {
        stubFun038Message();

        assertThatThrownBy(() -> rule.validate(UpdateMessageDTO.builder().functionalityId(functionalityId).build()))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo(FUN_038_USER_MESSAGE));

        verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_038.getCode());
    }

    @Test
    void validate_throwsBusinessRuleUsingFun038_whenDtoIsNull() {
        stubFun038Message();

        assertThatThrownBy(() -> rule.validate(null))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo(FUN_038_USER_MESSAGE));

        verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_038.getCode());
    }
}
