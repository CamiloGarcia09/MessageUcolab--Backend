package co.edu.uco.application.primaryports.dto.page;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class PageRequestDTOTest {

    private PageRequestDTO buildPageRequest() {
        return PageRequestDTO.builder()
                .page("1")
                .size("50")
                .columnSort("id")
                .sort("ASC")
                .build();
    }

    @Test
    void builder_createsDtoWithValues() {
        PageRequestDTO dto = buildPageRequest();

        assertThat(dto.getPage()).isEqualTo("1");
        assertThat(dto.getSize()).isEqualTo("50");
        assertThat(dto.getColumnSort()).isEqualTo("id");
        assertThat(dto.getSort()).isEqualTo("ASC");
    }

    @Test
    void setters_updateValues() {
        PageRequestDTO dto = new PageRequestDTO();
        dto.setPage("2");
        dto.setSize("10");
        dto.setColumnSort("code");
        dto.setSort(null);

        assertThat(dto.getPage()).isEqualTo("2");
        assertThat(dto.getSize()).isEqualTo("10");
        assertThat(dto.getColumnSort()).isEqualTo("code");
        assertThat(dto.getSort()).isNull();
    }

    @Test
    void equals_returnsTrueForSameValuesAndSameHashCode() {
        PageRequestDTO a = buildPageRequest();
        PageRequestDTO b = buildPageRequest();

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void equals_returnsTrueWhenBothFieldsAreNull() {
        PageRequestDTO a = new PageRequestDTO();
        PageRequestDTO b = new PageRequestDTO();

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void equals_returnsFalseForNullAndDifferentType() {
        PageRequestDTO a = buildPageRequest();

        assertThat(a).isNotEqualTo(null);
        assertThat(a).isNotEqualTo("1");
    }

    @Test
    void equals_returnsFalseWhenFieldDiffers() {
        PageRequestDTO a = buildPageRequest();
        PageRequestDTO b = buildPageRequest();
        b.setSize("100");

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equals_returnsFalseWhenOneFieldIsNullAndOtherIsNot() {
        PageRequestDTO a = buildPageRequest();
        PageRequestDTO b = buildPageRequest();
        b.setSort(null);

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equals_returnsTrue_whenComparingTheSameInstance() {
        PageRequestDTO dto = buildPageRequest();

        assertThat(dto).isEqualTo(dto);
    }

    @Test
    void equals_returnsFalse_whenTheReceiverFieldIsNullAndTheOtherIsNot() {
        PageRequestDTO empty = new PageRequestDTO();
        PageRequestDTO withPage = new PageRequestDTO();
        withPage.setPage("1");
        PageRequestDTO withSize = new PageRequestDTO();
        withSize.setSize("50");
        PageRequestDTO withColumnSort = new PageRequestDTO();
        withColumnSort.setColumnSort("code");
        PageRequestDTO withSort = new PageRequestDTO();
        withSort.setSort("DESC");

        assertAll(
                () -> assertThat(empty).isNotEqualTo(withPage),
                () -> assertThat(empty).isNotEqualTo(withSize),
                () -> assertThat(empty).isNotEqualTo(withColumnSort),
                () -> assertThat(empty).isNotEqualTo(withSort));
    }

    @Test
    void toString_containsFieldValues() {
        String value = buildPageRequest().toString();

        assertThat(value).contains("1", "50", "id", "ASC");
    }

    @Test
    void builder_setsFilterFields() {
        PageRequestDTO dto = PageRequestDTO.builder()
                .filterCode("MSG-001")
                .filterTitle("Welcome")
                .filterCategory("INFO")
                .filterFunctionality("LOGIN")
                .filterType("ALERT")
                .filterStatus("ACTIVE")
                .build();

        assertThat(dto.getFilterCode()).isEqualTo("MSG-001");
        assertThat(dto.getFilterTitle()).isEqualTo("Welcome");
        assertThat(dto.getFilterCategory()).isEqualTo("INFO");
        assertThat(dto.getFilterFunctionality()).isEqualTo("LOGIN");
        assertThat(dto.getFilterType()).isEqualTo("ALERT");
        assertThat(dto.getFilterStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void setters_updateFilterFields() {
        PageRequestDTO dto = new PageRequestDTO();
        dto.setFilterCode("C");
        dto.setFilterTitle("T");
        dto.setFilterCategory("CAT");
        dto.setFilterFunctionality("FUNC");
        dto.setFilterType("TYPE");
        dto.setFilterStatus("STATUS");

        assertThat(dto.getFilterCode()).isEqualTo("C");
        assertThat(dto.getFilterTitle()).isEqualTo("T");
        assertThat(dto.getFilterCategory()).isEqualTo("CAT");
        assertThat(dto.getFilterFunctionality()).isEqualTo("FUNC");
        assertThat(dto.getFilterType()).isEqualTo("TYPE");
        assertThat(dto.getFilterStatus()).isEqualTo("STATUS");
    }

    @Test
    void equals_returnsTrueForSameFilterFields() {
        PageRequestDTO a = PageRequestDTO.builder()
                .filterCode("X").filterTitle("Y").filterCategory("C")
                .filterFunctionality("F").filterType("T").filterStatus("S").build();
        PageRequestDTO b = PageRequestDTO.builder()
                .filterCode("X").filterTitle("Y").filterCategory("C")
                .filterFunctionality("F").filterType("T").filterStatus("S").build();

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void equals_returnsFalseWhenFilterCodeDiffers() {
        PageRequestDTO a = new PageRequestDTO();
        a.setFilterCode("A");
        PageRequestDTO b = new PageRequestDTO();
        b.setFilterCode("B");

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equals_returnsFalseWhenFilterTitleDiffers() {
        PageRequestDTO a = new PageRequestDTO();
        a.setFilterTitle("A");
        PageRequestDTO b = new PageRequestDTO();

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equals_returnsFalseWhenFilterCategoryDiffers() {
        PageRequestDTO a = new PageRequestDTO();
        a.setFilterCategory("CAT");
        PageRequestDTO b = new PageRequestDTO();

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equals_returnsFalseWhenFilterFunctionalityDiffers() {
        PageRequestDTO a = new PageRequestDTO();
        a.setFilterFunctionality("FUNC");
        PageRequestDTO b = new PageRequestDTO();

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equals_returnsFalseWhenFilterTypeDiffers() {
        PageRequestDTO a = new PageRequestDTO();
        a.setFilterType("TYPE");
        PageRequestDTO b = new PageRequestDTO();

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equals_returnsFalseWhenFilterStatusDiffers() {
        PageRequestDTO a = new PageRequestDTO();
        a.setFilterStatus("ACTIVE");
        PageRequestDTO b = new PageRequestDTO();

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void toString_containsFilterFieldValues() {
        PageRequestDTO dto = PageRequestDTO.builder()
                .filterCode("CODE").filterTitle("TITLE").build();

        assertThat(dto.toString()).contains("CODE", "TITLE");
    }
}