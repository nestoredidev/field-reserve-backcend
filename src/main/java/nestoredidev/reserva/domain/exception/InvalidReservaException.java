package nestoredidev.reserva.domain.exception;

import nestoredidev.common.exception.DomainException;

public class InvalidReservaException extends DomainException {
    public InvalidReservaException(String message) {
        super(message);
    }
}
