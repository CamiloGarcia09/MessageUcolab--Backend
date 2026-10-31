package co.edu.uco.application.usecase.validator.specification;

import java.util.function.Function;

import static co.edu.uco.crosscutting.helpers.UtilObject.isNullObject;

/**
 * Utilidades para componer {@link Specification} a partir de la extracción de un campo.
 */
public final class Specifications {

    private Specifications() {
    }

    public static <T, F> Specification<T> field(Function<T, F> extractor, Specification<F> specification) {
        return (T candidate) -> !isNullObject(candidate) && specification.isSatisfiedBy(extractor.apply(candidate));
    }
}