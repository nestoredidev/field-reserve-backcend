package nestoredidev.bodega.compra.domain.exception;

import nestoredidev.common.exception.DomainException;

public class InvalidCompraException extends DomainException {
    public InvalidCompraException(String message) {
        super(message);
    }
}
