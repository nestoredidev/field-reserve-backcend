package nestoredidev.bodega.producto.domain.exception;

import nestoredidev.common.exception.DomainException;

public class InvalidProductoException extends DomainException {
    public InvalidProductoException(String message) {
        super(message);
    }
}
