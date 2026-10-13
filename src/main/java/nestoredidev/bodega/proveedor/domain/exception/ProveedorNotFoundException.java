package nestoredidev.bodega.proveedor.domain.exception;

import nestoredidev.common.exception.ResourceNotFoundException;

public class ProveedorNotFoundException extends ResourceNotFoundException {
    public ProveedorNotFoundException(Long id) {
        super("Proveedor con id " + id + " no existe");
    }
}
