package nestoredidev.usuario.domain.exception;

import nestoredidev.common.exception.DomainException;

public class InvalidUsuarioException extends DomainException {
    public InvalidUsuarioException(String message) {
        super(message);
    }
}
