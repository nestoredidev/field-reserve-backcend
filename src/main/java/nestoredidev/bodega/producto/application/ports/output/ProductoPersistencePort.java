package nestoredidev.bodega.producto.application.ports.output;

import nestoredidev.bodega.producto.domain.model.MovimientoInventario;
import nestoredidev.bodega.producto.domain.model.Producto;
import nestoredidev.bodega.producto.domain.model.TipoMovimientoInventario;
import nestoredidev.bodega.producto.domain.model.TipoProducto;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;

import java.util.List;
import java.util.Optional;

public interface ProductoPersistencePort {

    /** Guarda los datos de catálogo. En una fila existente NO toca el stock (solo lo cambia el kardex). */
    Producto guardar(Producto producto);

    /** Guarda únicamente el stock; se llama con la fila bloqueada. */
    Producto guardarStock(Producto producto);

    Optional<Producto> buscarPorId(Long id);

    /** Bloquea la fila (PESSIMISTIC_WRITE) para serializar cambios de stock. */
    Optional<Producto> buscarPorIdParaActualizar(Long id);

    Optional<Producto> buscarPorCodigoBarras(String codigoBarras);

    boolean existePorCodigoBarras(String codigoBarras, Long excluirId);

    boolean esEnvaseDeOtroProducto(Long id);

    PageResult<Producto> listar(String q, Long categoriaId, TipoProducto tipo, Boolean activo, PageQuery query);

    List<Producto> stockBajo();

    MovimientoInventario guardarMovimiento(MovimientoInventario movimiento);

    PageResult<MovimientoInventario> listarMovimientos(Long productoId, TipoMovimientoInventario tipo, PageQuery query);

    List<MovimientoInventario> movimientosDeVenta(Long ventaId);
}
