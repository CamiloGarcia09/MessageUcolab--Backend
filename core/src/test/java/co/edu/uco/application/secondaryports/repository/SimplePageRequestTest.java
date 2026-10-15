package co.edu.uco.application.secondaryports.repository;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class SimplePageRequestTest {

    @Test
    void defaultConstructor_initializesWithDefaults() {
        SimplePageRequest request = new SimplePageRequest();

        assertThat(request.getPage()).isEqualTo(1);
        assertThat(request.getSort()).isEqualTo("ASC");
        assertThat(request.getColumnSort()).isEqualTo("id");
        assertThat(request.getSize()).isEqualTo(50);
    }

    @Test
    void setters_updateValues() {
        SimplePageRequest request = new SimplePageRequest();
        request.setPage(2);
        request.setSort("DESC");
        request.setColumnSort("code");
        request.setSize(10);

        assertThat(request.getPage()).isEqualTo(2);
        assertThat(request.getSort()).isEqualTo("DESC");
        assertThat(request.getColumnSort()).isEqualTo("code");
        assertThat(request.getSize()).isEqualTo(10);
    }

    @Test
    void equals_returnsTrueForSameValuesAndSameHashCode() {
        SimplePageRequest a = new SimplePageRequest();
        SimplePageRequest b = new SimplePageRequest();
        b.setSort("DESC");
        b.setColumnSort("code");
        b.setSize(10);
        b.setPage(2);

        SimplePageRequest copy = new SimplePageRequest();
        copy.setSort("DESC");
        copy.setColumnSort("code");
        copy.setSize(10);
        copy.setPage(2);

        assertThat(b).isEqualTo(copy);
        assertThat(b.hashCode()).isEqualTo(copy.hashCode());
    }

    @Test
    void equals_returnsTrueWhenSortFieldsAreNullInBoth() {
        SimplePageRequest a = new SimplePageRequest();
        a.setSort(null);
        a.setColumnSort(null);
        SimplePageRequest b = new SimplePageRequest();
        b.setSort(null);
        b.setColumnSort(null);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void equals_returnsFalseForNullAndDifferentType() {
        SimplePageRequest a = new SimplePageRequest();

        assertThat(a).isNotEqualTo(null);
        assertThat(a).isNotEqualTo("1");
    }

    @Test
    void equals_returnsFalseWhenFieldDiffers() {
        SimplePageRequest a = new SimplePageRequest();
        SimplePageRequest b = new SimplePageRequest();
        b.setPage(2);

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equals_returnsFalseWhenOneSortFieldIsNullAndOtherIsNot() {
        SimplePageRequest a = new SimplePageRequest();
        SimplePageRequest b = new SimplePageRequest();
        b.setSort(null);

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equals_returnsTrue_whenComparedWithTheSameInstance() {
        SimplePageRequest request = new SimplePageRequest();

        assertThat(request).isEqualTo(request);
    }

    @Test
    void equals_returnsFalse_whenTheReceiverSortIsNullAndTheOtherIsNot() {
        SimplePageRequest receiver = new SimplePageRequest();
        receiver.setSort(null);
        SimplePageRequest other = new SimplePageRequest();

        assertThat(receiver).isNotEqualTo(other);
    }

    @Test
    void equals_returnsFalse_whenTheReceiverColumnSortIsNullAndTheOtherIsNot() {
        SimplePageRequest receiver = new SimplePageRequest();
        receiver.setColumnSort(null);
        SimplePageRequest other = new SimplePageRequest();

        assertThat(receiver).isNotEqualTo(other);
    }

    @Test
    void equals_returnsFalse_whenTheSizeDiffers() {
        SimplePageRequest receiver = new SimplePageRequest();
        SimplePageRequest other = new SimplePageRequest();
        other.setSize(10);

        assertThat(receiver).isNotEqualTo(other);
    }

    @Test
    void toString_containsDefaultValues() {
        String value = new SimplePageRequest().toString();

        assertThat(value).contains("ASC", "id", "50");
    }

    @Test
    void filter_defaultIsNotNull() {
        SimplePageRequest request = new SimplePageRequest();

        assertThat(request.getFilter()).isNotNull();
        assertThat(request.getFilter().hasFilters()).isFalse();
    }

    @Test
    void equals_returnsFalseWhenFilterDiffers() {
        SimplePageRequest a = new SimplePageRequest();
        SimplePageRequest b = new SimplePageRequest();
        b.setFilter(MessageFilter.builder().code("X").build());

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equals_returnsTrueWhenFilterMatches() {
        SimplePageRequest a = new SimplePageRequest();
        a.setFilter(MessageFilter.builder().code("X").build());
        SimplePageRequest b = new SimplePageRequest();
        b.setFilter(MessageFilter.builder().code("X").build());

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void equals_returnsFalseWhenFilterIsNullInOne() {
        SimplePageRequest a = new SimplePageRequest();
        a.setFilter(null);
        SimplePageRequest b = new SimplePageRequest();

        assertThat(a).isNotEqualTo(b);
    }
}