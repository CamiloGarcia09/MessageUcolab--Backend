package co.edu.uco.application.usecase.validator.session.rule;

import co.edu.uco.application.primaryports.dto.catalog.CatalogItemDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.validator.session.SessionApplicationValidationContext;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionApplicationAssignedRuleTest {

    private static final String APPLICATION_ID = "c2d9f822-36d3-4580-a838-4f79f3c7cff8";
    private static final String OTHER_APPLICATION_ID = "72436a46-54d7-4cb5-a42c-af14f92d9416";
    private static final String FORBIDDEN_MESSAGE = "Permission denied";

    @Mock
    private CatalogPort catalogPort;

    private SessionApplicationAssignedRule rule;

    @BeforeEach
    void setUp() {
        rule = new SessionApplicationAssignedRule(catalogPort);
    }

    @Test
    void validate_doesNotThrow_whenApplicationIsAssignedToSession() {
        assertThatCode(() -> rule.validate(context(List.of(app(OTHER_APPLICATION_ID), app(APPLICATION_ID)),
                APPLICATION_ID))).doesNotThrowAnyException();

        verifyNoInteractions(catalogPort);
    }

    @Test
    void validate_comparesUuidsIgnoringCase() {
        assertThatCode(() -> rule.validate(context(List.of(app(APPLICATION_ID.toUpperCase())), APPLICATION_ID)))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_throwsForbiddenUsingFun153_whenApplicationIsNotAssigned() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_153.getCode())).thenReturn(FORBIDDEN_MESSAGE);
        SessionApplicationValidationContext context = context(List.of(app(OTHER_APPLICATION_ID)), APPLICATION_ID);

        assertThatThrownBy(() -> rule.validate(context))
                .isInstanceOf(ForbiddenException.class)
                .satisfies(exception -> assertThat((ForbiddenException) exception)
                        .extracting(ForbiddenException::getHttpStatus, ForbiddenException::getUserMessage)
                        .containsExactly(403, FORBIDDEN_MESSAGE));
    }

    @Test
    void validate_throwsForbidden_whenSessionHasNoApplications() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_153.getCode())).thenReturn(FORBIDDEN_MESSAGE);

        assertThatThrownBy(() -> rule.validate(context(List.of(), APPLICATION_ID)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void validate_throwsForbidden_whenSessionApplicationsAreNull() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_153.getCode())).thenReturn(FORBIDDEN_MESSAGE);

        assertThatThrownBy(() -> rule.validate(context(null, APPLICATION_ID)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void validate_ignoresSessionApplicationsWithoutUsableId() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_153.getCode())).thenReturn(FORBIDDEN_MESSAGE);
        SessionApplicationValidationContext context = context(
                List.of(app(""), app("not-a-uuid"), app("00000000-0000-0000-0000-000000000000")),
                "00000000-0000-0000-0000-000000000000");

        assertThatThrownBy(() -> rule.validate(context)).isInstanceOf(ForbiddenException.class);
    }

    @ParameterizedTest(name = "applicationId=[{0}]")
    @ValueSource(strings = {"", "not-a-uuid", "00000000-0000-0000-0000-000000000000"})
    void validate_throwsForbidden_whenRequestedIdIsNotAUsableUuid(String applicationId) {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_153.getCode())).thenReturn(FORBIDDEN_MESSAGE);

        assertThatThrownBy(() -> rule.validate(context(List.of(app(APPLICATION_ID)), applicationId)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void validate_throwsForbidden_whenContextIsNull() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_153.getCode())).thenReturn(FORBIDDEN_MESSAGE);

        assertThatThrownBy(() -> rule.validate(null)).isInstanceOf(ForbiddenException.class);
    }

    private static CatalogItemDTO app(String id) {
        return CatalogItemDTO.create(id, "App " + id);
    }

    private static SessionApplicationValidationContext context(List<CatalogItemDTO> applications, String applicationId) {
        return new SessionApplicationValidationContext(applications, applicationId, null);
    }
}
