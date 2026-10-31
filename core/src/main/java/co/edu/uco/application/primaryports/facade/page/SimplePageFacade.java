package co.edu.uco.application.primaryports.facade.page;

import co.edu.uco.application.primaryports.dto.page.PageRequestDTO;
import co.edu.uco.application.secondaryports.repository.SimplePageRequest;

public interface SimplePageFacade {
    SimplePageRequest execute(PageRequestDTO pageRequestDTO);
}