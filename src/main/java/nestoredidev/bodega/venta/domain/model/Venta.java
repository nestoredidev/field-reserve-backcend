package nestoredidev.bodega.venta.domain.model;

import nestoredidev.bodega.venta.domain.exception.InvalidVentaException;
import nestoredidev.common.domain.Money;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Venta {
    private final Long id;
    private final Long cajaId;
    private final OffsetDateTime fecha;
    private final BigDecimal total;
    private EstadoVenta estado;
    private String motivoAnulacion;
    private final Long registradoPor;
    private final List<DetalleVenta> detalles;
    private final List<PagoVenta> pagos;

    private Venta(Long id, Long cajaId, OffsetDateTime fecha, BigDecimal total, EstadoVenta estado,
                  String motivoAnulacion, Long registradoPor, List<DetalleVenta> detalles, List<PagoVenta> pagos) {
        this.id = id;
        this.cajaId = cajaId;
        this.fecha = fecha;
        this.total = total;
        this.estado = estado;
        this.motivoAnulacion = motivoAnulacion;
        this.registradoPor = registradoPor;
        this.detalles = detalles;
        this.pagos = pagos;
    }

    /** RN-VEN-01..03 y RN-DVE-01: detalle sin repetidos y pagos que suman exactamente el total. */
    public static Venta crear(Long cajaId, List<DetalleVenta> detalles, List<PagoVenta> pagos, Long registradoPor) {
        if (detalles == null || detalles.isEmpty()) {
            throw new InvalidVentaException("RN-VEN-01: la venta necesita al menos un detalle");
        }
        Set<Long> productos = new HashSet<>();
        for (DetalleVenta d : detalles) {
            if (!productos.add(d.getProductoId())) {
                throw new InvalidVentaException("RN-DVE-01: el producto " + d.getProductoId()
                        + " aparece más de una vez en la venta");
            }
        }
        BigDecimal total = detalles.stream().map(DetalleVenta::subtotal).reduce(Money.ZERO, BigDecimal::add);
        if (!Money.isPositive(total)) {
            throw new InvalidVentaException("El total de la venta debe ser mayor que cero");
        }
        if (pagos == null || pagos.isEmpty()) {
            throw new InvalidVentaException("RN-VEN-03: la venta necesita al menos un pago");
        }
        BigDecimal pagado = pagos.stream().map(PagoVenta::getMonto).reduce(Money.ZERO, BigDecimal::add);
        if (pagado.compareTo(total) != 0) {
            throw new InvalidVentaException("RN-VEN-03: la suma de los pagos (S/ " + pagado
                    + ") debe ser igual al total (S/ " + total + ")");
        }
        return new Venta(null, cajaId, null, total, EstadoVenta.REGISTRADA, null, registradoPor,
                List.copyOf(detalles), List.copyOf(pagos));
    }

    public static Venta restaurar(Long id, Long cajaId, OffsetDateTime fecha, BigDecimal total, EstadoVenta estado,
                                  String motivoAnulacion, Long registradoPor, List<DetalleVenta> detalles,
                                  List<PagoVenta> pagos) {
        return new Venta(id, cajaId, fecha, total, estado, motivoAnulacion, registradoPor, detalles, pagos);
    }

    /** RN-VEN-04 (que la caja siga abierta lo valida el servicio). */
    public void anular(String motivo) {
        if (estado == EstadoVenta.ANULADA) {
            throw new InvalidVentaException("La venta ya está anulada");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new InvalidVentaException("RN-VEN-04: el motivo de anulación es obligatorio");
        }
        if (motivo.trim().length() > 255) {
            throw new InvalidVentaException("motivo no debe exceder 255 caracteres");
        }
        this.estado = EstadoVenta.ANULADA;
        this.motivoAnulacion = motivo.trim();
    }

    public Long getId() { return id; }
    public Long getCajaId() { return cajaId; }
    public OffsetDateTime getFecha() { return fecha; }
    public BigDecimal getTotal() { return total; }
    public EstadoVenta getEstado() { return estado; }
    public String getMotivoAnulacion() { return motivoAnulacion; }
    public Long getRegistradoPor() { return registradoPor; }
    public List<DetalleVenta> getDetalles() { return detalles; }
    public List<PagoVenta> getPagos() { return pagos; }
}
