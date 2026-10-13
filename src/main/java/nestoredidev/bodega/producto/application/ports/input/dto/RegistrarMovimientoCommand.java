package nestoredidev.bodega.producto.application.ports.input.dto;

import nestoredidev.bodega.producto.domain.model.TipoMovimientoInventario;

/** Uso interno (compras y ventas): no se expone por REST. */
public record RegistrarMovimientoCommand(Long productoId, TipoMovimientoInventario tipo, int cantidad,
                                         Long compraId, Long ventaId, String motivo) {
}
