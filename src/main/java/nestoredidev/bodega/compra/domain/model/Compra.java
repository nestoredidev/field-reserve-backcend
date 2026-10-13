package nestoredidev.bodega.compra.domain.model;

import nestoredidev.bodega.compra.domain.exception.InvalidCompraException;
import nestoredidev.common.domain.Money;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Compra {
    private final Long id;
    private final Long proveedorId;
    private final String nroComprobante;
    private final OffsetDateTime fecha;
    private final BigDecimal total;
    private EstadoCompra estado;
    private final Long registradoPor;
    private final List<DetalleCompra> detalles;

    private Compra(Long id, Long proveedorId, String nroComprobante, OffsetDateTime fecha, BigDecimal total,
                   EstadoCompra estado, Long registradoPor, List<DetalleCompra> detalles) {
        this.id = id;
        this.proveedorId = proveedorId;
        this.nroComprobante = nroComprobante;
        this.fecha = fecha;
        this.total = total;
        this.estado = estado;
        this.registradoPor = registradoPor;
        this.detalles = detalles;
    }

    /** RN-COM-01 y RN-COM-03. */
    public static Compra crear(Long proveedorId, String nroComprobante, List<DetalleCompra> detalles,
                               Long registradoPor) {
        if (detalles == null || detalles.isEmpty()) {
            throw new InvalidCompraException("RN-COM-01: la compra necesita al menos un detalle");
        }
        Set<Long> productos = new HashSet<>();
        for (DetalleCompra d : detalles) {
            if (!productos.add(d.getProductoId())) {
                throw new InvalidCompraException("RN-COM-01: el producto " + d.getProductoId()
                        + " aparece más de una vez en la compra");
            }
        }
        if (nroComprobante != null && nroComprobante.trim().length() > 30) {
            throw new InvalidCompraException("nroComprobante no debe exceder 30 caracteres");
        }
        BigDecimal total = detalles.stream().map(DetalleCompra::subtotal).reduce(Money.ZERO, BigDecimal::add);
        return new Compra(null, proveedorId, nroComprobante == null || nroComprobante.isBlank() ? null
                : nroComprobante.trim(), null, total, EstadoCompra.REGISTRADA, registradoPor, List.copyOf(detalles));
    }

    public static Compra restaurar(Long id, Long proveedorId, String nroComprobante, OffsetDateTime fecha,
                                   BigDecimal total, EstadoCompra estado, Long registradoPor,
                                   List<DetalleCompra> detalles) {
        return new Compra(id, proveedorId, nroComprobante, fecha, total, estado, registradoPor, detalles);
    }

    /** RN-COM-06 (el motivo se guarda en el kardex). */
    public void anular(String motivo) {
        if (estado == EstadoCompra.ANULADA) {
            throw new InvalidCompraException("La compra ya está anulada");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new InvalidCompraException("RN-COM-06: el motivo de anulación es obligatorio");
        }
        if (motivo.trim().length() > 255) {
            throw new InvalidCompraException("motivo no debe exceder 255 caracteres");
        }
        this.estado = EstadoCompra.ANULADA;
    }

    public Long getId() { return id; }
    public Long getProveedorId() { return proveedorId; }
    public String getNroComprobante() { return nroComprobante; }
    public OffsetDateTime getFecha() { return fecha; }
    public BigDecimal getTotal() { return total; }
    public EstadoCompra getEstado() { return estado; }
    public Long getRegistradoPor() { return registradoPor; }
    public List<DetalleCompra> getDetalles() { return detalles; }
}
