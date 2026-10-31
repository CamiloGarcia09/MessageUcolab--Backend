package co.edu.uco.crosscutting.helpers;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UtilPaginationTest {

    @Test
    void toZeroBasedPage_convertsOneBasedToZeroBased() {
        assertThat(UtilPagination.toZeroBasedPage(1)).isZero();
        assertThat(UtilPagination.toZeroBasedPage(3)).isEqualTo(2);
    }

    @Test
    void toOneBasedPage_convertsZeroBasedToOneBased() {
        assertThat(UtilPagination.toOneBasedPage(0)).isEqualTo(1);
        assertThat(UtilPagination.toOneBasedPage(2)).isEqualTo(3);
    }
}