package co.edu.uco.application.usecase.validator.specification.impl;

import co.edu.uco.application.usecase.validator.specification.Specification;

import static co.edu.uco.crosscutting.helpers.UtilObject.isNullObject;

/**
 * Especificación genérica que se cumple cuando el candidato no es nulo.
 *
 * @param <T> tipo del candidato evaluado
 */
public final class NotNullSpecification<T> implements Specification<T> {

    @Override
    public boolean isSatisfiedBy(T candidate) {
        return !isNullObject(candidate);
    }
}