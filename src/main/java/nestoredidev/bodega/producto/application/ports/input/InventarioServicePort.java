package nestoredidev.bodega.producto.application.ports.input;

import nestoredidev.bodega.producto.application.ports.input.dto.*;
import nestoredidev.bodega.producto.domain.model.TipoMovimientoInventario;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

public interface InventarioServicePort {

    /** Ajuste manual del ADMIN (ajuste, merma, envases). */
    MovimientoInventarioDto ajustar(AjusteInventarioCommand command);

    PageResult<MovimientoInventarioDto> listarMovimientos(Long productoId, TipoMovimientoInventario tipo,
                                                          PageQuery query);

    // ---- API interna para compras y ventas ----

    /** Lee y bloquea los productos (en orden de id) para operar sobre su stock en la transacción actual. */
    List<ProductoDto> obtenerParaOperar(Collection<Long> ids);

    MovimientoInventarioDto registrarMovimiento(RegistrarMovimientoCommand command);

    List<MovimientoInventarioDto> movimientosDeVenta(Long ventaId);

    /** RN-PRO-06 */
    void actualizarPrecioCompra(Long productoId, BigDecimal costoUnitario);
}
