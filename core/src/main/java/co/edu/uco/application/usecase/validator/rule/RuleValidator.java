package co.edu.uco.application.usecase.validator.rule;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.validator.Validator;
import co.edu.uco.application.usecase.validator.specification.Specification;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;

import java.util.function.Function;
import java.util.function.BiFunction;

/**
 * Base común para reglas de negocio individuales. Cada regla encapsula una
 * {@link Specification} reutilizable y el código de catálogo con el que se
 * resuelve el mensaje que recibe el usuario cuando la regla no se cumple.
 *
 * @param <T> tipo de dato sobre el que se aplica la regla
 */
public abstract class RuleValidator<T> implements Validator<T> {

    private final CatalogPort catalogPort;
    private final Specification<T> rule;
    private final MessageCatalogCodeEnum catalogCode;
    private final Function<String, ? extends RuntimeException> exceptionFactory;
    private final BiFunction<String, T, String> messageFormatter;

    protected RuleValidator(CatalogPort catalogPort, Specification<T> rule, MessageCatalogCodeEnum catalogCode) {
        this(catalogPort, rule, catalogCode, BusinessRuleException::buildUserException, (message, data) -> message);
    }

    protected RuleValidator(CatalogPort catalogPort, Specification<T> rule, MessageCatalogCodeEnum catalogCode,
                            Function<String, ? extends RuntimeException> exceptionFactory) {
        this(catalogPort, rule, catalogCode, exceptionFactory, (message, data) -> message);
    }

    protected RuleValidator(CatalogPort catalogPort, Specification<T> rule, MessageCatalogCodeEnum catalogCode,
                            Function<String, ? extends RuntimeException> exceptionFactory,
                            BiFunction<String, T, String> messageFormatter) {
        this.catalogPort = catalogPort;
        this.rule = rule;
        this.catalogCode = catalogCode;
        this.exceptionFactory = exceptionFactory;
        this.messageFormatter = messageFormatter;
    }

    @Override
    public final void validate(T data) {
        if (!rule.isSatisfiedBy(data)) {
            String message = catalogPort.getMessage(catalogCode.getCode());
            throw exceptionFactory.apply(messageFormatter.apply(message, data));
        }
    }
}
