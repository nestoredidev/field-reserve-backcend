package nestoredidev.cancha.domain.exception;

import nestoredidev.common.exception.ResourceNotFoundException;

public class TarifaNotFoundException extends ResourceNotFoundException {
    public TarifaNotFoundException(Long id) {
        super("Tarifa con id " + id + " no existe");
    }
}
