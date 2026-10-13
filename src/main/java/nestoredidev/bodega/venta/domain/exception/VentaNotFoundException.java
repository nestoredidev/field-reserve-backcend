package nestoredidev.bodega.venta.domain.exception;

import nestoredidev.common.exception.ResourceNotFoundException;

public class VentaNotFoundException extends ResourceNotFoundException {
    public VentaNotFoundException(Long id) {
        super("Venta con id " + id + " no existe");
    }
}
