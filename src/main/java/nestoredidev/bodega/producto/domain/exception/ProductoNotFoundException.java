package nestoredidev.bodega.producto.domain.exception;

import nestoredidev.common.exception.ResourceNotFoundException;

public class ProductoNotFoundException extends ResourceNotFoundException {
    public ProductoNotFoundException(String message) {
        super(message);
    }

    public static ProductoNotFoundException porId(Long id) {
        return new ProductoNotFoundException("Producto con id " + id + " no existe");
    }
}
