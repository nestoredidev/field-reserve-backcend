package nestoredidev.cliente.domain.exception;

import nestoredidev.common.exception.ResourceNotFoundException;

public class ClienteNotFoundException extends ResourceNotFoundException {
    public ClienteNotFoundException(String message) {
        super(message);
    }

    public static ClienteNotFoundException porId(Long id) {
        return new ClienteNotFoundException("Cliente con id " + id + " no existe");
    }
}
