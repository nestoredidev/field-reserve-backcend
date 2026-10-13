package nestoredidev.bodega.proveedor.application.ports.output;

import nestoredidev.bodega.proveedor.domain.model.Proveedor;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;

import java.util.Optional;

public interface ProveedorPersistencePort {

    Proveedor guardar(Proveedor proveedor);

    Optional<Proveedor> buscarPorId(Long id);

    boolean existePorRuc(String ruc, Long excluirId);

    PageResult<Proveedor> listar(String q, Boolean activo, PageQuery query);
}
