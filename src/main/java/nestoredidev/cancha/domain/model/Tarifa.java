package nestoredidev.cancha.domain.model;

import nestoredidev.cancha.domain.exception.InvalidCanchaException;
import nestoredidev.common.domain.Money;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.LocalTime;

public class Tarifa {
    private final Long id;
    private final Long canchaId;
    private final Short diaSemana;
    private final LocalTime horaInicio;
    private final LocalTime horaFin;
    private final BigDecimal precioHora;
    private final boolean activa;
    private final OffsetDateTime createdAt;

    private Tarifa(Long id, Long canchaId, Short diaSemana, LocalTime horaInicio, LocalTime horaFin,
                   BigDecimal precioHora, boolean activa, OffsetDateTime createdAt) {
        this.id = id;
        this.canchaId = canchaId;
        this.diaSemana = diaSemana;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.precioHora = precioHora;
        this.activa = activa;
        this.createdAt = createdAt;
    }

    public static Tarifa crear(Long canchaId, Short diaSemana, LocalTime horaInicio, LocalTime horaFin,
                               BigDecimal precioHora) {
        if (canchaId == null) {
            throw new InvalidCanchaException("canchaId es obligatorio");
        }
        if (diaSemana != null && (diaSemana < 1 || diaSemana > 7)) {
            throw new InvalidCanchaException("diaSemana debe estar entre 1 (lunes) y 7 (domingo)");
        }
        if (horaInicio == null || horaFin == null) {
            throw new InvalidCanchaException("horaInicio y horaFin son obligatorias");
        }
        if (!horaFin.isAfter(horaInicio)) {
            throw new InvalidCanchaException("horaFin debe ser posterior a horaInicio");
        }
        if (!Money.isPositive(precioHora)) {
            throw new InvalidCanchaException("RN-TAR-03: el precio por hora debe ser mayor que cero");
        }
        return new Tarifa(null, canchaId, diaSemana, horaInicio, horaFin, Money.of(precioHora), true, null);
    }

    public static Tarifa restaurar(Long id, Long canchaId, Short diaSemana, LocalTime horaInicio, LocalTime horaFin,
                                   BigDecimal precioHora, boolean activa, OffsetDateTime createdAt) {
        return new Tarifa(id, canchaId, diaSemana, horaInicio, horaFin, precioHora, activa, createdAt);
    }

    public Tarifa desactivar() {
        return new Tarifa(id, canchaId, diaSemana, horaInicio, horaFin, precioHora, false, createdAt);
    }

    /** RN-TAR-06: el precio no se edita; se crea una tarifa nueva con las mismas franjas. */
    public Tarifa conNuevoPrecio(BigDecimal nuevoPrecio) {
        return crear(canchaId, diaSemana, horaInicio, horaFin, nuevoPrecio);
    }

    public Long getId() { return id; }
    public Long getCanchaId() { return canchaId; }
    public Short getDiaSemana() { return diaSemana; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public LocalTime getHoraFin() { return horaFin; }
    public BigDecimal getPrecioHora() { return precioHora; }
    public boolean isActiva() { return activa; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
