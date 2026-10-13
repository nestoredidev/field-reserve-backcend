package nestoredidev.bodega.compra.application.ports.output;

import nestoredidev.bodega.compra.domain.model.Compra;
import nestoredidev.bodega.compra.domain.model.EstadoCompra;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface CompraPersistencePort {

    /** Inserta la compra con sus detalles, o actualiza el estado de una existente. */
    Compra guardar(Compra compra);

    Optional<Compra> buscarPorId(Long id);

    Optional<Compra> buscarPorIdParaActualizar(Long id);

    PageResult<Compra> listar(Long proveedorId, EstadoCompra estado, OffsetDateTime desde, OffsetDateTime hasta,
                              PageQuery query);
}
