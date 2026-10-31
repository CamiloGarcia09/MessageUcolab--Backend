package co.edu.uco.application.usecase;

import co.edu.uco.application.common.catalog.strategy.ConsumerMessageTranslationStrategy;
import co.edu.uco.application.common.catalog.strategy.ConsumerTranslationSnapshot;
import co.edu.uco.application.primaryports.dto.message.ConsumerTranslatedMessageDTO;
import co.edu.uco.application.primaryports.facade.message.ResolveMessageUseCaseFacade;
import co.edu.uco.application.primaryports.facade.message.TranslateConsumerMessageUseCaseFacade;
import co.edu.uco.application.secondaryports.repository.ApplicationLanguageRepository;
import co.edu.uco.application.usecase.domain.security.MessageAccessContext;
import co.edu.uco.application.usecase.validator.message.ApplicationBaseLanguageValidator;
import co.edu.uco.application.usecase.validator.message.ConsumerTranslationComparison;
import co.edu.uco.application.usecase.validator.message.ConsumerTranslationConsistencyValidator;
import co.edu.uco.application.usecase.validator.message.ConsumerTranslationLanguageValidator;

import java.util.Locale;

import static co.edu.uco.crosscutting.helpers.UtilText.trim;

public final class TranslateConsumerMessageUseCase implements TranslateConsumerMessageUseCaseFacade {
    private final ResolveMessageUseCaseFacade resolver;
    private final ApplicationLanguageRepository languages;
    private final ConsumerMessageTranslationStrategy translations;
    private final ConsumerTranslationLanguageValidator targetValidator;
    private final ApplicationBaseLanguageValidator sourceValidator;
    private final ConsumerTranslationConsistencyValidator consistencyValidator;

    public TranslateConsumerMessageUseCase(ResolveMessageUseCaseFacade resolver, ApplicationLanguageRepository languages,
                                            ConsumerMessageTranslationStrategy translations,
                                            ConsumerTranslationLanguageValidator targetValidator,
                                            ApplicationBaseLanguageValidator sourceValidator,
                                            ConsumerTranslationConsistencyValidator consistencyValidator) {
        this.resolver = resolver;
        this.languages = languages;
        this.translations = translations;
        this.targetValidator = targetValidator;
        this.sourceValidator = sourceValidator;
        this.consistencyValidator = consistencyValidator;
    }

    @Override
    public ConsumerTranslatedMessageDTO execute(String code, String environmentId, MessageAccessContext context,
                                                 String targetLanguage) {
        var target = normalizeLanguage(targetLanguage);
        targetValidator.validate(target);
        var before = snapshot(code, environmentId, context);
        var translation = translations.translate(before, context.applicationId(), environmentId, target);
        var after = snapshot(code, environmentId, context);
        consistencyValidator.validate(new ConsumerTranslationComparison(before, after));
        return new ConsumerTranslatedMessageDTO(before.message(), before.sourceLanguage(), target, translation);
    }

    private ConsumerTranslationSnapshot snapshot(String code, String environmentId, MessageAccessContext context) {
        // Reuse the consumer operation: ownership, environment, active message and active association.
        var message = resolver.execute(code, environmentId, context);
        var source = normalizeLanguage(languages.findBaseLanguageCode(context.applicationId()).orElse(""));
        sourceValidator.validate(source);
        return new ConsumerTranslationSnapshot(message, source);
    }

    private static String normalizeLanguage(String language) {
        return trim(language).replace('_', '-').toLowerCase(Locale.ROOT);
    }
}
