package nestoredidev.common.infrastructure.persistence;

import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.function.Function;

public final class PageAdapter {
    private PageAdapter() {
    }

    public static Pageable toPageable(PageQuery query) {
        Sort sort = Sort.by(query.sortBy());
        sort = "desc".equalsIgnoreCase(query.direction()) ? sort.descending() : sort.ascending();
        return PageRequest.of(query.page(), query.size(), sort);
    }

    public static <E, T> PageResult<T> toResult(Page<E> page, Function<E, T> mapper) {
        return new PageResult<>(page.getContent().stream().map(mapper).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
