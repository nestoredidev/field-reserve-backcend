package nestoredidev.bodega.compra.application.ports.input.dto;

import nestoredidev.bodega.compra.domain.model.Compra;
import nestoredidev.bodega.compra.domain.model.DetalleCompra;
import nestoredidev.bodega.compra.domain.model.EstadoCompra;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record CompraDto(Long id, Long proveedorId, String nroComprobante, OffsetDateTime fecha, BigDecimal total,
                        EstadoCompra estado, Long registradoPor, List<DetalleCompraDto> detalles) {

    public record DetalleCompraDto(Long id, Long productoId, int cantidadCajas, int cantidadUnidades,
                                   BigDecimal costoUnitario, BigDecimal subtotal) {
        static DetalleCompraDto fromDomain(DetalleCompra d) {
            return new DetalleCompraDto(d.getId(), d.getProductoId(), d.getCantidadCajas(), d.getCantidadUnidades(),
                    d.getCostoUnitario(), d.subtotal());
        }
    }

    public static CompraDto fromDomain(Compra c, boolean conDetalles) {
        return new CompraDto(c.getId(), c.getProveedorId(), c.getNroComprobante(), c.getFecha(), c.getTotal(),
                c.getEstado(), c.getRegistradoPor(),
                conDetalles ? c.getDetalles().stream().map(DetalleCompraDto::fromDomain).toList() : null);
    }
}
