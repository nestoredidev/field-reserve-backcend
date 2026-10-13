package nestoredidev.cliente.domain.exception;

import nestoredidev.common.exception.DomainException;

public class InvalidClienteException extends DomainException {
    public InvalidClienteException(String message) {
        super(message);
    }
}
