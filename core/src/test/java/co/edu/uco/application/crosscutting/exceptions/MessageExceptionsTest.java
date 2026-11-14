package co.edu.uco.application.crosscutting.exceptions;

import co.edu.uco.application.common.catalog.CatalogPortStaticRef;
import co.edu.uco.application.secondaryports.catalog.CatalogPort;
import co.edu.uco.crosscutting.exceptions.CrossWordsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MessageExceptionsTest {

    @BeforeEach
    void setUp() {
        CatalogPort catalogPort = mock(CatalogPort.class);
        when(catalogPort.getMessage(anyString())).thenReturn("mensaje");
        when(catalogPort.getTitle(anyString())).thenReturn("titulo");
        CatalogPortStaticRef.set(catalogPort);
    }

    @Test
    void titleCanNotBeEmpty_report_throwsBusinessRuleException() {
        assertThatThrownBy(TitleCanNotBeEmptyException::report)
                .isInstanceOf(TitleCanNotBeEmptyException.class)
                .isInstanceOf(CrossWordsException.class);
    }

    @Test
    void titleBelowMinimumLength_report_throwsBusinessRuleException() {
        assertThatThrownBy(TitleBelowMinimumLengthException::report)
                .isInstanceOf(TitleBelowMinimumLengthException.class)
                .isInstanceOf(CrossWordsException.class);
    }

    @Test
    void titleExceedsMaximumLength_report_throwsBusinessRuleException() {
        assertThatThrownBy(TitleExceedsMaximumLengthException::report)
                .isInstanceOf(TitleExceedsMaximumLengthException.class)
                .isInstanceOf(CrossWordsException.class);
    }

    @Test
    void contentCanNotBeEmpty_report_throwsBusinessRuleException() {
        assertThatThrownBy(ContentCanNotBeEmptyException::report)
                .isInstanceOf(ContentCanNotBeEmptyException.class)
                .isInstanceOf(CrossWordsException.class);
    }

    @Test
    void contentBelowMinimumLength_report_throwsBusinessRuleException() {
        assertThatThrownBy(ContentBelowMinimumLengthException::report)
                .isInstanceOf(ContentBelowMinimumLengthException.class)
                .isInstanceOf(CrossWordsException.class);
    }

    @Test
    void contentExceedsMaximumLength_report_throwsBusinessRuleException() {
        assertThatThrownBy(ContentExceedsMaximumLengthException::report)
                .isInstanceOf(ContentExceedsMaximumLengthException.class)
                .isInstanceOf(CrossWordsException.class);
    }

    @Test
    void messageNotFound_report_throwsMessageNotFoundException() {
        assertThatThrownBy(() -> MessageNotFoundException.report("MSG-001"))
                .isInstanceOf(MessageNotFoundException.class)
                .isInstanceOf(CrossWordsException.class);
    }

    @Test
    void messageKeyCanNotBeEmpty_report_throwsMessageKeyCanNotBeEmptyException() {
        assertThatThrownBy(MessageKeyCanNotBeEmptyException::report)
                .isInstanceOf(MessageKeyCanNotBeEmptyException.class)
                .isInstanceOf(CrossWordsException.class);
    }

    @Test
    void messageKeyCanNotBeNull_report_throwsMessageKeyCanNotBeNullException() {
        assertThatThrownBy(MessageKeyCanNotBeNullException::report)
                .isInstanceOf(MessageKeyCanNotBeNullException.class)
                .isInstanceOf(CrossWordsException.class);
    }
}
