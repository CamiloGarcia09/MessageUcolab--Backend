package co.edu.uco.application.secondaryports.repository;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class PageBuilderTest {

    @Test
    void createPageRequest_buildsZeroBasedPageWithSort() {
        SimplePageRequest request = new SimplePageRequest();
        request.setPage(1);
        request.setSize(20);
        request.setSort("DESC");
        request.setColumnSort("code");

        PageRequest result = PageBuilder.createPageRequest(request);

        assertAll(
                () -> assertThat(result.getPageNumber()).isZero(),
                () -> assertThat(result.getPageSize()).isEqualTo(20),
                () -> assertThat(result.getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "code")));
    }

    @Test
    void createPageRequest_convertsOneBasedPageToZeroBased() {
        SimplePageRequest request = new SimplePageRequest();
        request.setPage(3);
        request.setSize(10);
        request.setSort("ASC");
        request.setColumnSort("id");

        PageRequest result = PageBuilder.createPageRequest(request);

        assertAll(
                () -> assertThat(result.getPageNumber()).isEqualTo(2),
                () -> assertThat(result.getPageSize()).isEqualTo(10),
                () -> assertThat(result.getSort().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.ASC));
    }

    @Test
    void createPageRequest_usesAscendingByDefault() {
        SimplePageRequest request = new SimplePageRequest();

        PageRequest result = PageBuilder.createPageRequest(request);

        assertThat(result.getSort().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.ASC);
    }
}
