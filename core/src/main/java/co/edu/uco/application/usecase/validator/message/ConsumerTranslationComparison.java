package co.edu.uco.application.usecase.validator.message;

import co.edu.uco.application.common.catalog.strategy.ConsumerTranslationSnapshot;

public record ConsumerTranslationComparison(ConsumerTranslationSnapshot before, ConsumerTranslationSnapshot after) {
}
