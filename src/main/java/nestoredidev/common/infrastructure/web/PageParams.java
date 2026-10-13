package nestoredidev.common.infrastructure.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.exception.InvalidRequestException;

import java.util.Arrays;

/** Parámetros de paginación comunes: page, size, sortBy y direction. */
public record PageParams(
        @Min(value = 0, message = "page must be >= 0") Integer page,
        @Min(value = 1, message = "size must be >= 1") @Max(value = 100, message = "size must be <= 100") Integer size,
        String sortBy,
        @Pattern(regexp = "(?i)asc|desc", message = "direction must be asc or desc") String direction
) {
    public PageParams {
        page = page == null ? 0 : page;
        size = size == null ? 10 : size;
        direction = direction == null ? "asc" : direction;
    }

    public PageQuery toQuery(String defaultSort, String... allowedSorts) {
        String sort = sortBy == null || sortBy.isBlank() ? defaultSort : sortBy;
        if (Arrays.stream(allowedSorts).noneMatch(sort::equals)) {
            throw new InvalidRequestException("sortBy must be one of: " + String.join(", ", allowedSorts));
        }
        return new PageQuery(page, size, sort, direction);
    }
}
