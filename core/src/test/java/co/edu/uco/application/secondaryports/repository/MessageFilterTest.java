package co.edu.uco.application.secondaryports.repository;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MessageFilterTest {

    @Test
    void hasFilters_returnsFalse_whenAllFieldsNull() {
        MessageFilter filter = new MessageFilter();

        assertThat(filter.hasFilters()).isFalse();
    }

    @Test
    void hasFilters_returnsFalse_whenAllFieldsBlank() {
        MessageFilter filter = MessageFilter.builder()
                .code("").title("  ").category("").functionality("").type("").status("").build();

        assertThat(filter.hasFilters()).isFalse();
    }

    @Test
    void hasFilters_returnsTrue_whenCodeIsSet() {
        MessageFilter filter = MessageFilter.builder().code("MSG-001").build();

        assertThat(filter.hasFilters()).isTrue();
    }

    @Test
    void hasFilters_returnsTrue_whenTitleIsSet() {
        MessageFilter filter = MessageFilter.builder().title("Welcome").build();

        assertThat(filter.hasFilters()).isTrue();
    }

    @Test
    void hasFilters_returnsTrue_whenCategoryIsSet() {
        MessageFilter filter = MessageFilter.builder().category("INFO").build();

        assertThat(filter.hasFilters()).isTrue();
    }

    @Test
    void hasFilters_returnsTrue_whenFunctionalityIsSet() {
        MessageFilter filter = MessageFilter.builder().functionality("LOGIN").build();

        assertThat(filter.hasFilters()).isTrue();
    }

    @Test
    void hasFilters_returnsTrue_whenTypeIsSet() {
        MessageFilter filter = MessageFilter.builder().type("ALERT").build();

        assertThat(filter.hasFilters()).isTrue();
    }

    @Test
    void hasFilters_returnsTrue_whenStatusIsSet() {
        MessageFilter filter = MessageFilter.builder().status("ACTIVE").build();

        assertThat(filter.hasFilters()).isTrue();
    }

    @Test
    void hasFilters_returnsTrue_whenMultipleFieldsSet() {
        MessageFilter filter = MessageFilter.builder().code("X").title("Y").status("ACTIVE").build();

        assertThat(filter.hasFilters()).isTrue();
    }

    @Test
    void builder_setsAllFields() {
        MessageFilter filter = MessageFilter.builder()
                .code("C").title("T").category("CAT").functionality("FUNC").type("TYPE").status("ST").build();

        assertThat(filter.getCode()).isEqualTo("C");
        assertThat(filter.getTitle()).isEqualTo("T");
        assertThat(filter.getCategory()).isEqualTo("CAT");
        assertThat(filter.getFunctionality()).isEqualTo("FUNC");
        assertThat(filter.getType()).isEqualTo("TYPE");
        assertThat(filter.getStatus()).isEqualTo("ST");
    }

    @Test
    void equals_returnsTrueForIdenticalFilters() {
        MessageFilter a = MessageFilter.builder().code("X").build();
        MessageFilter b = MessageFilter.builder().code("X").build();

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void equals_returnsFalseForDifferentFilters() {
        MessageFilter a = MessageFilter.builder().code("X").build();
        MessageFilter b = MessageFilter.builder().code("Y").build();

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equals_returnsFalseForNull() {
        MessageFilter a = new MessageFilter();

        assertThat(a).isNotEqualTo(null);
    }

    @Test
    void equals_returnsFalseForDifferentType() {
        MessageFilter a = new MessageFilter();

        assertThat(a).isNotEqualTo("string");
    }

    @Test
    void equals_returnsTrue_forSameInstance() {
        MessageFilter a = MessageFilter.builder().code("X").build();

        assertThat(a).isEqualTo(a);
    }

    @Test
    void equals_returnsFalseWhenTitleDiffers() {
        MessageFilter a = MessageFilter.builder().title("T1").build();
        MessageFilter b = new MessageFilter();

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equals_returnsFalseWhenCategoryDiffers() {
        MessageFilter a = MessageFilter.builder().category("CAT").build();
        MessageFilter b = new MessageFilter();

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equals_returnsFalseWhenFunctionalityDiffers() {
        MessageFilter a = MessageFilter.builder().functionality("FUNC").build();
        MessageFilter b = new MessageFilter();

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equals_returnsFalseWhenTypeDiffers() {
        MessageFilter a = MessageFilter.builder().type("TYPE").build();
        MessageFilter b = new MessageFilter();

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equals_returnsFalseWhenStatusDiffers() {
        MessageFilter a = MessageFilter.builder().status("ACTIVE").build();
        MessageFilter b = new MessageFilter();

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void hashCode_isConsistent() {
        MessageFilter a = MessageFilter.builder().code("X").title("T").build();

        assertThat(a.hashCode()).isEqualTo(a.hashCode());
    }

    @Test
    void toString_containsFieldValues() {
        MessageFilter filter = MessageFilter.builder().code("C").title("T").build();

        assertThat(filter.toString()).contains("C", "T");
    }
}
