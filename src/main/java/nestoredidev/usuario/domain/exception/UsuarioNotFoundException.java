package nestoredidev.usuario.domain.exception;

import nestoredidev.common.exception.ResourceNotFoundException;

public class UsuarioNotFoundException extends ResourceNotFoundException {
    public UsuarioNotFoundException(Long id) {
        super("Usuario con id " + id + " no existe");
    }
}
