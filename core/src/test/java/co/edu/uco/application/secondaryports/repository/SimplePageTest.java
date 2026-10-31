package co.edu.uco.application.secondaryports.repository;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SimplePageTest {

    @Test
    void of_Page_convertsToOneBasedPage() {
        Page<String> springPage = new PageImpl<>(List.of("a", "b"), PageRequest.of(2, 10), 30);

        SimplePage<String> result = SimplePage.of(springPage);

        assertThat(result.getData()).containsExactly("a", "b");
        assertThat(result.getPage()).isEqualTo(3);
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.getTotalItems()).isEqualTo(30);
        assertThat(result.getTotalPages()).isEqualTo(3);
    }

    @Test
    void of_Page_handlesZeroBasedFirstPage() {
        Page<String> springPage = new PageImpl<>(List.of("a"), PageRequest.of(0, 5), 1);

        SimplePage<String> result = SimplePage.of(springPage);

        assertThat(result.getPage()).isEqualTo(1);
    }

    @Test
    void of_list_createsPageFromValues() {
        SimplePage<String> result = SimplePage.of(List.of("x"), 4, 10, 31, 4);

        assertThat(result.getData()).containsExactly("x");
        assertThat(result.getPage()).isEqualTo(4);
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.getTotalItems()).isEqualTo(31);
        assertThat(result.getTotalPages()).isEqualTo(4);
    }

    @Test
    void setData_normalizesNullToEmptyList() {
        SimplePage<String> page = new SimplePage<>(null, 1, 10, 0, 0);

        page.setData(null);

        assertThat(page.getData()).isEmpty();
    }

    @Test
    void constructor_keepsProvidedValues() {
        SimplePage<String> page = new SimplePage<>(List.of("a"), 2, 15, 8, 1);

        assertThat(page.getPage()).isEqualTo(2);
        assertThat(page.getSize()).isEqualTo(15);
        assertThat(page.getTotalItems()).isEqualTo(8);
        assertThat(page.getTotalPages()).isEqualTo(1);
    }

    @Test
    void setters_keepValidValues() {
        SimplePage<String> page = new SimplePage<>(null, 0, 0, 0, 0);
        page.setPage(5);
        page.setSize(25);
        page.setTotalItems(100);
        page.setTotalPages(4);

        assertThat(page.getPage()).isEqualTo(5);
        assertThat(page.getSize()).isEqualTo(25);
        assertThat(page.getTotalItems()).isEqualTo(100);
        assertThat(page.getTotalPages()).isEqualTo(4);
    }
}