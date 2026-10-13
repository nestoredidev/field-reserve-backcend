package nestoredidev.cancha.domain.exception;

import nestoredidev.common.exception.ResourceNotFoundException;

public class CanchaNotFoundException extends ResourceNotFoundException {
    public CanchaNotFoundException(Long id) {
        super("Cancha con id " + id + " no existe");
    }
}
