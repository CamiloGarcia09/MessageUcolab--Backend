package co.edu.uco.infraestructure.primaryadapters.controller;

import co.edu.uco.application.primaryports.dto.message.MessageDTO;
import co.edu.uco.application.primaryports.dto.message.TranslateMessageDTO;
import co.edu.uco.application.primaryports.dto.message.TranslatedMessageDTO;
import co.edu.uco.application.primaryports.dto.page.PageRequestDTO;
import co.edu.uco.application.primaryports.facade.message.FindMessageByCodeAndEnvironmentUseCaseFacade;
import co.edu.uco.application.primaryports.facade.message.FindMessagesByEnvironmentUsecaseFacade;
import co.edu.uco.application.primaryports.facade.message.TranslateMessageByCodeAndEnvironmentUseCaseFacade;
import co.edu.uco.application.secondaryports.presenter.PresenterPort;
import co.edu.uco.application.secondaryports.repository.SimplePage;
import co.edu.uco.application.usecase.domain.security.ExternalIdentity;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessagesControllerImplTest {

    private static final ExternalIdentity IDENTITY = new ExternalIdentity(
            "issuer", "subject", null, Instant.MAX);

    @Mock
    private FindMessagesByEnvironmentUsecaseFacade findMessagesByEnvironmentUsecaseFacade;
    @Mock
    private FindMessageByCodeAndEnvironmentUseCaseFacade findMessageByCodeAndEnvironmentUseCaseFacade;
    @Mock
    private TranslateMessageByCodeAndEnvironmentUseCaseFacade translateMessageByCodeAndEnvironmentUseCaseFacade;
    @Mock
    private PresenterPort<MessageDTO> restPresenter;
    @Mock
    private PresenterPort<TranslatedMessageDTO> translationPresenter;
    @Mock
    private PresenterPort<SimplePage<MessageDTO>> restPresenterPage;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    private MessagesControllerImpl controller;

    @BeforeEach
    void setUp() {
        controller = new MessagesControllerImpl(
                findMessagesByEnvironmentUsecaseFacade,
                findMessageByCodeAndEnvironmentUseCaseFacade,
                translateMessageByCodeAndEnvironmentUseCaseFacade,
                restPresenter,
                translationPresenter,
                restPresenterPage);
    }

    @Test
    void findByEnvironmentAndMessage_usesAuthenticatedEnvironmentAndPresentsResult() {
        when(request.getAttribute("environmentId")).thenReturn("env-1");
        SimplePage<MessageDTO> page = SimplePage.of(List.of(), 1, 10, 0, 0);
        when(findMessagesByEnvironmentUsecaseFacade.execute(any(MessageAccessContext.class), any(PageRequestDTO.class)))
                .thenReturn(page);

        controller.findByEnvironmentAndMessage("1", "10", "asc", "code", request, response);

        ArgumentCaptor<PageRequestDTO> captor = ArgumentCaptor.forClass(PageRequestDTO.class);
        ArgumentCaptor<MessageAccessContext> contextCaptor = ArgumentCaptor.forClass(MessageAccessContext.class);
        verify(findMessagesByEnvironmentUsecaseFacade).execute(contextCaptor.capture(), captor.capture());
        PageRequestDTO captured = captor.getValue();
        assertAll(
                () -> assertThat(contextCaptor.getValue().legacyEnvironmentId()).isEqualTo("env-1"),
                () -> assertThat(contextCaptor.getValue().externalIdentity()).isNull(),
                () -> assertThat(captured.getPage()).isEqualTo("1"),
                () -> assertThat(captured.getSize()).isEqualTo("10"),
                () -> assertThat(captured.getSort()).isEqualTo("asc"),
                () -> assertThat(captured.getColumnSort()).isEqualTo("code"));
        verify(restPresenterPage).presentRestSuccess(List.of(page), request, response);
    }

    @Test
    void findByCodeMessageAndEnvironment_presentsSingleMessage() {
        when(request.getAttribute("environmentId")).thenReturn("env-1");
        MessageDTO dto = MessageDTO.create("CODE", "Title", "Content", "TYPE", "CAT", "FUNC");
        when(findMessageByCodeAndEnvironmentUseCaseFacade.execute(eq("CODE"), any(MessageAccessContext.class)))
                .thenReturn(dto);

        controller.findByCodeMessageAndEnvironment("CODE", request, response);

        ArgumentCaptor<MessageAccessContext> contextCaptor = ArgumentCaptor.forClass(MessageAccessContext.class);
        verify(findMessageByCodeAndEnvironmentUseCaseFacade).execute(eq("CODE"), contextCaptor.capture());
        assertThat(contextCaptor.getValue().legacyEnvironmentId()).isEqualTo("env-1");
        verify(restPresenter).presentRestSuccess(List.of(dto), request, response);
    }

    @Test
    void translateByCodeMessageAndEnvironment_presentsTranslatedMessage() {
        when(request.getAttribute("environmentId")).thenReturn("env-1");
        TranslatedMessageDTO dto = TranslatedMessageDTO.create("CODE", "es", "en", "T", "C", "TT", "TC",
                "TYPE", "CAT", "APP", "FUNC", "provider", "model", 10);
        TranslateMessageDTO body = new TranslateMessageDTO();
        body.setSourceLanguage("es");
        body.setTargetLanguage("en");
        when(translateMessageByCodeAndEnvironmentUseCaseFacade.execute(
                eq("CODE"), any(MessageAccessContext.class), eq("es"), eq("en")))
                .thenReturn(dto);

        controller.translateByCodeMessageAndEnvironment("CODE", body, request, response);

        ArgumentCaptor<MessageAccessContext> contextCaptor = ArgumentCaptor.forClass(MessageAccessContext.class);
        verify(translateMessageByCodeAndEnvironmentUseCaseFacade)
                .execute(eq("CODE"), contextCaptor.capture(), eq("es"), eq("en"));
        assertThat(contextCaptor.getValue().legacyEnvironmentId()).isEqualTo("env-1");
        verify(translationPresenter).presentRestSuccess(List.of(dto), request, response);
    }

    @Test
    void translateByCodeMessageAndEnvironment_usesAutomaticSourceLanguageWhenBodyOmitsIt() {
        when(request.getAttribute("environmentId")).thenReturn("env-1");
        TranslatedMessageDTO dto = TranslatedMessageDTO.create("CODE", "auto", "en", "T", "C", "TT", "TC",
                "TYPE", "CAT", "APP", "FUNC", "provider", "model", 10);
        TranslateMessageDTO body = new TranslateMessageDTO();
        body.setTargetLanguage("en");
        when(translateMessageByCodeAndEnvironmentUseCaseFacade.execute(
                eq("CODE"), any(MessageAccessContext.class), eq("auto"), eq("en")))
                .thenReturn(dto);

        controller.translateByCodeMessageAndEnvironment("CODE", body, request, response);

        verify(translateMessageByCodeAndEnvironmentUseCaseFacade)
                .execute(eq("CODE"), any(MessageAccessContext.class), eq("auto"), eq("en"));
        verify(translationPresenter).presentRestSuccess(List.of(dto), request, response);
    }

    @Test
    void findByCodeMessageAndEnvironment_propagatesExternalIdentityAlongsideLegacyAttribute() {
        when(request.getAttribute("environmentId")).thenReturn("legacy-env");
        when(request.getAttribute("externalIdentity")).thenReturn(IDENTITY);
        MessageDTO dto = MessageDTO.create("CODE", "Title", "Content", "TYPE", "CAT", "FUNC");
        when(findMessageByCodeAndEnvironmentUseCaseFacade.execute(eq("CODE"), any(MessageAccessContext.class)))
                .thenReturn(dto);

        controller.findByCodeMessageAndEnvironment("CODE", request, response);

        ArgumentCaptor<MessageAccessContext> contextCaptor = ArgumentCaptor.forClass(MessageAccessContext.class);
        verify(findMessageByCodeAndEnvironmentUseCaseFacade).execute(eq("CODE"), contextCaptor.capture());
        assertThat(contextCaptor.getValue())
                .extracting(MessageAccessContext::legacyEnvironmentId, MessageAccessContext::externalIdentity)
                .containsExactly("legacy-env", IDENTITY);
    }
}
