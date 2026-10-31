package co.edu.uco.application.primaryports.facade.page.impl;

import co.edu.uco.application.common.mapper.SimplePageMapper;
import co.edu.uco.application.primaryports.dto.page.PageRequestDTO;
import co.edu.uco.application.secondaryports.repository.SimplePageRequest;
import co.edu.uco.application.usecase.validator.page.PageRequestDTOCompositeValidator;
import co.edu.uco.crosscutting.exceptions.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimplePageFacadeImplTest {

    private static final String NUMERIC_PATTERN_MESSAGE = "El atributo page debe ser numerico";

    @Mock
    private SimplePageMapper simplePageMapper;
    @Mock
    private PageRequestDTOCompositeValidator pageRequestValidator;

    @InjectMocks
    private SimplePageFacadeImpl facade;

    @Test
    void execute_validatesAndMapsPageRequest() {
        PageRequestDTO dto = PageRequestDTO.builder()
                .page("2").size("10").columnSort("title").sort("desc").build();
        SimplePageRequest request = new SimplePageRequest();
        when(simplePageMapper.toSimplePageRequest(dto)).thenReturn(request);

        SimplePageRequest result = facade.execute(dto);

        assertThat(result).isSameAs(request);
        verify(pageRequestValidator).validate(dto);
    }

    @Test
    void execute_mapsTheDefaultPageRequest_whenEveryAttributeIsNull() {
        PageRequestDTO dto = new PageRequestDTO();
        SimplePageRequest request = new SimplePageRequest();
        when(simplePageMapper.toSimplePageRequest(dto)).thenReturn(request);

        SimplePageRequest result = facade.execute(dto);

        assertThat(result).isSameAs(request);
        verify(pageRequestValidator).validate(dto);
    }

    @Test
    void execute_propagatesTheBusinessRule_whenThePageCompositeRejectsTheDto() {
        PageRequestDTO dto = PageRequestDTO.builder().page("abc").build();
        doThrow(BusinessRuleException.buildUserException(NUMERIC_PATTERN_MESSAGE))
                .when(pageRequestValidator).validate(dto);

        assertThatThrownBy(() -> facade.execute(dto))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(exception -> assertThat((BusinessRuleException) exception)
                        .extracting(BusinessRuleException::getHttpStatus, BusinessRuleException::getUserMessage)
                        .containsExactly(422, NUMERIC_PATTERN_MESSAGE));
        verifyNoInteractions(simplePageMapper);
    }
}
