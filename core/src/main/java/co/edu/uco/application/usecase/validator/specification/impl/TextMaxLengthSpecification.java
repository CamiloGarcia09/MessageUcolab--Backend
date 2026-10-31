package co.edu.uco.application.usecase.validator.specification.impl;

import co.edu.uco.application.usecase.validator.specification.Specification;

import static co.edu.uco.crosscutting.helpers.UtilText.isEmptyOrNull;
import static co.edu.uco.crosscutting.helpers.UtilText.trim;

/**
 * Especificación que se cumple cuando un texto, una vez recortado, tiene una longitud máxima.
 */
public final class TextMaxLengthSpecification implements Specification<String> {

    private final int maxLength;

    public TextMaxLengthSpecification(int maxLength) {
        this.maxLength = maxLength;
    }

    @Override
    public boolean isSatisfiedBy(String candidate) {
        return isEmptyOrNull(candidate) || trim(candidate).length() <= maxLength;
    }
}