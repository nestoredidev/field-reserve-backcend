package nestoredidev.bodega.caja.domain.exception;

import nestoredidev.common.exception.ResourceNotFoundException;

public class CajaBodegaNotFoundException extends ResourceNotFoundException {
    public CajaBodegaNotFoundException(String message) {
        super(message);
    }
}
