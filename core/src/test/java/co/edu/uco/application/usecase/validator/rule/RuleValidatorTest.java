package co.edu.uco.application.usecase.validator.rule;

import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.application.usecase.validator.specification.Specification;
import co.edu.uco.crosscutting.catalog.MessageCatalogCodeEnum;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import co.edu.uco.crosscutting.exceptions.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleValidatorTest {

    private static final String ACCEPTED_MESSAGE = "accepted";
    private static final String REJECTED_MESSAGE = "rejected";
    private static final String CATALOG_RULE_MESSAGE = "The rule was not satisfied.";
    private static final String CATALOG_FORBIDDEN_MESSAGE = "The operation is not allowed.";

    @Mock
    private CatalogPort catalogPort;

    private final AtomicReference<String> receivedCandidate = new AtomicReference<>();
    private final AtomicInteger messageFormatterInvocations = new AtomicInteger();

    private RuleValidator<String> defaultFactoryRule;
    private RuleValidator<String> forbiddenFactoryRule;
    private RuleValidator<String> formattedMessageRule;

    @BeforeEach
    void setUp() {
        defaultFactoryRule = new DefaultFactoryRule(catalogPort, this::isAccepted);
        forbiddenFactoryRule = new ForbiddenFactoryRule(catalogPort, this::isAccepted);
        formattedMessageRule = new FormattedMessageRule(catalogPort, this::isAccepted, messageFormatterInvocations);
    }

    @Test
    void validate_doesNotThrow_whenTheSpecificationIsSatisfied() {
        assertThatCode(() -> defaultFactoryRule.validate(ACCEPTED_MESSAGE)).doesNotThrowAnyException();

        verifyNoInteractions(catalogPort);
    }

    @Test
    void validate_forwardsTheValidatedCandidateToTheSpecification() {
        defaultFactoryRule.validate(ACCEPTED_MESSAGE);

        assertThat(receivedCandidate).hasValue(ACCEPTED_MESSAGE);
        verifyNoInteractions(catalogPort);
    }

    @Test
    void validate_throwsBusinessRuleUsingTheCatalogMessage_whenTheSpecificationIsNotSatisfied() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_040.getCode()))
                .thenReturn(CATALOG_RULE_MESSAGE);

        assertThatThrownBy(() -> defaultFactoryRule.validate(REJECTED_MESSAGE))
                .isInstanceOf(BusinessRuleException.class)
                .isNotInstanceOf(ForbiddenException.class)
                .satisfies(exception -> assertThat((BusinessRuleException) exception)
                        .extracting(BusinessRuleException::getHttpStatus, BusinessRuleException::getUserMessage)
                        .containsExactly(422, CATALOG_RULE_MESSAGE));
        verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_040.getCode());
    }

    @Test
    void validate_throwsForbidden_whenACustomExceptionFactoryIsProvided() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_153.getCode()))
                .thenReturn(CATALOG_FORBIDDEN_MESSAGE);

        assertThatThrownBy(() -> forbiddenFactoryRule.validate(REJECTED_MESSAGE))
                .isInstanceOf(ForbiddenException.class)
                .satisfies(exception -> assertThat((ForbiddenException) exception)
                        .extracting(ForbiddenException::getHttpStatus, ForbiddenException::getUserMessage)
                        .containsExactly(403, CATALOG_FORBIDDEN_MESSAGE));
        verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_153.getCode());
    }

    @Test
    void validate_doesNotUseTheCustomExceptionFactory_whenTheSpecificationIsSatisfied() {
        assertThatCode(() -> forbiddenFactoryRule.validate(ACCEPTED_MESSAGE)).doesNotThrowAnyException();

        verifyNoInteractions(catalogPort);
    }

    @Test
    void validate_throwsBusinessRule_whenCandidateIsNull() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_040.getCode()))
                .thenReturn(CATALOG_RULE_MESSAGE);

        assertThatThrownBy(() -> defaultFactoryRule.validate(null))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(exception -> assertThat(receivedCandidate).hasValue(null));
        verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_040.getCode());
    }

    @Test
    void validate_appliesTheCustomMessageFormatterToTheCatalogMessage_whenTheSpecificationIsNotSatisfied() {
        when(catalogPort.getMessage(MessageCatalogCodeEnum.FUN_040.getCode()))
                .thenReturn(CATALOG_RULE_MESSAGE);

        assertThatThrownBy(() -> formattedMessageRule.validate(REJECTED_MESSAGE))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(exception -> assertThat((BusinessRuleException) exception)
                        .extracting(BusinessRuleException::getHttpStatus, BusinessRuleException::getUserMessage)
                        .containsExactly(422, CATALOG_RULE_MESSAGE + " -> " + REJECTED_MESSAGE));
        assertThat(messageFormatterInvocations).hasValue(1);
        verify(catalogPort).getMessage(MessageCatalogCodeEnum.FUN_040.getCode());
    }

    @Test
    void validate_doesNotApplyTheCustomMessageFormatter_whenTheSpecificationIsSatisfied() {
        assertThatCode(() -> formattedMessageRule.validate(ACCEPTED_MESSAGE)).doesNotThrowAnyException();

        assertThat(messageFormatterInvocations).hasValue(0);
        verifyNoInteractions(catalogPort);
    }

    private boolean isAccepted(String candidate) {
        receivedCandidate.set(candidate);
        return ACCEPTED_MESSAGE.equals(candidate);
    }

    private static final class DefaultFactoryRule extends RuleValidator<String> {

        private DefaultFactoryRule(CatalogPort catalogPort, Specification<String> specification) {
            super(catalogPort, specification, MessageCatalogCodeEnum.FUN_040);
        }
    }

    private static final class ForbiddenFactoryRule extends RuleValidator<String> {

        private ForbiddenFactoryRule(CatalogPort catalogPort, Specification<String> specification) {
            super(catalogPort, specification, MessageCatalogCodeEnum.FUN_153,
                    ForbiddenException::buildUserException);
        }
    }

    private static final class FormattedMessageRule extends RuleValidator<String> {

        private FormattedMessageRule(CatalogPort catalogPort, Specification<String> specification,
                                     AtomicInteger messageFormatterInvocations) {
            super(catalogPort, specification, MessageCatalogCodeEnum.FUN_040,
                    BusinessRuleException::buildUserException,
                    (message, candidate) -> {
                        messageFormatterInvocations.incrementAndGet();
                        return message + " -> " + candidate;
                    });
        }
    }
}
