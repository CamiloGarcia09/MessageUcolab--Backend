package co.edu.uco.application.secondaryports.repository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static co.edu.uco.crosscutting.helpers.UtilPagination.toZeroBasedPage;

public final class PageBuilder {
    private PageBuilder() {}
    public static PageRequest createPageRequest(SimplePageRequest pageRequest) {
        int zeroBasedPage = toZeroBasedPage(pageRequest.getPage());
        Sort sort = Sort.by(
                Sort.Direction.fromString(pageRequest.getSort()),
                pageRequest.getColumnSort()
        );
        return PageRequest.of(zeroBasedPage, pageRequest.getSize(), sort);
    }
}
