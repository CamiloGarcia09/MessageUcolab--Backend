package co.edu.uco.application.usecase.validator.message.rule;

import co.edu.uco.application.primaryports.dto.message.CreateMessageDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.entity.ApplicationData;
import co.edu.uco.application.secondaryports.entity.FunctionalityData;
import co.edu.uco.application.secondaryports.repository.FunctionalityCatalogRepository;
import co.edu.uco.application.usecase.validator.message.CreateMessageValidationContext;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageFunctionalityBelongsApplicationRuleTest {

    private static final UUID APPLICATION_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614175105");
    private static final UUID OTHER_APPLICATION_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614175199");
    private static final UUID FUNCTIONALITY_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614175106");
    private static final UUID OTHER_FUNCTIONALITY_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614175198");
    private static final String OUTSIDE_APPLICATION_MESSAGE = "Functionality outside application";
    private static final String DEFAULT_UUID = "00000000-0000-0000-0000-000000000000";

    @Mock
    private CatalogPort catalogPort;
    @Mock
    private FunctionalityCatalogRepository functionalityCatalogRepository;

    private MessageFunctionalityBelongsApplicationRule rule;

    @BeforeEach
    void setUp() {
        rule = new MessageFunctionalityBelongsApplicationRule(catalogPort, functionalityCatalogRepository);
    }

    @Test
    void validate_doesNotThrow_whenFunctionalityBelongsToTheContextApplication() {
        when(functionalityCatalogRepository.findAllByApplicationId(APPLICATION_ID.toString()))
                .thenReturn(List.of(functionality(FUNCTIONALITY_ID, APPLICATION_ID)));

        assertThatCode(() -> rule.validate(context())).doesNotThrowAnyException();

        verify(functionalityCatalogRepository).findAllByApplicationId(APPLICATION_ID.toString());
    }

    @Test
    void validate_doesNotThrow_whenIdentifiersUseDifferentCase() {
        CreateMessageValidationContext context = new CreateMessageValidationContext(
                CreateMessageDTO.builder()
                        .functionalityId(FUNCTIONALITY_ID.toString().toUpperCase())
                        .build(),
                APPLICATION_ID.toString().toUpperCase());
        when(functionalityCatalogRepository.findAllByApplicationId(APPLICATION_ID.toString()))
                .thenReturn(List.of(functionality(FUNCTIONALITY_ID, APPLICATION_ID)));

        assertThatCode(() -> rule.validate(context)).doesNotThrowAnyException();

        verify(functionalityCatalogRepository).findAllByApplicationId(APPLICATION_ID.toString());
    }

    @Test
    void validate_throwsForbiddenUsingFun146_whenFunctionalityListIsEmpty() {
        when(functionalityCatalogRepository.findAllByApplicationId(APPLICATION_ID.toString()))
                .thenReturn(List.of());
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_146.getCode()))
                .thenReturn(OUTSIDE_APPLICATION_MESSAGE);
        CreateMessageValidationContext context = context();

        assertThatThrownBy(() -> rule.validate(context))
                .isInstanceOf(ForbiddenException.class)
                .satisfies(exception -> assertThat((ForbiddenException) exception)
                        .extracting(ForbiddenException::getHttpStatus, ForbiddenException::getUserMessage)
                        .containsExactly(403, OUTSIDE_APPLICATION_MESSAGE));
        verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_146.getCode());
    }

    @Test
    void validate_throwsForbiddenUsingFun146_whenFunctionalityRepositoryReturnsNull() {
        when(functionalityCatalogRepository.findAllByApplicationId(APPLICATION_ID.toString()))
                .thenReturn(null);
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_146.getCode()))
                .thenReturn(OUTSIDE_APPLICATION_MESSAGE);
        CreateMessageValidationContext context = context();

        assertThatThrownBy(() -> rule.validate(context))
                .isInstanceOf(ForbiddenException.class)
                .satisfies(exception -> assertThat((ForbiddenException) exception)
                        .extracting(ForbiddenException::getHttpStatus, ForbiddenException::getUserMessage)
                        .containsExactly(403, OUTSIDE_APPLICATION_MESSAGE));
    }

    @ParameterizedTest(name = "[{index}] functionality outside application")
    @MethodSource("functionalitiesOutsideApplication")
    void validate_throwsForbiddenUsingFun146_whenFunctionalityIsOutsideApplication(
            List<FunctionalityData> functionalities) {
        when(functionalityCatalogRepository.findAllByApplicationId(APPLICATION_ID.toString()))
                .thenReturn(functionalities);
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_146.getCode()))
                .thenReturn(OUTSIDE_APPLICATION_MESSAGE);
        CreateMessageValidationContext context = context();

        assertThatThrownBy(() -> rule.validate(context))
                .isInstanceOf(ForbiddenException.class)
                .satisfies(exception -> assertThat((ForbiddenException) exception)
                        .extracting(ForbiddenException::getHttpStatus, ForbiddenException::getUserMessage)
                        .containsExactly(403, OUTSIDE_APPLICATION_MESSAGE));
    }

    @Test
    void validate_throwsForbiddenUsingFun146_whenContextApplicationIdIsAbsent() {
        when(functionalityCatalogRepository.findAllByApplicationId(DEFAULT_UUID)).thenReturn(List.of());
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_146.getCode()))
                .thenReturn(OUTSIDE_APPLICATION_MESSAGE);
        CreateMessageValidationContext context = new CreateMessageValidationContext(
                CreateMessageDTO.builder()
                        .functionalityId(FUNCTIONALITY_ID.toString())
                        .build(),
                null);

        assertThatThrownBy(() -> rule.validate(context))
                .isInstanceOf(ForbiddenException.class)
                .satisfies(exception -> assertThat((ForbiddenException) exception)
                        .extracting(ForbiddenException::getHttpStatus, ForbiddenException::getUserMessage)
                        .containsExactly(403, OUTSIDE_APPLICATION_MESSAGE));

        verify(functionalityCatalogRepository).findAllByApplicationId(DEFAULT_UUID);
    }

    private static Stream<List<FunctionalityData>> functionalitiesOutsideApplication() {
        FunctionalityData withoutApplication = new FunctionalityData();
        withoutApplication.setId(FUNCTIONALITY_ID);
        return Stream.of(
                Collections.singletonList(null),
                Collections.singletonList(functionality(OTHER_FUNCTIONALITY_ID, APPLICATION_ID)),
                Collections.singletonList(functionality(FUNCTIONALITY_ID, OTHER_APPLICATION_ID)),
                Collections.singletonList(withoutApplication)
        );
    }

    private static CreateMessageValidationContext context() {
        return new CreateMessageValidationContext(
                CreateMessageDTO.builder()
                        .functionalityId(FUNCTIONALITY_ID.toString())
                        .build(),
                APPLICATION_ID.toString());
    }

    private static FunctionalityData functionality(UUID functionalityId, UUID applicationId) {
        return new FunctionalityData(functionalityId, "Functionality",
                ApplicationData.build(applicationId, "Application"));
    }
}
