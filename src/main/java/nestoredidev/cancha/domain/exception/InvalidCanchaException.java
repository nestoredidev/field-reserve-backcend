package nestoredidev.cancha.domain.exception;

import nestoredidev.common.exception.DomainException;

public class InvalidCanchaException extends DomainException {
    public InvalidCanchaException(String message) {
        super(message);
    }
}
