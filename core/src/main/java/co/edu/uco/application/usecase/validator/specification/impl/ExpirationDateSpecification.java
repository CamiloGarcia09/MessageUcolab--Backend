package co.edu.uco.application.usecase.validator.specification.impl;

import co.edu.uco.application.usecase.validator.specification.Specification;

import java.time.LocalDateTime;

/**
 * Especificación que se cumple cuando una fecha de expiración es posterior a la fecha/hora actual.
 */
public final class ExpirationDateSpecification implements Specification<LocalDateTime> {

    private final LocalDateTime now;

    public ExpirationDateSpecification(LocalDateTime now) {
        this.now = now;
    }

    @Override
    public boolean isSatisfiedBy(LocalDateTime candidate) {
        return candidate != null && candidate.isAfter(now);
    }
}