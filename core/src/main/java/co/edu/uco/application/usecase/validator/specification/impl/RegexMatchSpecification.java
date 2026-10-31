package co.edu.uco.application.usecase.validator.specification.impl;

import co.edu.uco.application.usecase.validator.specification.Specification;

import static co.edu.uco.crosscutting.helpers.UtilText.isEmptyOrNull;
import static co.edu.uco.crosscutting.helpers.UtilText.validMatch;

/**
 * Especificación que se cumple cuando un texto no vacío coincide con una expresión regular.
 */
public final class RegexMatchSpecification implements Specification<String> {

    private final String pattern;

    public RegexMatchSpecification(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public boolean isSatisfiedBy(String candidate) {
        return !isEmptyOrNull(candidate) && validMatch(candidate, pattern);
    }
}