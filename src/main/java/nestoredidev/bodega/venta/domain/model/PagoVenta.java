package nestoredidev.bodega.venta.domain.model;

import nestoredidev.bodega.venta.domain.exception.InvalidVentaException;
import nestoredidev.common.domain.MetodoPago;
import nestoredidev.common.domain.Money;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class PagoVenta {
    private final Long id;
    private final MetodoPago metodo;
    private final BigDecimal monto;
    private final String nroOperacion;
    private final OffsetDateTime fecha;

    private PagoVenta(Long id, MetodoPago metodo, BigDecimal monto, String nroOperacion, OffsetDateTime fecha) {
        this.id = id;
        this.metodo = metodo;
        this.monto = monto;
        this.nroOperacion = nroOperacion;
        this.fecha = fecha;
    }

    /** RN-PVE-01 y RN-PVE-03. */
    public static PagoVenta crear(MetodoPago metodo, BigDecimal monto, String nroOperacion) {
        if (metodo == null) {
            throw new InvalidVentaException("metodo es obligatorio");
        }
        if (!Money.isPositive(monto)) {
            throw new InvalidVentaException("RN-PVE-01: cada pago debe ser mayor que cero");
        }
        String nro = nroOperacion == null || nroOperacion.isBlank() ? null : nroOperacion.trim();
        if (metodo.requiereNroOperacion() && nro == null) {
            throw new InvalidVentaException("RN-PVE-03: el número de operación es obligatorio para " + metodo);
        }
        if (nro != null && nro.length() > 30) {
            throw new InvalidVentaException("nroOperacion no debe exceder 30 caracteres");
        }
        return new PagoVenta(null, metodo, Money.of(monto), metodo.requiereNroOperacion() ? nro : null, null);
    }

    public static PagoVenta restaurar(Long id, MetodoPago metodo, BigDecimal monto, String nroOperacion,
                                      OffsetDateTime fecha) {
        return new PagoVenta(id, metodo, monto, nroOperacion, fecha);
    }

    public Long getId() { return id; }
    public MetodoPago getMetodo() { return metodo; }
    public BigDecimal getMonto() { return monto; }
    public String getNroOperacion() { return nroOperacion; }
    public OffsetDateTime getFecha() { return fecha; }
}
