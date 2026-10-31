package co.edu.uco.application.usecase.validator.specification.impl;

import co.edu.uco.application.usecase.validator.specification.Specification;

import static co.edu.uco.crosscutting.helpers.UtilText.isEmptyOrNull;

/**
 * Especificación que se cumple cuando un texto no es nulo ni vacío (ignorando espacios).
 */
public final class TextRequiredSpecification implements Specification<String> {

    @Override
    public boolean isSatisfiedBy(String candidate) {
        return !isEmptyOrNull(candidate);
    }
}