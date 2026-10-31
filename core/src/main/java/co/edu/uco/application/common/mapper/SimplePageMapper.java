package co.edu.uco.application.common.mapper;

import co.edu.uco.application.primaryports.dto.page.PageRequestDTO;
import co.edu.uco.application.secondaryports.repository.MessageFilter;
import co.edu.uco.application.secondaryports.repository.SimplePageRequest;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

import static co.edu.uco.application.CrosswordsConstant.*;
import static co.edu.uco.crosscutting.helpers.UtilObject.isNullObject;
import static co.edu.uco.crosscutting.helpers.UtilText.getDefault;

@Component
public final class SimplePageMapper {
    public SimplePageRequest toSimplePageRequest(PageRequestDTO dto) {
        if (isNullObject(dto)) {
            return new SimplePageRequest();
        }
        var request = new SimplePageRequest();
        request.setPage(convertToInt(dto.getPage(), REQUEST_PAGE_DEFAULT));
        request.setSize(convertToInt(dto.getSize(), REQUEST_SIZE_DEFAULT));
        request.setSort(getDefault(dto.getSort(), REQUEST_PAGE_SORT_ASC));
        request.setColumnSort(getDefault(dto.getColumnSort(), REQUEST_COLUMN_SORT_DEFAULT));
        request.setFilter(MessageFilter.builder()
                .code(dto.getFilterCode())
                .title(dto.getFilterTitle())
                .category(dto.getFilterCategory())
                .functionality(dto.getFilterFunctionality())
                .type(dto.getFilterType())
                .status(dto.getFilterStatus())
                .statusId(dto.getFilterStatusId())
                .functionalityId(dto.getFilterFunctionalityId())
                .environmentIds(environmentIds(dto.getFilterEnvironments()))
                .build());
        return request;
    }
    private static List<String> environmentIds(String value) {
        return Arrays.stream(getDefault(value).split(","))
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .toList();
    }

    public PageRequestDTO toPageRequestDTO(SimplePageRequest request) {
        if (isNullObject(request)) {
            return new PageRequestDTO();
        }
        return PageRequestDTO.builder()
                .page(String.valueOf(request.getPage()))
                .size(String.valueOf(request.getSize()))
                .sort(request.getSort())
                .columnSort(request.getColumnSort())
                .build();
    }
    private int convertToInt(String value, int defaultValue) {
        if (isNullObject(value)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
