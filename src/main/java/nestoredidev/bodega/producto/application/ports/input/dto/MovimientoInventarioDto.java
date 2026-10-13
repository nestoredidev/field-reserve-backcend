package nestoredidev.bodega.producto.application.ports.input.dto;

import nestoredidev.bodega.producto.domain.model.MovimientoInventario;
import nestoredidev.bodega.producto.domain.model.TipoMovimientoInventario;

import java.time.OffsetDateTime;

public record MovimientoInventarioDto(Long id, Long productoId, TipoMovimientoInventario tipo, int cantidad,
                                      int stockAnterior, int stockResultante, Long compraId, Long ventaId,
                                      String motivo, Long registradoPor, OffsetDateTime fecha) {
    public static MovimientoInventarioDto fromDomain(MovimientoInventario m) {
        return new MovimientoInventarioDto(m.getId(), m.getProductoId(), m.getTipo(), m.getCantidad(),
                m.getStockAnterior(), m.getStockResultante(), m.getCompraId(), m.getVentaId(), m.getMotivo(),
                m.getRegistradoPor(), m.getFecha());
    }
}
