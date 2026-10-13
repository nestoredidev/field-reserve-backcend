package nestoredidev.bodega.categoria.domain.exception;

import nestoredidev.common.exception.ResourceNotFoundException;

public class CategoriaNotFoundException extends ResourceNotFoundException {
    public CategoriaNotFoundException(Long id) {
        super("Categoría con id " + id + " no existe");
    }
}
