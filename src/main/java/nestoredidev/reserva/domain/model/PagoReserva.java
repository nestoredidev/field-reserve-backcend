package nestoredidev.reserva.domain.model;

import nestoredidev.common.domain.MetodoPago;
import nestoredidev.common.domain.Money;
import nestoredidev.reserva.domain.exception.InvalidReservaException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class PagoReserva {
    private final Long id;
    private final Long reservaId;
    private final Long cajaId;
    private final TipoPagoReserva tipo;
    private final MetodoPago metodo;
    private final BigDecimal monto;
    private final String nroOperacion;
    private final String voucherUrl;
    private final Long registradoPor;
    private final OffsetDateTime fecha;

    private PagoReserva(Long id, Long reservaId, Long cajaId, TipoPagoReserva tipo, MetodoPago metodo,
                        BigDecimal monto, String nroOperacion, String voucherUrl, Long registradoPor,
                        OffsetDateTime fecha) {
        this.id = id;
        this.reservaId = reservaId;
        this.cajaId = cajaId;
        this.tipo = tipo;
        this.metodo = metodo;
        this.monto = monto;
        this.nroOperacion = nroOperacion;
        this.voucherUrl = voucherUrl;
        this.registradoPor = registradoPor;
        this.fecha = fecha;
    }

    public static PagoReserva crear(Long reservaId, Long cajaId, TipoPagoReserva tipo, MetodoPago metodo,
                                    BigDecimal monto, String nroOperacion, String voucherUrl, Long registradoPor) {
        if (metodo == null) {
            throw new InvalidReservaException("metodo es obligatorio");
        }
        if (!Money.isPositive(monto)) {
            throw new InvalidReservaException("RN-PAG-03: el monto debe ser mayor que cero");
        }
        String nro = nroOperacion == null || nroOperacion.isBlank() ? null : nroOperacion.trim();
        if (metodo.requiereNroOperacion() && nro == null) {
            throw new InvalidReservaException("RN-PAG-05: el número de operación es obligatorio para " + metodo);
        }
        if (nro != null && nro.length() > 30) {
            throw new InvalidReservaException("nroOperacion no debe exceder 30 caracteres");
        }
        if (voucherUrl != null && voucherUrl.length() > 500) {
            throw new InvalidReservaException("voucherUrl no debe exceder 500 caracteres");
        }
        return new PagoReserva(null, reservaId, cajaId, tipo, metodo, Money.of(monto),
                metodo.requiereNroOperacion() ? nro : null,
                voucherUrl == null || voucherUrl.isBlank() ? null : voucherUrl.trim(), registradoPor, null);
    }

    public static PagoReserva restaurar(Long id, Long reservaId, Long cajaId, TipoPagoReserva tipo,
                                        MetodoPago metodo, BigDecimal monto, String nroOperacion,
                                        String voucherUrl, Long registradoPor, OffsetDateTime fecha) {
        return new PagoReserva(id, reservaId, cajaId, tipo, metodo, monto, nroOperacion, voucherUrl,
                registradoPor, fecha);
    }

    public Long getId() { return id; }
    public Long getReservaId() { return reservaId; }
    public Long getCajaId() { return cajaId; }
    public TipoPagoReserva getTipo() { return tipo; }
    public MetodoPago getMetodo() { return metodo; }
    public BigDecimal getMonto() { return monto; }
    public String getNroOperacion() { return nroOperacion; }
    public String getVoucherUrl() { return voucherUrl; }
    public Long getRegistradoPor() { return registradoPor; }
    public OffsetDateTime getFecha() { return fecha; }
}
