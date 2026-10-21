package co.edu.uco.application.common.mapper;

import co.edu.uco.application.primaryports.dto.page.PageRequestDTO;
import co.edu.uco.application.secondaryports.repository.SimplePageRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SimplePageMapperTest {

    private final SimplePageMapper mapper = new SimplePageMapper();

    @Test
    void toSimplePageRequest_mapsAllFields() {
        PageRequestDTO dto = PageRequestDTO.builder()
                .page("3")
                .size("25")
                .sort("DESC")
                .columnSort("code")
                .build();

        SimplePageRequest result = mapper.toSimplePageRequest(dto);

        assertThat(result.getPage()).isEqualTo(3);
        assertThat(result.getSize()).isEqualTo(25);
        assertThat(result.getSort()).isEqualTo("DESC");
        assertThat(result.getColumnSort()).isEqualTo("code");
    }

    @Test
    void toSimplePageRequest_usesDefaultsWhenDtoIsNull() {
        SimplePageRequest result = mapper.toSimplePageRequest(null);

        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(50);
        assertThat(result.getSort()).isEqualTo("ASC");
        assertThat(result.getColumnSort()).isEqualTo("id");
    }

    @Test
    void toSimplePageRequest_usesDefaultsWhenValuesInvalid() {
        PageRequestDTO dto = PageRequestDTO.builder()
                .page("abc")
                .size("xyz")
                .sort("")
                .columnSort("")
                .build();

        SimplePageRequest result = mapper.toSimplePageRequest(dto);

        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(50);
        assertThat(result.getSort()).isEmpty();
        assertThat(result.getColumnSort()).isEmpty();
    }

    @Test
    void toSimplePageRequest_usesDefaultsWhenValuesNull() {
        PageRequestDTO dto = PageRequestDTO.builder().build();

        SimplePageRequest result = mapper.toSimplePageRequest(dto);

        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(50);
        assertThat(result.getSort()).isEqualTo("ASC");
        assertThat(result.getColumnSort()).isEqualTo("id");
    }

    @Test
    void toPageRequestDTO_mapsAllFields() {
        SimplePageRequest request = new SimplePageRequest();
        request.setPage(4);
        request.setSize(30);
        request.setSort("DESC");
        request.setColumnSort("title");

        PageRequestDTO result = mapper.toPageRequestDTO(request);

        assertThat(result.getPage()).isEqualTo("4");
        assertThat(result.getSize()).isEqualTo("30");
        assertThat(result.getSort()).isEqualTo("DESC");
        assertThat(result.getColumnSort()).isEqualTo("title");
    }

    @Test
    void toPageRequestDTO_returnsNewDtoWhenRequestIsNull() {
        PageRequestDTO result = mapper.toPageRequestDTO(null);

        assertThat(result).isNotNull();
    }

    @org.junit.jupiter.api.Test
    void toSimplePageRequest_splitsEnvironmentFilterIgnoringBlanks() {
        var dto = co.edu.uco.application.primaryports.dto.page.PageRequestDTO.builder()
                .filterEnvironments(" env-1, ,env-2 ,").build();

        var request = new SimplePageMapper().toSimplePageRequest(dto);

        org.assertj.core.api.Assertions.assertThat(request.getFilter().getEnvironmentIds())
                .containsExactly("env-1", "env-2");
    }

    @org.junit.jupiter.api.Test
    void toSimplePageRequest_usesEmptyEnvironmentFilter_whenParameterIsMissing() {
        var request = new SimplePageMapper().toSimplePageRequest(
                co.edu.uco.application.primaryports.dto.page.PageRequestDTO.builder().build());

        org.assertj.core.api.Assertions.assertThat(request.getFilter().getEnvironmentIds()).isEmpty();
    }
}
