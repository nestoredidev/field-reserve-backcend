package nestoredidev.common.exception;

import java.util.List;

public record ApiError(int code, String message, List<String> errors) {
}
