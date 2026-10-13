package nestoredidev.bodega.compra.domain.exception;

import nestoredidev.common.exception.ResourceNotFoundException;

public class CompraNotFoundException extends ResourceNotFoundException {
    public CompraNotFoundException(Long id) {
        super("Compra con id " + id + " no existe");
    }
}
