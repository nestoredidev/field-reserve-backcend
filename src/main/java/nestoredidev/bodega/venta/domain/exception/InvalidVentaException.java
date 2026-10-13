package nestoredidev.bodega.venta.domain.exception;

import nestoredidev.common.exception.DomainException;

public class InvalidVentaException extends DomainException {
    public InvalidVentaException(String message) {
        super(message);
    }
}
