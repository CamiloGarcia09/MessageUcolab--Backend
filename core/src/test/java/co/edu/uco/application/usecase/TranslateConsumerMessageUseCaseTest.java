package co.edu.uco.application.usecase;

import co.edu.uco.application.common.catalog.strategy.ConsumerMessageTranslationStrategy;
import co.edu.uco.application.common.catalog.strategy.ConsumerTranslationSnapshot;
import co.edu.uco.application.primaryports.dto.message.ResolvedMessageDTO;
import co.edu.uco.application.primaryports.facade.message.ResolveMessageUseCaseFacade;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.entity.MessageTranslationResponseData;
import co.edu.uco.application.secondaryports.repository.ApplicationLanguageRepository;
import co.edu.uco.application.secondaryports.translation.MessageTranslationCachePort;
import co.edu.uco.application.secondaryports.translation.MessageTranslationPort;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;
import co.edu.uco.application.usecase.validator.message.ApplicationBaseLanguageValidator;
import co.edu.uco.application.usecase.validator.message.ConsumerTranslationConsistencyValidator;
import co.edu.uco.application.usecase.validator.message.ConsumerTranslationLanguageValidator;
import co.edu.uco.crosscutting.exceptions.ConflictException;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import co.edu.uco.crosscutting.exceptions.CrossWordsException;
import co.edu.uco.crosscutting.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TranslateConsumerMessageUseCaseTest {
    @Mock private ResolveMessageUseCaseFacade resolver;
    @Mock private ApplicationLanguageRepository languages;
    @Mock private MessageTranslationCachePort cache;
    @Mock private MessageTranslationPort translator;
    @Mock private CatalogPort catalog;

    private TranslateConsumerMessageUseCase useCase;
    private final MessageAccessContext context = new MessageAccessContext("app-id");

    @BeforeEach
    void setUp() {
        var strategy = new ConsumerMessageTranslationStrategy(cache, translator, catalog);
        useCase = new TranslateConsumerMessageUseCase(resolver, languages, strategy,
                new ConsumerTranslationLanguageValidator(catalog),
                new ApplicationBaseLanguageValidator(catalog),
                new ConsumerTranslationConsistencyValidator(catalog));
    }

    @Test
    void execute_rejectsInactiveMessageBeforeReadingTranslationCache() {
        when(resolver.execute("MSG", "env-id", context)).thenThrow(NotFoundException.buildUserException("unavailable"));

        assertThatThrownBy(() -> useCase.execute("MSG", "env-id", context, "es"))
                .isInstanceOf(NotFoundException.class);

        verify(cache, never()).findTranslation(any(), any(), any(), any());
        verify(translator, never()).translate(any());
        verify(languages, never()).findBaseLanguageCode(any());
    }

    @Test
    void execute_revalidatesAvailabilityAfterCacheHit() {
        var message = message("title", "content", Instant.parse("2026-01-01T00:00:00Z"));
        when(languages.findBaseLanguageCode("app-id")).thenReturn(Optional.of(" EN "));
        when(resolver.execute("MSG", "env-id", context)).thenReturn(message)
                .thenThrow(NotFoundException.buildUserException("disabled during request"));
        var snapshot = new ConsumerTranslationSnapshot(message, "en");
        when(cache.findTranslation(eq("MSG"), org.mockito.ArgumentMatchers.contains("consumer-v1:app-id:env-id:"), eq("en"), eq("es")))
                .thenReturn(Optional.of(translated("cached")));

        assertThatThrownBy(() -> useCase.execute("MSG", "env-id", context, "es"))
                .isInstanceOf(NotFoundException.class);

        verify(resolver, times(2)).execute("MSG", "env-id", context);
        verify(translator, never()).translate(any());
    }

    @Test
    void execute_doesNotReturnCacheHitWhenFinalAvailabilityCheckFailsTechnically() {
        var message = message("title", "content", Instant.parse("2026-01-01T00:00:00Z"));
        when(languages.findBaseLanguageCode("app-id")).thenReturn(Optional.of("en"));
        var technicalFailure = CrossWordsException.buildInfrastructure("database unavailable",
                new IllegalStateException("database unavailable"));
        when(resolver.execute("MSG", "env-id", context)).thenReturn(message).thenThrow(technicalFailure);
        when(cache.findTranslation(eq("MSG"), any(), eq("en"), eq("es")))
                .thenReturn(Optional.of(translated("cached")));

        assertThatThrownBy(() -> useCase.execute("MSG", "env-id", context, "es"))
                .isSameAs(technicalFailure);
    }

    @Test
    void execute_rejectsMessageThatBecomesUnavailableDuringTranslation() {
        when(languages.findBaseLanguageCode("app-id")).thenReturn(Optional.of("en"));
        when(resolver.execute("MSG", "env-id", context)).thenReturn(message("title", "content", null))
                .thenThrow(NotFoundException.buildUserException("disabled"));
        when(cache.findTranslation(eq("MSG"), any(), eq("en"), eq("es"))).thenReturn(Optional.empty());
        when(translator.translate(any())).thenReturn(translated("translation"));

        assertThatThrownBy(() -> useCase.execute("MSG", "env-id", context, "es"))
                .isInstanceOf(NotFoundException.class);
        verify(cache).saveTranslation(eq("MSG"), any(), eq("en"), eq("es"), any());
    }

    @Test
    void execute_rejectsMessageChangedDuringTranslation() {
        when(resolver.execute("MSG", "env-id", context))
                .thenReturn(message("old", "content", Instant.parse("2026-01-01T00:00:00Z")))
                .thenReturn(message("new", "content", Instant.parse("2026-01-02T00:00:00Z")));
        when(languages.findBaseLanguageCode("app-id")).thenReturn(Optional.of("en"));
        when(cache.findTranslation(any(), any(), any(), any())).thenReturn(Optional.empty());
        when(translator.translate(any())).thenReturn(translated("nuevo"));
        when(catalog.getMessage("FUN_220")).thenReturn("changed");

        assertThatThrownBy(() -> useCase.execute("MSG", "env-id", context, "es"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void execute_normalizesTargetAndUsesApplicationBaseLanguage() {
        var message = message("Title", "Content", Instant.parse("2026-01-01T00:00:00Z"));
        when(languages.findBaseLanguageCode("app-id")).thenReturn(Optional.of(" EN "));
        when(resolver.execute("MSG", "env-id", context)).thenReturn(message);
        when(cache.findTranslation(eq("MSG"), any(), eq("en"), eq("zh-hans"))).thenReturn(Optional.empty());
        when(translator.translate(any())).thenReturn(translated("Translated"));

        var result = useCase.execute("MSG", "env-id", context, " ZH_HANS ");

        assertThat(result.getSourceLanguage()).isEqualTo("en");
        assertThat(result.getTargetLanguage()).isEqualTo("zh-hans");
        verify(resolver, times(2)).execute("MSG", "env-id", context);
        verify(languages, times(2)).findBaseLanguageCode("app-id");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "English", "es-123456789"})
    void execute_rejectsMissingOrMalformedTargetBeforeResolvingMessage(String target) {
        org.mockito.Mockito.lenient().when(catalog.getMessage("FUN_010")).thenReturn("target required");
        org.mockito.Mockito.lenient().when(catalog.getMessage("FUN_044")).thenReturn("target required");
        org.mockito.Mockito.lenient().when(catalog.getMessage("FUN_045")).thenReturn("target malformed");

        assertThatThrownBy(() -> useCase.execute("MSG", "env-id", context, target))
                .isInstanceOf(BusinessRuleException.class);
        verify(resolver, never()).execute(any(), any(), any());
        verify(translator, never()).translate(any());
    }

    @Test
    void execute_rejectsChangedBaseLanguageDuringTranslation() {
        var message = message("title", "content", null);
        when(resolver.execute("MSG", "env-id", context)).thenReturn(message);
        when(languages.findBaseLanguageCode("app-id")).thenReturn(Optional.of("en"), Optional.of("fr"));
        when(cache.findTranslation(eq("MSG"), any(), eq("en"), eq("es"))).thenReturn(Optional.empty());
        when(translator.translate(any())).thenReturn(translated("translated"));
        when(catalog.getMessage("FUN_220")).thenReturn("changed");

        assertThatThrownBy(() -> useCase.execute("MSG", "env-id", context, "es"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void execute_rejectsMissingBaseLanguageWithoutTranslation() {
        when(languages.findBaseLanguageCode("app-id")).thenReturn(Optional.empty());
        when(resolver.execute("MSG", "env-id", context)).thenReturn(message("title", "content", null));
        when(catalog.getMessage("FUN_219")).thenReturn("invalid base language");

        assertThatThrownBy(() -> useCase.execute("MSG", "env-id", context, "es"))
                .isInstanceOf(BusinessRuleException.class);

        verify(cache, never()).findTranslation(any(), any(), any(), any());
        verify(translator, never()).translate(any());
    }

    @Test
    void execute_sameLanguageStillRevalidatesMessageAvailability() {
        var message = message("Title", "Content", null);
        when(languages.findBaseLanguageCode("app-id")).thenReturn(Optional.of("en"));
        when(resolver.execute("MSG", "env-id", context)).thenReturn(message);

        var result = useCase.execute("MSG", "env-id", context, "EN");

        assertThat(result.getTitle()).isEqualTo("Title");
        verify(resolver, times(2)).execute("MSG", "env-id", context);
        verify(cache, never()).findTranslation(any(), any(), any(), any());
        verify(translator, never()).translate(any());
    }

    private static ResolvedMessageDTO message(String title, String content, Instant updatedAt) {
        return new ResolvedMessageDTO("MSG", title, content, "type", "category", "functionality",
                Instant.parse("2025-12-01T00:00:00Z"), updatedAt);
    }

    private static MessageTranslationResponseData translated(String title) {
        return MessageTranslationResponseData.create(title, "translated content", "tipo", "categoría",
                "funcionalidad", "provider", "model", 3L);
    }
}
