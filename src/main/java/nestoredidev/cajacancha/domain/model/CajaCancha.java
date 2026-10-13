package nestoredidev.cajacancha.domain.model;

import nestoredidev.cajacancha.domain.exception.InvalidCajaCanchaException;
import nestoredidev.common.domain.EstadoCaja;
import nestoredidev.common.domain.Money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public class CajaCancha {
    private final Long id;
    private final LocalDate fecha;
    private final BigDecimal montoApertura;
    private BigDecimal montoCierreContado;
    private EstadoCaja estado;
    private String observacion;
    private final Long abiertaPor;
    private Long cerradaPor;
    private final OffsetDateTime abiertaAt;
    private OffsetDateTime cerradaAt;

    private CajaCancha(Long id, LocalDate fecha, BigDecimal montoApertura, BigDecimal montoCierreContado,
                       EstadoCaja estado, String observacion, Long abiertaPor, Long cerradaPor,
                       OffsetDateTime abiertaAt, OffsetDateTime cerradaAt) {
        this.id = id;
        this.fecha = fecha;
        this.montoApertura = montoApertura;
        this.montoCierreContado = montoCierreContado;
        this.estado = estado;
        this.observacion = observacion;
        this.abiertaPor = abiertaPor;
        this.cerradaPor = cerradaPor;
        this.abiertaAt = abiertaAt;
        this.cerradaAt = cerradaAt;
    }

    /** RN-CAJ-02: el monto de apertura puede ser S/ 0. */
    public static CajaCancha abrir(BigDecimal montoApertura, Long usuarioId, OffsetDateTime ahora) {
        if (montoApertura == null || montoApertura.signum() < 0) {
            throw new InvalidCajaCanchaException("RN-CAJ-02: monto de apertura inválido (no puede ser negativo)");
        }
        return new CajaCancha(null, ahora.toLocalDate(), Money.of(montoApertura), null, EstadoCaja.ABIERTA, null,
                usuarioId, null, ahora, null);
    }

    public static CajaCancha restaurar(Long id, LocalDate fecha, BigDecimal montoApertura,
                                       BigDecimal montoCierreContado, EstadoCaja estado, String observacion,
                                       Long abiertaPor, Long cerradaPor, OffsetDateTime abiertaAt,
                                       OffsetDateTime cerradaAt) {
        return new CajaCancha(id, fecha, montoApertura, montoCierreContado, estado, observacion, abiertaPor,
                cerradaPor, abiertaAt, cerradaAt);
    }

    /** RN-CAJ-03, RN-CAJ-04 y RN-CAJ-05. */
    public void cerrar(BigDecimal contado, BigDecimal efectivoEsperado, String observacion, Long usuarioId,
                       OffsetDateTime ahora) {
        if (estado == EstadoCaja.CERRADA) {
            throw new InvalidCajaCanchaException("RN-CAJ-05: una caja cerrada no se modifica");
        }
        if (contado == null || contado.signum() < 0) {
            throw new InvalidCajaCanchaException("RN-CAJ-03: el efectivo contado es obligatorio y no puede ser negativo");
        }
        boolean hayDiferencia = Money.of(contado).compareTo(Money.of(efectivoEsperado)) != 0;
        if (hayDiferencia && (observacion == null || observacion.isBlank())) {
            throw new InvalidCajaCanchaException(
                    "RN-CAJ-04: hay diferencia entre el efectivo contado y el esperado; la observación es obligatoria");
        }
        if (observacion != null && observacion.length() > 255) {
            throw new InvalidCajaCanchaException("observacion no debe exceder 255 caracteres");
        }
        this.montoCierreContado = Money.of(contado);
        this.observacion = observacion == null || observacion.isBlank() ? null : observacion.trim();
        this.estado = EstadoCaja.CERRADA;
        this.cerradaPor = usuarioId;
        this.cerradaAt = ahora;
    }

    public boolean estaAbierta() {
        return estado == EstadoCaja.ABIERTA;
    }

    public Long getId() { return id; }
    public LocalDate getFecha() { return fecha; }
    public BigDecimal getMontoApertura() { return montoApertura; }
    public BigDecimal getMontoCierreContado() { return montoCierreContado; }
    public EstadoCaja getEstado() { return estado; }
    public String getObservacion() { return observacion; }
    public Long getAbiertaPor() { return abiertaPor; }
    public Long getCerradaPor() { return cerradaPor; }
    public OffsetDateTime getAbiertaAt() { return abiertaAt; }
    public OffsetDateTime getCerradaAt() { return cerradaAt; }
}
