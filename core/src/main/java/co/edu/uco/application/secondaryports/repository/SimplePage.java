package co.edu.uco.application.secondaryports.repository;

import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

import static co.edu.uco.crosscutting.helpers.UtilNumeric.getDefault;
import static co.edu.uco.crosscutting.helpers.UtilObject.getDefaultIsNullObject;
import static co.edu.uco.crosscutting.helpers.UtilPagination.toOneBasedPage;

@Getter
public final class SimplePage<T> {
    private List<T> data;
    private int page;
    private int size;
    private long totalItems;
    private int totalPages;
    public SimplePage(List<T> data, int page, int size, long totalItems, int totalPages) {
        this.data = data;
        this.page = page;
        this.size = size;
        this.totalItems = totalItems;
        this.totalPages = totalPages;
    }
    public static <T> SimplePage<T> of(Page<T> map) {
        int oneBasedPage = toOneBasedPage(map.getNumber());
        return new SimplePage<>(map.getContent(), oneBasedPage, map.getSize(), map.getTotalElements(), map.getTotalPages());
    }
    public static <T> SimplePage<T> of(List<T> data, int page, int size, long totalItems, int totalPage) {
        return new SimplePage<>(data, page, size, totalItems, totalPage);
    }
    public void setData(List<T> data) {
        this.data = getDefaultIsNullObject(data, List.of());
    }
    public void setPage(int page) {
        this.page = (int) getDefault(page);
    }
    public void setSize(int size) {
        this.size = (int) getDefault(size);
    }
    public void setTotalItems(long totalItems) {
        this.totalItems = (long) getDefault(totalItems);
    }
    public void setTotalPages(int totalPages) {
        this.totalPages = (int) getDefault(totalPages);
    }
}