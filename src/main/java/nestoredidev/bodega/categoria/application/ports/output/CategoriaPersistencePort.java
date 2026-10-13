package nestoredidev.bodega.categoria.application.ports.output;

import nestoredidev.bodega.categoria.domain.model.Categoria;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;

import java.util.Optional;

public interface CategoriaPersistencePort {

    Categoria guardar(Categoria categoria);

    Optional<Categoria> buscarPorId(Long id);

    boolean existePorNombre(String nombre, Long excluirId);

    PageResult<Categoria> listar(PageQuery query);

    boolean tieneProductos(Long id);

    void eliminar(Long id);
}
