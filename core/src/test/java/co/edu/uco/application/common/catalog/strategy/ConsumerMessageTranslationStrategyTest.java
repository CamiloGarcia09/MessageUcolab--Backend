package co.edu.uco.application.common.catalog.strategy;

import co.edu.uco.application.primaryports.dto.message.ResolvedMessageDTO;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.secondaryports.entity.MessageTranslationResponseData;
import co.edu.uco.application.secondaryports.translation.MessageTranslationCachePort;
import co.edu.uco.application.secondaryports.translation.MessageTranslationPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.eq;

@ExtendWith(MockitoExtension.class)
class ConsumerMessageTranslationStrategyTest {
    @Mock private MessageTranslationCachePort cache;
    @Mock private MessageTranslationPort translator;
    @Mock private CatalogPort catalog;

    private ConsumerMessageTranslationStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new ConsumerMessageTranslationStrategy(cache, translator, catalog);
    }

    @Test
    void translate_returnsCachedValueUsingConsumerScope_whenCacheHit() {
        var snapshot = snapshot("source");
        var cached = translated("cached title");
        when(cache.findTranslation("MSG", "consumer-v1:app:env:" + hash(snapshot), "en", "es"))
                .thenReturn(Optional.of(cached));

        var result = strategy.translate(snapshot, "app", "env", "es");

        assertThat(result).isSameAs(cached);
        verify(translator, never()).translate(any());
    }

    @Test
    void translate_translatesAndCachesOnMiss() {
        var snapshot = snapshot("source");
        var translated = translated("translated title");
        when(cache.findTranslation("MSG", "consumer-v1:app:env:" + hash(snapshot), "en", "es"))
                .thenReturn(Optional.empty());
        when(translator.translate(any())).thenReturn(translated);

        var result = strategy.translate(snapshot, "app", "env", "es");

        assertThat(result).isSameAs(translated);
        verify(cache).saveTranslation("MSG", "consumer-v1:app:env:" + hash(snapshot), "en", "es", translated);
    }

    @Test
    void translate_returnsOriginalWithoutCacheOrTranslator_whenLanguagesMatch() {
        var snapshot = snapshot("source");

        var result = strategy.translate(snapshot, "app", "env", "en");

        assertThat(result.getTranslatedTitle()).isEqualTo("Title");
        assertThat(result.getTranslatedContent()).isEqualTo("Content");
        verify(cache, never()).findTranslation(any(), any(), any(), any());
        verify(translator, never()).translate(any());
    }

    @Test
    void translate_usesDifferentScopeForDifferentApplicationAndEnvironment() {
        var snapshot = snapshot("source");
        var first = translated("first");
        when(cache.findTranslation("MSG", "consumer-v1:app-a:env-a:" + hash(snapshot), "en", "es"))
                .thenReturn(Optional.of(first));
        when(cache.findTranslation("MSG", "consumer-v1:app-b:env-a:" + hash(snapshot), "en", "es"))
                .thenReturn(Optional.of(translated("second")));

        var firstResult = strategy.translate(snapshot, "app-a", "env-a", "es");
        var secondResult = strategy.translate(snapshot, "app-b", "env-a", "es");

        assertThat(firstResult.getTranslatedTitle()).isEqualTo("first");
        assertThat(secondResult.getTranslatedTitle()).isEqualTo("second");
    }

    @Test
    void translate_usesVersionedContentHashAndStableNullDatesInCacheScope() {
        var first = snapshot("source");
        var revised = new ConsumerTranslationSnapshot(new ResolvedMessageDTO("MSG", "Revised", "Content",
                "type", "category", "function", null, null), "en");
        when(cache.findTranslation("MSG", "consumer-v1:app:env:" + hash(first), "en", "es"))
                .thenReturn(Optional.empty());
        when(cache.findTranslation("MSG", "consumer-v1:app:env:" + hash(revised), "en", "es"))
                .thenReturn(Optional.empty());
        when(translator.translate(any())).thenReturn(translated("output"));

        strategy.translate(first, "app", "env", "es");
        strategy.translate(revised, "app", "env", "es");

        assertThat(hash(first)).isNotEqualTo(hash(revised));
        verify(translator, times(2)).translate(any());
        verify(cache, times(2)).saveTranslation(org.mockito.ArgumentMatchers.eq("MSG"),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq("en"),
                org.mockito.ArgumentMatchers.eq("es"), any());
    }

    @Test
    void translate_separatesCacheEntriesByApplicationEnvironmentAndMessageVersion() {
        var original = snapshot("source");
        var revisedMessage = new ResolvedMessageDTO("MSG", "Revised", "Content",
                "type", "category", "function", null, null);
        var revised = new ConsumerTranslationSnapshot(revisedMessage, "en");
        when(cache.findTranslation(eq("MSG"), any(), eq("en"), eq("es"))).thenReturn(Optional.empty());
        when(translator.translate(any())).thenReturn(translated("output"));

        strategy.translate(original, "app", "env-a", "es");
        strategy.translate(original, "app", "env-b", "es");
        strategy.translate(revised, "app", "env-a", "es");

        var scope = ArgumentCaptor.forClass(String.class);
        verify(cache, times(3)).findTranslation(eq("MSG"), scope.capture(), eq("en"), eq("es"));
        assertThat(scope.getAllValues()).doesNotHaveDuplicates().hasSize(3);
        assertThat(scope.getAllValues().get(0)).startsWith("consumer-v1:app:env-a:");
        assertThat(scope.getAllValues().get(1)).startsWith("consumer-v1:app:env-b:");
        assertThat(scope.getAllValues().get(2)).startsWith("consumer-v1:app:env-a:");
        verify(translator, times(3)).translate(any());
    }

    @Test
    void translate_hashSeparatesValuesContainingDelimitersAndKeepsNullDatesStable() {
        var first = messageWithContent("a|b", "c");
        var second = messageWithContent("a", "b|c");
        var repeated = messageWithContent("a|b", "c");

        assertThat(hash(first)).isNotEqualTo(hash(second));
        assertThat(hash(first)).isEqualTo(hash(repeated));
    }

    private static ConsumerTranslationSnapshot snapshot(String title) {
        var message = new ResolvedMessageDTO("MSG", title.equals("source") ? "Title" : title,
                "Content", "type", "category", "function", null, null);
        return new ConsumerTranslationSnapshot(message, "en");
    }

    private static ConsumerTranslationSnapshot messageWithContent(String title, String content) {
        return new ConsumerTranslationSnapshot(new ResolvedMessageDTO("MSG", title, content,
                "type", "category", "function", null, null), "en");
    }

    private static MessageTranslationResponseData translated(String title) {
        return MessageTranslationResponseData.create(title, "Content translated", "type translated",
                "category translated", "function translated", "provider", "model", 1L);
    }

    private static String hash(ConsumerTranslationSnapshot snapshot) {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            for (var value : snapshot.values()) {
                var bytes = value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                digest.update(java.nio.ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
                digest.update(bytes);
            }
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new AssertionError(exception);
        }
    }
}
