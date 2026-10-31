package co.edu.uco.application.usecase.validator.specification.impl;

import co.edu.uco.application.usecase.validator.specification.Specification;
import co.edu.uco.crosscutting.exceptions.CrossWordsException;

import static co.edu.uco.application.CrosswordsConstant.DATE_PATTERN;
import static co.edu.uco.crosscutting.helpers.UtilDate.parseDate;
import static co.edu.uco.crosscutting.helpers.UtilText.isEmptyOrNull;

/**
 * Especificación que se cumple cuando un texto es una fecha con formato y valor válidos.
 */
public final class ValidDateSpecification implements Specification<String> {

    @Override
    public boolean isSatisfiedBy(String candidate) {
        if (isEmptyOrNull(candidate) || !candidate.matches(DATE_PATTERN)) {
            return false;
        }
        try {
            parseDate(candidate);
            return true;
        } catch (CrossWordsException exception) {
            return false;
        }
    }
}