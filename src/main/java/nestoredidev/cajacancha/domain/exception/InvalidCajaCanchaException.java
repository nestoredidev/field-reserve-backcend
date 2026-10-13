package nestoredidev.cajacancha.domain.exception;

import nestoredidev.common.exception.DomainException;

public class InvalidCajaCanchaException extends DomainException {
    public InvalidCajaCanchaException(String message) {
        super(message);
    }
}
