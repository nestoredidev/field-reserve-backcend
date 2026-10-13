package nestoredidev.bodega.caja.domain.exception;

import nestoredidev.common.exception.DomainException;

public class InvalidCajaBodegaException extends DomainException {
    public InvalidCajaBodegaException(String message) {
        super(message);
    }
}
