package nestoredidev.bodega.categoria.domain.exception;

import nestoredidev.common.exception.DomainException;

public class InvalidCategoriaException extends DomainException {
    public InvalidCategoriaException(String message) {
        super(message);
    }
}
