package nestoredidev.reserva.domain.exception;

import nestoredidev.common.exception.ResourceNotFoundException;

public class ReservaNotFoundException extends ResourceNotFoundException {
    public ReservaNotFoundException(Long id) {
        super("Reserva con id " + id + " no existe");
    }
}
