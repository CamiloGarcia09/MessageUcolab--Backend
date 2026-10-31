package co.edu.uco.application.usecase.validator.application.rule;

import co.edu.uco.application.primaryports.dto.application.CreateApplicationDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.repository.ApplicationRepository;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import co.edu.uco.crosscutting.exceptions.ConflictException;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * El id de la aplicacion es opcional; cuando se envia, debe ser un UUID que aun no exista.
 */
@ExtendWith(MockitoExtension.class)
class ApplicationIdRulesTest {

    private static final String APPLICATION_ID = "c2d9f822-36d3-4580-a838-4f79f3c7cff8";

    @Mock
    private CatalogPort catalogPort;
    @Mock
    private ApplicationRepository applicationRepository;

    @ParameterizedTest(name = "id=[{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void rules_doNotThrowNorQueryRepository_whenIdIsOmitted(String id) {
        CreateApplicationDTO dto = dto(id);

        assertThatCode(() -> new ApplicationIdUuidRule(catalogPort).validate(dto)).doesNotThrowAnyException();
        assertThatCode(() -> new ApplicationIdDuplicatedRule(catalogPort, applicationRepository).validate(dto))
                .doesNotThrowAnyException();

        verifyNoInteractions(applicationRepository);
    }

    @Test
    void uuidRule_doesNotThrow_whenIdIsValidUuid() {
        assertThatCode(() -> new ApplicationIdUuidRule(catalogPort).validate(dto(APPLICATION_ID)))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "id=[{0}]")
    @ValueSource(strings = {"not-a-uuid", "00000000-0000-0000-0000-000000000000"})
    void uuidRule_throwsUsingFun038_whenIdIsNotAUsableUuid(String id) {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_038.getCode())).thenReturn("Invalid id");

        assertThatThrownBy(() -> new ApplicationIdUuidRule(catalogPort).validate(dto(id)))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(exception -> assertThat(((BusinessRuleException) exception).getUserMessage())
                        .isEqualTo("Invalid id"));
    }

    @Test
    void duplicatedRule_doesNotThrow_whenIdIsNotRegistered() {
        when(applicationRepository.existsById(APPLICATION_ID)).thenReturn(false);

        assertThatCode(() -> new ApplicationIdDuplicatedRule(catalogPort, applicationRepository)
                .validate(dto(APPLICATION_ID))).doesNotThrowAnyException();
    }

    @Test
    void duplicatedRule_throwsConflictUsingFun211_whenIdIsAlreadyRegistered() {
        when(applicationRepository.existsById(APPLICATION_ID)).thenReturn(true);
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_211.getCode())).thenReturn("Duplicate application id");
        CreateApplicationDTO dto = dto(APPLICATION_ID);

        assertThatThrownBy(() -> new ApplicationIdDuplicatedRule(catalogPort, applicationRepository).validate(dto))
                .isInstanceOf(ConflictException.class)
                .satisfies(exception -> assertThat((ConflictException) exception)
                        .extracting(ConflictException::getHttpStatus, ConflictException::getUserMessage)
                        .containsExactly(409, "Duplicate application id"));
    }

    private static CreateApplicationDTO dto(String id) {
        CreateApplicationDTO dto = new CreateApplicationDTO();
        dto.setName("MessageUcoLab");
        dto.setId(id);
        return dto;
    }
}
