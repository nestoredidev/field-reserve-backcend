package nestoredidev.bodega.venta.application.ports.input.dto;

import nestoredidev.bodega.venta.domain.model.EstadoVenta;
import nestoredidev.common.domain.MetodoPago;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/** costoUnitario solo lo ve el ADMIN (reglas.md: reportes de ganancia). */
public record VentaDto(Long id, Long cajaId, OffsetDateTime fecha, BigDecimal total, EstadoVenta estado,
                       String motivoAnulacion, Long registradoPor, List<DetalleVentaDto> detalles,
                       List<PagoVentaDto> pagos) {

    public record DetalleVentaDto(Long id, Long productoId, String productoNombre, int cantidad,
                                  BigDecimal precioUnitario, BigDecimal costoUnitario, BigDecimal subtotal) {
    }

    public record PagoVentaDto(Long id, MetodoPago metodo, BigDecimal monto, String nroOperacion,
                               OffsetDateTime fecha) {
    }
}
