package co.edu.uco.application.usecase;

import co.edu.uco.application.primaryports.dto.message.CreateMessageDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.entity.MessageData;
import co.edu.uco.application.secondaryports.entity.MessageEnvironmentStateData;
import co.edu.uco.application.secondaryports.logging.LoggingPort;
import co.edu.uco.application.secondaryports.logging.LoggingPortFactory;
import co.edu.uco.application.secondaryports.repository.CreateMessageRepository;
import co.edu.uco.application.secondaryports.repository.MessageEnvironmentStateCatalogRepository;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;
import co.edu.uco.application.usecase.security.MessageEnvironmentResolver;
import co.edu.uco.application.usecase.validator.message.CreateMessageCompositeValidator;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import co.edu.uco.crosscutting.exceptions.CrossWordsException;
import co.edu.uco.crosscutting.exceptions.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static co.edu.uco.application.CrosswordsConstant.STATE_ACTIVE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateMessageUseCaseTest {

    private static final String DEVELOP_UUID = "123e4567-e89b-12d3-a456-426614175000";
    private static final String PRODUCTION_UUID = "123e4567-e89b-12d3-a456-426614175002";
    private static final String APP_UUID = "123e4567-e89b-12d3-a456-426614175020";
    private static final String FUNC_UUID = "123e4567-e89b-12d3-a456-426614175001";
    private static final String TYPE_UUID = "123e4567-e89b-12d3-a456-426614175010";
    private static final String CATEGORY_UUID = "123e4567-e89b-12d3-a456-426614175011";
    private static final String STATUS_UUID = "123e4567-e89b-12d3-a456-426614175012";
    private static final String ACTIVE_STATE_UUID = "123e4567-e89b-12d3-a456-426614175013";
    private static final String FOREIGN_STATE_UUID = "123e4567-e89b-12d3-a456-426614175014";
    private static final List<String> ENVIRONMENTS = List.of(DEVELOP_UUID, PRODUCTION_UUID);
    private static final String NO_ACTIVE_STATE_MESSAGE = "No existe estado de ambiente de mensaje activo";

    @Mock
    private CreateMessageRepository createMessageRepository;
    @Mock
    private CreateMessageCompositeValidator validator;
    @Mock
    private MessageEnvironmentResolver environmentResolver;
    @Mock
    private MessageEnvironmentStateCatalogRepository messageEnvironmentStateRepository;
    @Mock
    private CatalogPort catalogPort;
    @Mock
    private LoggingPortFactory loggerFactory;
    @Mock
    private LoggingPort log;

    private CreateMessageUseCase useCase;

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(CreateMessageUseCase.class)).thenReturn(log);
        useCase = new CreateMessageUseCase(createMessageRepository, validator,
                environmentResolver, messageEnvironmentStateRepository,
                catalogPort, loggerFactory);
    }

    private CreateMessageDTO validDto() {
        return CreateMessageDTO.builder()
                .code("WELCOME")
                .title("A valid title")
                .content("A valid message content")
                .typeId(TYPE_UUID)
                .categoryId(CATEGORY_UUID)
                .statusId(STATUS_UUID)
                .functionalityId(FUNC_UUID)
                .environmentIds(ENVIRONMENTS)
                .build();
    }

    private MessageAccessContext sessionContext() {
        return new MessageAccessContext(APP_UUID);
    }

    private MessageAccessContext authenticatedContext() {
        return new MessageAccessContext(APP_UUID);
    }

    private void stubApplicationResolvingTo(String applicationId) {
        when(environmentResolver.resolveApplication(any()))
                .thenReturn(applicationId);
    }

    private void stubActiveStateFoundAmongForeignOnes() {
        when(messageEnvironmentStateRepository.findAll())
                .thenReturn(List.of(
                        new MessageEnvironmentStateData(UUID.fromString(FOREIGN_STATE_UUID), "Pending"),
                        new MessageEnvironmentStateData(UUID.fromString(ACTIVE_STATE_UUID), STATE_ACTIVE)));
    }

    private static Stream<Arguments> inactiveStateCatalogs() {
        return Stream.of(
                Arguments.of("lista vacía", List.<MessageEnvironmentStateData>of()),
                Arguments.of("lista nula", (List<MessageEnvironmentStateData>) null),
                Arguments.of("solo estados ajenos",
                        List.of(new MessageEnvironmentStateData(
                                UUID.fromString(FOREIGN_STATE_UUID), "Pending"))));
    }

    @Test
    void createMessage_resolvesApplicationValidatesAndPersistsInSelectedEnvironments() {
        CreateMessageDTO dto = validDto();
        MessageAccessContext context = sessionContext();
        stubApplicationResolvingTo(APP_UUID);
        stubActiveStateFoundAmongForeignOnes();

        useCase.createMessage(dto, context);

        InOrder order = inOrder(environmentResolver, validator, messageEnvironmentStateRepository,
                createMessageRepository);
        order.verify(environmentResolver).resolveApplication(context);
        order.verify(validator).validate(dto, APP_UUID);
        order.verify(messageEnvironmentStateRepository).findAll();
        order.verify(createMessageRepository)
                .createMessage(any(), eq(ENVIRONMENTS), eq(ACTIVE_STATE_UUID));
        verify(log).info("Message created successfully with code: {}", "MSG_WELCOME");
    }

    @Test
    void createMessage_persistsInSingleEnvironment_whenOnlyOneIsSelected() {
        CreateMessageDTO dto = validDto();
        dto.setEnvironmentIds(List.of(PRODUCTION_UUID));
        stubApplicationResolvingTo(APP_UUID);
        stubActiveStateFoundAmongForeignOnes();

        useCase.createMessage(dto, sessionContext());

        verify(createMessageRepository).createMessage(any(), eq(List.of(PRODUCTION_UUID)), eq(ACTIVE_STATE_UUID));
    }

    @Test
    void createMessage_normalizesEnvironmentIds() {
        CreateMessageDTO dto = validDto();
        dto.setEnvironmentIds(List.of(" " + DEVELOP_UUID.toUpperCase() + " "));
        stubApplicationResolvingTo(APP_UUID);
        stubActiveStateFoundAmongForeignOnes();

        useCase.createMessage(dto, sessionContext());

        verify(createMessageRepository).createMessage(any(), eq(List.of(DEVELOP_UUID)), eq(ACTIVE_STATE_UUID));
    }

    @Test
    void createMessage_persistsAndLogsNormalizedCode_whenDtoCodeIsRawSuffix() {
        CreateMessageDTO dto = validDto();
        dto.setCode("  wel  come  ");
        stubApplicationResolvingTo(APP_UUID);
        stubActiveStateFoundAmongForeignOnes();
        ArgumentCaptor<MessageData> messageCaptor = ArgumentCaptor.forClass(MessageData.class);

        useCase.createMessage(dto, sessionContext());

        verify(createMessageRepository).createMessage(messageCaptor.capture(), eq(ENVIRONMENTS),
                eq(ACTIVE_STATE_UUID));
        assertAll(
                () -> assertThat(messageCaptor.getValue().getCode())
                        .isEqualTo("MSG_WEL_COME"),
                () -> verify(log).info("Message created successfully with code: {}",
                        "MSG_WEL_COME"));
    }

    @Test
    void createMessage_resolvesApplicationFromSessionContext() {
        CreateMessageDTO dto = validDto();
        MessageAccessContext context = authenticatedContext();
        stubApplicationResolvingTo(APP_UUID);
        stubActiveStateFoundAmongForeignOnes();

        useCase.createMessage(dto, context);

        verify(environmentResolver).resolveApplication(context);
        verify(validator).validate(dto, APP_UUID);
        verify(createMessageRepository).createMessage(any(), eq(ENVIRONMENTS), eq(ACTIVE_STATE_UUID));
    }

    @Test
    void createMessage_buildsCatalogEntitiesWithResolvedApplicationIdAndEmptyNames() {
        CreateMessageDTO dto = validDto();
        stubApplicationResolvingTo(APP_UUID);
        stubActiveStateFoundAmongForeignOnes();
        ArgumentCaptor<MessageData> messageCaptor = ArgumentCaptor.forClass(MessageData.class);

        useCase.createMessage(dto, sessionContext());

        verify(createMessageRepository).createMessage(messageCaptor.capture(), eq(ENVIRONMENTS),
                eq(ACTIVE_STATE_UUID));
        MessageData message = messageCaptor.getValue();
        assertSoftly(softly -> {
            softly.assertThat(message.getId()).isNotNull();
            softly.assertThat(message.getType().getId()).isEqualTo(UUID.fromString(TYPE_UUID));
            softly.assertThat(message.getType().getName()).isEmpty();
            softly.assertThat(message.getCategory().getId()).isEqualTo(UUID.fromString(CATEGORY_UUID));
            softly.assertThat(message.getCategory().getName()).isEmpty();
            softly.assertThat(message.getStatus().getId()).isEqualTo(UUID.fromString(STATUS_UUID));
            softly.assertThat(message.getStatus().getName()).isEmpty();
            softly.assertThat(message.getFunctionality().getId()).isEqualTo(UUID.fromString(FUNC_UUID));
            softly.assertThat(message.getFunctionality().getName()).isEmpty();
            softly.assertThat(message.getFunctionality().getApplication().getId())
                    .isEqualTo(UUID.fromString(APP_UUID));
            softly.assertThat(message.getApplication()).isEmpty();
            softly.assertThat(message.getFunctionality().getApplication().getName()).isEmpty();
        });
    }

    @Test
    void createMessage_throwsCrossWordsException_whenCatalogReferenceIsNotUuid() {
        CreateMessageDTO dto = validDto();
        dto.setTypeId("not-a-uuid");
        stubApplicationResolvingTo(APP_UUID);
        stubActiveStateFoundAmongForeignOnes();

        assertThatThrownBy(() -> useCase.createMessage(dto, sessionContext()))
                .isInstanceOf(CrossWordsException.class)
                .satisfies(ex -> assertThat(((CrossWordsException) ex).getTechnicalMessage())
                        .isEqualTo("The UUID to be converted has no valid format."));
        verifyNoInteractions(createMessageRepository);
    }

    @Test
    void createMessage_propagatesForbiddenFromApplicationResolver() {
        CreateMessageDTO dto = validDto();
        MessageAccessContext context = authenticatedContext();
        ForbiddenException failure = ForbiddenException.buildUserException("Permission denied");
        doThrow(failure).when(environmentResolver).resolveApplication(context);

        assertThatThrownBy(() -> useCase.createMessage(dto, context)).isSameAs(failure);
        verifyNoInteractions(validator, messageEnvironmentStateRepository, createMessageRepository);
    }

    @Test
    void createMessage_propagatesValidationErrorWithoutQueryingEnvironmentStateCatalog() {
        CreateMessageDTO dto = validDto();
        stubApplicationResolvingTo(APP_UUID);
        doThrow(BusinessRuleException.buildUserException("Select at least one environment"))
                .when(validator).validate(dto, APP_UUID);

        assertThatThrownBy(() -> useCase.createMessage(dto, sessionContext()))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                        .isEqualTo("Select at least one environment"));
        verifyNoInteractions(messageEnvironmentStateRepository, createMessageRepository);
    }

    @Test
    void createMessage_throwsCrossWordsException_whenRepositoryFails() {
        CreateMessageDTO dto = validDto();
        stubApplicationResolvingTo(APP_UUID);
        stubActiveStateFoundAmongForeignOnes();
        doThrow(new RuntimeException("db down")).when(createMessageRepository)
                .createMessage(any(), anyList(), anyString());

        assertThatThrownBy(() -> useCase.createMessage(dto, sessionContext()))
                .isInstanceOf(CrossWordsException.class)
                .satisfies(ex -> assertThat(((CrossWordsException) ex).getTechnicalMessage())
                        .isEqualTo("Error al crear el mensaje"));
        verify(log).error(eq("Error creating message in repository"), any(RuntimeException.class));
    }

    @Test
    void createMessage_rethrowsDomainExceptionFromRepository() {
        CreateMessageDTO dto = validDto();
        stubApplicationResolvingTo(APP_UUID);
        stubActiveStateFoundAmongForeignOnes();
        doThrow(BusinessRuleException.buildUserException("conflict"))
                .when(createMessageRepository).createMessage(any(), anyList(), anyString());

        assertThatThrownBy(() -> useCase.createMessage(dto, sessionContext()))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Nested
    class ActiveEnvironmentStateResolution {

        @ParameterizedTest(name = "nombre de estado \"{0}\" se resuelve ignorando mayúsculas")
        @ValueSource(strings = {"active", "ACTIVE", "aCtIvE"})
        void createMessage_persistsActiveStateId_whenCatalogNameDiffersOnlyInCase(String catalogStateName) {
            CreateMessageDTO dto = validDto();
            stubApplicationResolvingTo(APP_UUID);
            when(messageEnvironmentStateRepository.findAll())
                    .thenReturn(List.of(
                            new MessageEnvironmentStateData(UUID.fromString(FOREIGN_STATE_UUID), "Pending"),
                            new MessageEnvironmentStateData(
                                    UUID.fromString(ACTIVE_STATE_UUID), catalogStateName)));

            useCase.createMessage(dto, sessionContext());

            verify(createMessageRepository).createMessage(any(), eq(ENVIRONMENTS), eq(ACTIVE_STATE_UUID));
        }

        @Test
        void createMessage_skipsNullAndForeignEntries_whenCatalogReturnsMixedStates() {
            CreateMessageDTO dto = validDto();
            stubApplicationResolvingTo(APP_UUID);
            List<MessageEnvironmentStateData> mixedStates = Arrays.asList(
                    null,
                    new MessageEnvironmentStateData(UUID.fromString(FOREIGN_STATE_UUID), "Deleted"),
                    new MessageEnvironmentStateData(UUID.fromString(FOREIGN_STATE_UUID), "   "),
                    new MessageEnvironmentStateData(UUID.fromString(ACTIVE_STATE_UUID), STATE_ACTIVE));
            when(messageEnvironmentStateRepository.findAll()).thenReturn(mixedStates);

            useCase.createMessage(dto, sessionContext());

            verify(createMessageRepository).createMessage(any(), eq(ENVIRONMENTS), eq(ACTIVE_STATE_UUID));
        }

        @ParameterizedTest(name = "catálogo de estados {0}")
        @MethodSource("co.edu.uco.application.usecase.CreateMessageUseCaseTest#inactiveStateCatalogs")
        void createMessage_throwsBusinessRuleUsingFun197_whenCatalogHasNoActiveState(
                String scenario, List<MessageEnvironmentStateData> states) {
            CreateMessageDTO dto = validDto();
            stubApplicationResolvingTo(APP_UUID);
            when(messageEnvironmentStateRepository.findAll()).thenReturn(states);
            when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_197.getCode()))
                    .thenReturn(NO_ACTIVE_STATE_MESSAGE);

            assertThatThrownBy(() -> useCase.createMessage(dto, sessionContext()))
                    .as("Catálogo de estados: %s", scenario)
                    .isInstanceOf(BusinessRuleException.class)
                    .satisfies(ex -> assertThat(((BusinessRuleException) ex).getUserMessage())
                            .isEqualTo(NO_ACTIVE_STATE_MESSAGE));

            assertAll(
                    () -> verify(messageEnvironmentStateRepository).findAll(),
                    () -> verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_197.getCode()),
                    () -> verifyNoInteractions(createMessageRepository));
        }
    }
}
