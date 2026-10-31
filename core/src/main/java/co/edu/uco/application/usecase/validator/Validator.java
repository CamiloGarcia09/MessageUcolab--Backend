package co.edu.uco.application.usecase.validator;

import co.edu.uco.crosscutting.exceptions.BusinessRuleException;

public interface Validator<D> {
    void validate(D data) throws BusinessRuleException;
}