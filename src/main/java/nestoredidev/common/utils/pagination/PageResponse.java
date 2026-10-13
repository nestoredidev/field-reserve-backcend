package nestoredidev.common.utils.pagination;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import nestoredidev.common.application.PageResult;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResponse<T> {
    @JsonProperty("code")
    private int code;

    @JsonProperty("message")
    private String message;

    @JsonProperty("data")
    private List<T> data;

    @JsonProperty("page")
    private Integer page;

    @JsonProperty("size")
    private Integer size;

    @JsonProperty("total_elements")
    private Long totalElements;

    @JsonProperty("total_pages")
    private Integer totalPages;

    @JsonProperty("has_next")
    private Boolean hasNext;

    @JsonProperty("has_previous")
    private Boolean hasPrevious;

    public static <T> PageResponse<T> fromPage(Page<T> page) {
        return PageResponse.<T>builder()
                .data(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }

    public static <T> PageResponse<T> fromPage(Page<T> page, String message, int code) {
        return PageResponse.<T>builder()
                .data(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .message(message)
                .code(code)
                .build();
    }

    public static <T> PageResponse<T> of(List<T> list, String message, int code) {
        return PageResponse.<T>builder()
                .data(list)
                .message(message)
                .code(code)
                .build();
    }

    public static <T> PageResponse<T> single(T list, String message, int code) {
        return PageResponse.<T>builder()
                .data(List.of(list))
                .message(message)
                .code(code)
                .build();
    }

    public static <T> PageResponse<T> fromResult(PageResult<T> result, String message, int code) {
        return PageResponse.<T>builder()
                .data(result.content())
                .page(result.page())
                .size(result.size())
                .totalElements(result.totalElements())
                .totalPages(result.totalPages())
                .hasNext(result.hasNext())
                .hasPrevious(result.hasPrevious())
                .message(message)
                .code(code)
                .build();
    }

    public static <T> PageResponse<T> empty(PageResult<T> pag, String message, int code) {
        return PageResponse.<T>builder()
                .data(List.of())
                .page(pag.page())
                .size(pag.size())
                .totalElements(pag.totalElements())
                .totalPages(pag.totalPages())
                .hasNext(pag.hasNext())
                .hasPrevious(pag.hasPrevious())
                .message(message)
                .code(code)
                .build();
    }
}
