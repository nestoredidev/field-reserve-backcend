package nestoredidev.common.infrastructure.web;

import nestoredidev.common.application.PageResult;
import nestoredidev.common.utils.pagination.PageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

public final class ApiResponses {
    private ApiResponses() {
    }

    public static <T> ResponseEntity<PageResponse<T>> ok(T data, String message) {
        return ResponseEntity.ok(PageResponse.single(data, message, HttpStatus.OK.value()));
    }

    public static <T> ResponseEntity<PageResponse<T>> created(T data, String message) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(PageResponse.single(data, message, HttpStatus.CREATED.value()));
    }

    public static <T> ResponseEntity<PageResponse<T>> list(List<T> data, String message) {
        return ResponseEntity.ok(PageResponse.of(data, message, HttpStatus.OK.value()));
    }

    public static <T> ResponseEntity<PageResponse<T>> page(PageResult<T> result, String message) {
        PageResponse<T> body = result.isEmpty()
                ? PageResponse.empty(result, "No results found", HttpStatus.OK.value())
                : PageResponse.fromResult(result, message, HttpStatus.OK.value());
        return ResponseEntity.ok(body);
    }
}
