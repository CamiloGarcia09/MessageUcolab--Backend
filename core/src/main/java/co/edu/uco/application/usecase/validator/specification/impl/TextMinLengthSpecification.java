package co.edu.uco.application.usecase.validator.specification.impl;

import co.edu.uco.application.usecase.validator.specification.Specification;

import static co.edu.uco.crosscutting.helpers.UtilText.isEmptyOrNull;
import static co.edu.uco.crosscutting.helpers.UtilText.trim;

/**
 * Especificación que se cumple cuando un texto, una vez recortado, tiene una longitud mínima.
 */
public final class TextMinLengthSpecification implements Specification<String> {

    private final int minLength;

    public TextMinLengthSpecification(int minLength) {
        this.minLength = minLength;
    }

    @Override
    public boolean isSatisfiedBy(String candidate) {
        return !isEmptyOrNull(candidate) && trim(candidate).length() >= minLength;
    }
}