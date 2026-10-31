package co.edu.uco.application.usecase.validator.specification.impl;

import co.edu.uco.application.usecase.validator.specification.Specification;

import static co.edu.uco.crosscutting.helpers.UtilText.isEmptyOrNull;
import static co.edu.uco.crosscutting.helpers.UtilText.trim;

/**
 * Especificación compuesta que un texto recortado tenga una longitud entre un mínimo y un máximo.
 * Demuestra composición reutilizable de especificaciones mediante {@code and()}.
 */
public final class TextLengthRangeSpecification implements Specification<String> {

    private final Specification<String> minLength;
    private final Specification<String> maxLength;

    public TextLengthRangeSpecification(int minLength, int maxLength) {
        this.minLength = new TextMinLengthSpecification(minLength);
        this.maxLength = new TextMaxLengthSpecification(maxLength);
    }

    @Override
    public boolean isSatisfiedBy(String candidate) {
        return isEmptyOrNull(candidate) || minLength.and(maxLength).isSatisfiedBy(trim(candidate));
    }
}