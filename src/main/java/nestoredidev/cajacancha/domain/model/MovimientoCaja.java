package nestoredidev.cajacancha.domain.model;

import nestoredidev.cajacancha.domain.exception.InvalidCajaCanchaException;
import nestoredidev.common.domain.MetodoPago;
import nestoredidev.common.domain.Money;
import nestoredidev.common.domain.TipoMovimientoCaja;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class MovimientoCaja {
    private final Long id;
    private final Long cajaId;
    private final TipoMovimientoCaja tipo;
    private final MetodoPago metodo;
    private final String concepto;
    private final BigDecimal monto;
    private final Long registradoPor;
    private final OffsetDateTime fecha;

    private MovimientoCaja(Long id, Long cajaId, TipoMovimientoCaja tipo, MetodoPago metodo, String concepto,
                           BigDecimal monto, Long registradoPor, OffsetDateTime fecha) {
        this.id = id;
        this.cajaId = cajaId;
        this.tipo = tipo;
        this.metodo = metodo;
        this.concepto = concepto;
        this.monto = monto;
        this.registradoPor = registradoPor;
        this.fecha = fecha;
    }

    /** RN-MCA-02: concepto obligatorio y monto mayor que cero. */
    public static MovimientoCaja crear(Long cajaId, TipoMovimientoCaja tipo, MetodoPago metodo, String concepto,
                                       BigDecimal monto, Long registradoPor) {
        if (tipo == null || metodo == null) {
            throw new InvalidCajaCanchaException("tipo y metodo son obligatorios");
        }
        if (concepto == null || concepto.isBlank()) {
            throw new InvalidCajaCanchaException("RN-MCA-02: el concepto es obligatorio");
        }
        if (concepto.trim().length() > 150) {
            throw new InvalidCajaCanchaException("concepto no debe exceder 150 caracteres");
        }
        if (!Money.isPositive(monto)) {
            throw new InvalidCajaCanchaException("RN-MCA-02: el monto debe ser mayor que cero");
        }
        return new MovimientoCaja(null, cajaId, tipo, metodo, concepto.trim(), Money.of(monto), registradoPor, null);
    }

    public static MovimientoCaja restaurar(Long id, Long cajaId, TipoMovimientoCaja tipo, MetodoPago metodo,
                                           String concepto, BigDecimal monto, Long registradoPor,
                                           OffsetDateTime fecha) {
        return new MovimientoCaja(id, cajaId, tipo, metodo, concepto, monto, registradoPor, fecha);
    }

    public boolean esEgresoEfectivo() {
        return tipo == TipoMovimientoCaja.EGRESO && metodo == MetodoPago.EFECTIVO;
    }

    public Long getId() { return id; }
    public Long getCajaId() { return cajaId; }
    public TipoMovimientoCaja getTipo() { return tipo; }
    public MetodoPago getMetodo() { return metodo; }
    public String getConcepto() { return concepto; }
    public BigDecimal getMonto() { return monto; }
    public Long getRegistradoPor() { return registradoPor; }
    public OffsetDateTime getFecha() { return fecha; }
}
