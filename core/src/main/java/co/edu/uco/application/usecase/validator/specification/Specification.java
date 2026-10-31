package co.edu.uco.application.usecase.validator.specification;

/**
 * Especificación reutilizable que decide si un candidato cumple una regla de negocio.
 * No lanza excepciones: solo evalúa y permite componerse con {@code and()}, {@code or()}
 * y {@code not()}.
 *
 * @param <T> tipo del candidato evaluado
 */
@FunctionalInterface
public interface Specification<T> {

    boolean isSatisfiedBy(T candidate);

    default Specification<T> and(Specification<T> other) {
        return candidate -> isSatisfiedBy(candidate) && other.isSatisfiedBy(candidate);
    }

    default Specification<T> or(Specification<T> other) {
        return candidate -> isSatisfiedBy(candidate) || other.isSatisfiedBy(candidate);
    }

    default Specification<T> not() {
        return candidate -> !isSatisfiedBy(candidate);
    }
}