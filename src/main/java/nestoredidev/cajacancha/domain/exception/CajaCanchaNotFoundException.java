package nestoredidev.cajacancha.domain.exception;

import nestoredidev.common.exception.ResourceNotFoundException;

public class CajaCanchaNotFoundException extends ResourceNotFoundException {
    public CajaCanchaNotFoundException(String message) {
        super(message);
    }
}
