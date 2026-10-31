package co.edu.uco.application.primaryports.facade.page.impl;

import co.edu.uco.application.primaryports.dto.page.PageRequestDTO;
import co.edu.uco.application.primaryports.facade.page.SimplePageFacade;
import co.edu.uco.application.common.mapper.SimplePageMapper;
import co.edu.uco.application.secondaryports.repository.SimplePageRequest;
import co.edu.uco.application.usecase.validator.page.PageRequestDTOCompositeValidator;
import org.springframework.stereotype.Component;

@Component
public final class SimplePageFacadeImpl implements SimplePageFacade {
    private final SimplePageMapper simplePageMapper;
    private final PageRequestDTOCompositeValidator pageRequestValidator;
    SimplePageFacadeImpl(SimplePageMapper simplePageMapper, PageRequestDTOCompositeValidator pageRequestValidator) {
        this.simplePageMapper = simplePageMapper;
        this.pageRequestValidator = pageRequestValidator;
    }
    @Override
    public SimplePageRequest execute(PageRequestDTO pageDTO) {
        pageRequestValidator.validate(pageDTO);
        return simplePageMapper.toSimplePageRequest(pageDTO);
    }
}
