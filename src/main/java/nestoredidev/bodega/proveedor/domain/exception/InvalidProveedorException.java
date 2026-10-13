package nestoredidev.bodega.proveedor.domain.exception;

import nestoredidev.common.exception.DomainException;

public class InvalidProveedorException extends DomainException {
    public InvalidProveedorException(String message) {
        super(message);
    }
}
