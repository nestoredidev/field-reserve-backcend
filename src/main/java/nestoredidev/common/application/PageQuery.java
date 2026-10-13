package nestoredidev.common.application;

public record PageQuery(int page, int size, String sortBy, String direction) {
}
