package co.edu.uco.application.usecase.validator.message;

import co.edu.uco.application.primaryports.dto.message.CreateMessageDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.entity.ApplicationData;
import co.edu.uco.application.secondaryports.entity.FunctionalityData;
import co.edu.uco.application.secondaryports.repository.FunctionalityCatalogRepository;
import co.edu.uco.application.secondaryports.repository.MessageCodeQueryPort;
import co.edu.uco.application.secondaryports.repository.RecordExistsCatalogPort;
import co.edu.uco.application.secondaryports.repository.ReferenceCatalog;
import co.edu.uco.application.usecase.validator.message.environment.MessageEnvironmentSelectionCompositeValidator;
import co.edu.uco.application.usecase.domain.message.importing.ImportIssue;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import co.edu.uco.crosscutting.exceptions.ConflictException;
import co.edu.uco.crosscutting.exceptions.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateMessageCompositeValidatorTest {

    private static final UUID APPLICATION_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614175106");
    private static final UUID FUNCTIONALITY_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614175107");
    private static final String TYPE_ID = "123e4567-e89b-12d3-a456-426614175101";
    private static final String CATEGORY_ID = "123e4567-e89b-12d3-a456-426614175102";
    private static final String STATUS_ID = "123e4567-e89b-12d3-a456-426614175103";
    private static final String APPLICATION_ID_TEXT = APPLICATION_ID.toString();
    private static final String FUNCTIONALITY_ID_TEXT = FUNCTIONALITY_ID.toString();
    private static final String MISSING_ID = "123e4567-e89b-12d3-a456-426614175198";
    private static final String DEFAULT_UUID = "00000000-0000-0000-0000-000000000000";
    private static final String INVALID_UUID = "not-a-uuid";
    private static final String FUNCTIONALITY_OUTSIDE_APPLICATION = "Functionality outside application";
    private static final String VALID_CODE = "WELCOME";
    private static final String NORMALIZED_CODE = "MSG_WELCOME";
    private static final String CODE_TOO_LONG_MESSAGE = "El sufijo del código supera 10 caracteres.";
    private static final String CODE_FORMAT_MESSAGE = "El sufijo del código tiene un formato inválido.";
    private static final String DUPLICATED_CODE_MESSAGE = "Ya existe un mensaje con ese código.";
    private static final String INVALID_FUNCTIONALITY_UUID_MESSAGE = "El id de la funcionalidad no es un UUID válido.";

    @Mock
    private CatalogPort catalogPort;
    @Mock
    private RecordExistsCatalogPort recordExistsCatalogPort;
    @Mock
    private FunctionalityCatalogRepository functionalityCatalogRepository;
    @Mock
    private MessageCodeQueryPort messageCodeQueryPort;
    @Mock
    private MessageEnvironmentSelectionCompositeValidator environmentSelectionValidator;

    private CreateMessageCompositeValidator validator;

    @BeforeEach
    void setUp() {
        validator = new CreateMessageCompositeValidator(catalogPort, recordExistsCatalogPort,
                functionalityCatalogRepository, messageCodeQueryPort, environmentSelectionValidator);
    }

    private CreateMessageDTO.CreateMessageDTOBuilder validDtoBuilder() {
        return CreateMessageDTO.builder()
                .code(VALID_CODE)
                .title("A valid title")
                .content("A valid message content")
                .typeId(TYPE_ID)
                .categoryId(CATEGORY_ID)
                .statusId(STATUS_ID)
                .functionalityId(FUNCTIONALITY_ID_TEXT);
    }

    private CreateMessageDTO validDto() {
        return validDtoBuilder().build();
    }

    private void stubCatalogReferences() {
        lenient().when(recordExistsCatalogPort.exists(any(), anyString())).thenReturn(true);
    }

    private void stubFunctionalityBelongingToDerivedApplication() {
        lenient().when(functionalityCatalogRepository.findAllByApplicationId(APPLICATION_ID_TEXT))
                .thenReturn(List.of(functionality(FUNCTIONALITY_ID, APPLICATION_ID)));
    }

    private static Stream<Arguments> catalogReferenceFields() {
        return Stream.of(
                Arguments.of(ReferenceCatalog.MESSAGE_TYPE, "The message type",
                        (BiConsumer<CreateMessageDTO, String>) CreateMessageDTO::setTypeId,
                        MessageCatalogCodeEnum.FUN_190, MessageCatalogCodeEnum.FUN_191),
                Arguments.of(ReferenceCatalog.MESSAGE_CATEGORY, "The message category",
                        (BiConsumer<CreateMessageDTO, String>) CreateMessageDTO::setCategoryId,
                        MessageCatalogCodeEnum.FUN_192, MessageCatalogCodeEnum.FUN_193),
                Arguments.of(ReferenceCatalog.MESSAGE_STATE, "The message state",
                        (BiConsumer<CreateMessageDTO, String>) CreateMessageDTO::setStatusId,
                        MessageCatalogCodeEnum.FUN_194, MessageCatalogCodeEnum.FUN_195));
    }

    private static Stream<Arguments> catalogReferenceFieldsWithInvalidUuid() {
        return Stream.of(INVALID_UUID, DEFAULT_UUID)
                .flatMap(invalidValue -> catalogReferenceFields().map(arguments -> Arguments.of(
                        arguments.get()[0], arguments.get()[1], arguments.get()[2],
                        arguments.get()[3], arguments.get()[4], invalidValue)));
    }

    @Test
    void validate_acceptsValidDtoWhoseFunctionalityBelongsToTheDerivedApplication() {
        stubCatalogReferences();
        stubFunctionalityBelongingToDerivedApplication();

        assertDoesNotThrow(() -> validator.validate(validDto(), APPLICATION_ID_TEXT));

        verify(functionalityCatalogRepository, times(2))
                .findAllByApplicationId(APPLICATION_ID_TEXT);
    }

    @Test
    void validate_acceptsUpperCasedIdentifiersBecauseComparisonsAreCaseInsensitive() {
        stubCatalogReferences();
        stubFunctionalityBelongingToDerivedApplication();
        CreateMessageDTO dto = validDtoBuilder()
                .functionalityId(FUNCTIONALITY_ID_TEXT.toUpperCase())
                .build();

        assertDoesNotThrow(() -> validator.validate(dto, APPLICATION_ID_TEXT.toUpperCase()));

        verify(functionalityCatalogRepository, times(2))
                .findAllByApplicationId(APPLICATION_ID_TEXT);
    }

    @Test
    void validate_runsCatalogReferencesThenFunctionalityCheckThenDuplicatedCodeCheckInOrder() {
        stubCatalogReferences();
        stubFunctionalityBelongingToDerivedApplication();
        when(messageCodeQueryPort.existsByCodeAndApplicationId(NORMALIZED_CODE, APPLICATION_ID_TEXT))
                .thenReturn(false);

        validator.validate(validDto(), APPLICATION_ID_TEXT);

        InOrder order = inOrder(recordExistsCatalogPort, functionalityCatalogRepository,
                messageCodeQueryPort);
        order.verify(recordExistsCatalogPort).exists(ReferenceCatalog.MESSAGE_TYPE, TYPE_ID);
        order.verify(recordExistsCatalogPort).exists(ReferenceCatalog.MESSAGE_CATEGORY, CATEGORY_ID);
        order.verify(recordExistsCatalogPort).exists(ReferenceCatalog.MESSAGE_STATE, STATUS_ID);
        order.verify(functionalityCatalogRepository, times(2))
                .findAllByApplicationId(APPLICATION_ID_TEXT);
        order.verify(messageCodeQueryPort)
                .existsByCodeAndApplicationId(NORMALIZED_CODE, APPLICATION_ID_TEXT);
    }

    @Test
    void validate_throwsForbiddenUsingFun146_whenFunctionalityIsOutsideTheDerivedApplication() {
        stubCatalogReferences();
        when(functionalityCatalogRepository.findAllByApplicationId(APPLICATION_ID_TEXT)).thenReturn(List.of());
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_146.getCode()))
                .thenReturn(FUNCTIONALITY_OUTSIDE_APPLICATION);

        assertThatThrownBy(() -> validator.validate(validDto(), APPLICATION_ID_TEXT))
                .isInstanceOf(ForbiddenException.class)
                .satisfies(exception -> assertThat((ForbiddenException) exception)
                        .extracting(ForbiddenException::getHttpStatus, ForbiddenException::getUserMessage)
                        .containsExactly(403, FUNCTIONALITY_OUTSIDE_APPLICATION));

        verify(functionalityCatalogRepository).findAllByApplicationId(APPLICATION_ID_TEXT);
    }

    @Test
    void validate_throwsForbiddenUsingFun146_whenDerivedApplicationIdIsBlank() {
        stubCatalogReferences();
        when(functionalityCatalogRepository.findAllByApplicationId(DEFAULT_UUID)).thenReturn(List.of());
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_146.getCode()))
                .thenReturn(FUNCTIONALITY_OUTSIDE_APPLICATION);

        assertThatThrownBy(() -> validator.validate(validDto(), null))
                .isInstanceOf(ForbiddenException.class)
                .satisfies(exception -> assertThat((ForbiddenException) exception)
                        .extracting(ForbiddenException::getHttpStatus, ForbiddenException::getUserMessage)
                        .containsExactly(403, FUNCTIONALITY_OUTSIDE_APPLICATION));

        verify(functionalityCatalogRepository).findAllByApplicationId(DEFAULT_UUID);
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("catalogReferenceFields")
    void validate_throwsBusinessRule_whenCatalogReferenceIsRequired(ReferenceCatalog catalog, String fieldName,
                                                                     BiConsumer<CreateMessageDTO, String> setter,
                                                                     MessageCatalogCodeEnum requiredCode,
                                                                     MessageCatalogCodeEnum existsCode) {
        lenient().when(recordExistsCatalogPort.exists(any(), anyString())).thenReturn(true);
        when(catalogPort.getMessage(requiredCode.getCode())).thenReturn(fieldName + " is required.");
        CreateMessageDTO dto = validDto();
        setter.accept(dto, "");

        assertThatThrownBy(() -> validator.validate(dto, APPLICATION_ID_TEXT))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo(fieldName + " is required."));

        assertAll(
                () -> verify(catalogPort).getMessage(requiredCode.getCode()),
                () -> verify(catalogPort, never()).getMessage(existsCode.getCode()),
                () -> verify(recordExistsCatalogPort, never()).exists(eq(catalog), anyString()),
                () -> verifyNoInteractions(functionalityCatalogRepository));
    }

    @ParameterizedTest(name = "{1} [{5}]")
    @MethodSource("catalogReferenceFieldsWithInvalidUuid")
    void validate_throwsBusinessRuleUsingFun038_whenCatalogReferenceIsNotAValidUuid(
            ReferenceCatalog catalog, String fieldName,
            BiConsumer<CreateMessageDTO, String> setter,
            MessageCatalogCodeEnum requiredCode,
            MessageCatalogCodeEnum existsCode,
            String invalidValue) {
        lenient().when(recordExistsCatalogPort.exists(any(), anyString())).thenReturn(true);
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_038.getCode()))
                .thenReturn("The catalog id must be a valid UUID.");
        CreateMessageDTO dto = validDto();
        setter.accept(dto, invalidValue);

        assertThatThrownBy(() -> validator.validate(dto, APPLICATION_ID_TEXT))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .as("FUN_038 message for %s with %s", fieldName, invalidValue)
                        .isEqualTo("The catalog id must be a valid UUID."));

        assertAll(
                () -> verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_038.getCode()),
                () -> verify(catalogPort, never()).getMessage(requiredCode.getCode()),
                () -> verify(catalogPort, never()).getMessage(existsCode.getCode()),
                () -> verify(recordExistsCatalogPort, never()).exists(eq(catalog), anyString()),
                () -> verifyNoInteractions(functionalityCatalogRepository));
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("catalogReferenceFields")
    void validate_throwsBusinessRule_whenCatalogReferenceDoesNotExist(ReferenceCatalog catalog, String fieldName,
                                                                      BiConsumer<CreateMessageDTO, String> setter,
                                                                      MessageCatalogCodeEnum requiredCode,
                                                                      MessageCatalogCodeEnum existsCode) {
        lenient().when(recordExistsCatalogPort.exists(any(), anyString())).thenReturn(true);
        when(recordExistsCatalogPort.exists(catalog, MISSING_ID)).thenReturn(false);
        when(catalogPort.getMessage(existsCode.getCode())).thenReturn(fieldName + " does not exist.");
        CreateMessageDTO dto = validDto();
        setter.accept(dto, MISSING_ID);

        assertThatThrownBy(() -> validator.validate(dto, APPLICATION_ID_TEXT))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo(fieldName + " does not exist."));

        assertAll(
                () -> verify(catalogPort).getMessage(existsCode.getCode()),
                () -> verify(catalogPort, never()).getMessage(requiredCode.getCode()),
                () -> verify(recordExistsCatalogPort).exists(catalog, MISSING_ID),
                () -> verifyNoInteractions(functionalityCatalogRepository));
    }

    @Test
    void validate_throwsBusinessRule_whenDtoIsNull() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_010.getCode())).thenReturn("Invalid data");

        assertThatThrownBy(() -> validator.validate(null, APPLICATION_ID_TEXT))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo("Invalid data"));

        verifyNoInteractions(functionalityCatalogRepository);
    }

    @Test
    void validate_throwsBusinessRule_whenCodeIsEmpty() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_040.getCode()))
                .thenReturn("The message code is required");
        CreateMessageDTO dto = validDto();
        dto.setCode("");

        assertThatThrownBy(() -> validator.validate(dto, APPLICATION_ID_TEXT))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo("The message code is required"));

        verifyNoInteractions(functionalityCatalogRepository);
    }

    @Test
    void validate_throwsBusinessRule_whenTitleIsEmpty() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_022.getCode()))
                .thenReturn("The message title is required");
        CreateMessageDTO dto = validDto();
        dto.setTitle("");

        assertThatThrownBy(() -> validator.validate(dto, APPLICATION_ID_TEXT))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo("The message title is required"));

        verifyNoInteractions(functionalityCatalogRepository);
    }

    @Test
    void validate_throwsBusinessRule_whenTitleIsShorterThanTen() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_020.getCode()))
                .thenReturn("The message title is too short");
        CreateMessageDTO dto = validDto();
        dto.setTitle("Short");

        assertThatThrownBy(() -> validator.validate(dto, APPLICATION_ID_TEXT))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo("The message title is too short"));

        verifyNoInteractions(functionalityCatalogRepository);
    }

    @Test
    void validate_throwsBusinessRule_whenTitleIsLongerThanFifty() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_021.getCode()))
                .thenReturn("The message title is too long");
        CreateMessageDTO dto = validDto();
        dto.setTitle("a".repeat(51));

        assertThatThrownBy(() -> validator.validate(dto, APPLICATION_ID_TEXT))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo("The message title is too long"));

        verifyNoInteractions(functionalityCatalogRepository);
    }

    @Test
    void validate_throwsBusinessRule_whenContentIsEmpty() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_017.getCode()))
                .thenReturn("The message content is required");
        CreateMessageDTO dto = validDto();
        dto.setContent("");

        assertThatThrownBy(() -> validator.validate(dto, APPLICATION_ID_TEXT))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo("The message content is required"));

        verifyNoInteractions(functionalityCatalogRepository);
    }

    @Test
    void validate_throwsBusinessRule_whenContentIsShorterThanTen() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_018.getCode()))
                .thenReturn("The message content is too short");
        CreateMessageDTO dto = validDto();
        dto.setContent("Short");

        assertThatThrownBy(() -> validator.validate(dto, APPLICATION_ID_TEXT))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo("The message content is too short"));

        verifyNoInteractions(functionalityCatalogRepository);
    }

    @Test
    void validate_throwsBusinessRule_whenContentIsLongerThanOneHundred() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_019.getCode()))
                .thenReturn("The message content is too long");
        CreateMessageDTO dto = validDto();
        dto.setContent("a".repeat(101));

        assertThatThrownBy(() -> validator.validate(dto, APPLICATION_ID_TEXT))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo("The message content is too long"));

        verifyNoInteractions(functionalityCatalogRepository);
    }

    @Test
    void validate_throwsBusinessRule_whenFunctionalityIdIsEmpty() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_188.getCode()))
                .thenReturn("The functionality id is required");
        CreateMessageDTO dto = validDto();
        dto.setFunctionalityId("");

        assertThatThrownBy(() -> validator.validate(dto, APPLICATION_ID_TEXT))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo("The functionality id is required"));

        verifyNoInteractions(functionalityCatalogRepository);
    }

    @Test
    void validate_throwsBusinessRuleUsingFun198_whenCodeSuffixExceedsTenCharacters() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_198.getCode()))
                .thenReturn(CODE_TOO_LONG_MESSAGE);
        CreateMessageDTO dto = validDtoBuilder().code("SUFFIXTOOLONG").build();

        assertThatThrownBy(() -> validator.validate(dto, APPLICATION_ID_TEXT))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo(CODE_TOO_LONG_MESSAGE));

        assertAll(
                () -> verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_198.getCode()),
                () -> verifyNoInteractions(recordExistsCatalogPort, functionalityCatalogRepository,
                        messageCodeQueryPort));
    }

    @Test
    void validate_throwsBusinessRuleUsingFun199_whenCodeSuffixIsNotAlphanumericOrSpace() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_199.getCode()))
                .thenReturn(CODE_FORMAT_MESSAGE);
        CreateMessageDTO dto = validDtoBuilder().code("MSG-001").build();

        assertThatThrownBy(() -> validator.validate(dto, APPLICATION_ID_TEXT))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo(CODE_FORMAT_MESSAGE));

        assertAll(
                () -> verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_199.getCode()),
                () -> verifyNoInteractions(recordExistsCatalogPort, functionalityCatalogRepository,
                        messageCodeQueryPort));
    }

    @Test
    void validate_throwsBusinessRuleUsingFun038_whenFunctionalityIdIsNotAValidUuid() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_038.getCode()))
                .thenReturn(INVALID_FUNCTIONALITY_UUID_MESSAGE);
        CreateMessageDTO dto = validDtoBuilder().functionalityId(INVALID_UUID).build();

        assertThatThrownBy(() -> validator.validate(dto, APPLICATION_ID_TEXT))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo(INVALID_FUNCTIONALITY_UUID_MESSAGE));

        assertAll(
                () -> verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_038.getCode()),
                () -> verifyNoInteractions(recordExistsCatalogPort, functionalityCatalogRepository,
                        messageCodeQueryPort));
    }

    @Test
    void validate_queriesNormalizedCode_whenDtoSuffixUsesLowerCaseAndConsecutiveSpaces() {
        stubCatalogReferences();
        stubFunctionalityBelongingToDerivedApplication();
        CreateMessageDTO dto = validDtoBuilder().code("wel  come").build();
        when(messageCodeQueryPort.existsByCodeAndApplicationId("MSG_WEL_COME",
                APPLICATION_ID_TEXT)).thenReturn(false);

        assertDoesNotThrow(() -> validator.validate(dto, APPLICATION_ID_TEXT));

        verify(messageCodeQueryPort)
                .existsByCodeAndApplicationId("MSG_WEL_COME", APPLICATION_ID_TEXT);
    }

    @Test
    void validate_throwsConflictUsingFun200_whenCodeIsAlreadyRegisteredForTheApplication() {
        stubCatalogReferences();
        stubFunctionalityBelongingToDerivedApplication();
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_200.getCode()))
                .thenReturn(DUPLICATED_CODE_MESSAGE);
        when(messageCodeQueryPort.existsByCodeAndApplicationId(NORMALIZED_CODE, APPLICATION_ID_TEXT))
                .thenReturn(true);

        assertThatThrownBy(() -> validator.validate(validDto(), APPLICATION_ID_TEXT))
                .isInstanceOf(ConflictException.class)
                .satisfies(exception -> assertThat((ConflictException) exception)
                        .extracting(ConflictException::getHttpStatus, ConflictException::getUserMessage)
                        .containsExactly(409, DUPLICATED_CODE_MESSAGE));

        assertAll(
                () -> verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_200.getCode()),
                () -> verify(messageCodeQueryPort)
                        .existsByCodeAndApplicationId(NORMALIZED_CODE, APPLICATION_ID_TEXT));
    }

    private static FunctionalityData functionality(UUID functionalityId, UUID applicationId) {
        FunctionalityData functionality = new FunctionalityData(functionalityId, "Functionality",
                ApplicationData.build(applicationId, "Application"));
        functionality.setStateId(UUID.fromString("123e4567-e89b-12d3-a456-426614175107"));
        functionality.setState("Active");
        return functionality;
    }

    @Test
    void validate_delegatesSelectedEnvironmentsWithApplicationId() {
        stubCatalogReferences();
        List<String> environments = List.of(MISSING_ID);
        CreateMessageDTO dto = validDtoBuilder().environmentIds(environments).build();
        doThrow(BusinessRuleException.buildUserException("Environment check"))
                .when(environmentSelectionValidator).validate(environments, APPLICATION_ID_TEXT);

        assertThatThrownBy(() -> validator.validate(dto, APPLICATION_ID_TEXT))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo("Environment check"));

        verify(environmentSelectionValidator).validate(environments, APPLICATION_ID_TEXT);
        verifyNoInteractions(functionalityCatalogRepository, messageCodeQueryPort);
    }

    @Test
    void collectErrors_aggregatesValidFieldsAndRunsEnvironmentCompositeOnce() {
        stubCatalogReferences();
        stubFunctionalityBelongingToDerivedApplication();
        when(messageCodeQueryPort.existsByCodeAndApplicationId(NORMALIZED_CODE, APPLICATION_ID_TEXT))
                .thenReturn(false);

        var issues = validator.collectErrors(validDto(), APPLICATION_ID_TEXT);

        assertThat(issues).isEmpty();
        verify(environmentSelectionValidator).validate(any(), eq(APPLICATION_ID_TEXT));
        verify(functionalityCatalogRepository, times(2)).findAllByApplicationId(APPLICATION_ID_TEXT);
    }

    @Test
    void collectRowErrors_skipsSharedEnvironmentAndDependentFunctionalityRulesAfterInvalidId() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_038.getCode()))
                .thenReturn(INVALID_FUNCTIONALITY_UUID_MESSAGE);
        var dto = validDtoBuilder().functionalityId(INVALID_UUID).environmentIds(List.of(MISSING_ID)).build();

        var issues = validator.collectRowErrors(dto, APPLICATION_ID_TEXT);

        assertThat(issues).extracting(ImportIssue::field).contains("functionality").doesNotContain("environments");
        verifyNoInteractions(environmentSelectionValidator, functionalityCatalogRepository);
    }
}
