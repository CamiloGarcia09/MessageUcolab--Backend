package co.edu.uco.application.usecase.validator.specification.impl;

import co.edu.uco.application.usecase.validator.specification.Specification;
import co.edu.uco.crosscutting.exceptions.CrossWordsException;

import static co.edu.uco.crosscutting.helpers.UtilText.isEmptyOrNull;
import static co.edu.uco.crosscutting.helpers.UtilUUID.DEFAULT_UUID;
import static co.edu.uco.crosscutting.helpers.UtilUUID.getUUIDFromString;
import static co.edu.uco.crosscutting.helpers.UtilUUID.isEqual;

/**
 * Especificación que se cumple cuando un texto es un UUID válido y no es el UUID por defecto.
 */
public final class ValidUuidSpecification implements Specification<String> {

    @Override
    public boolean isSatisfiedBy(String candidate) {
        if (isEmptyOrNull(candidate)) {
            return false;
        }
        try {
            return !isEqual(getUUIDFromString(candidate), DEFAULT_UUID);
        } catch (CrossWordsException exception) {
            return false;
        }
    }
}