package nestoredidev.reserva.domain.model;

import nestoredidev.common.domain.Money;
import nestoredidev.reserva.domain.exception.InvalidReservaException;

import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Reserva de cancha o bloqueo de mantenimiento (misma tabla: así un solo índice impide cruces). */
public class Reserva {
    public static final int MAX_DIAS_ANTICIPACION = 30;
    public static final int MINUTOS_TOLERANCIA_NO_ASISTIO = 15;
    public static final int HORAS_PARA_ADELANTO = 2;
    public static final int HORAS_DEVOLUCION = 24;

    private final Long id;
    private final Long canchaId;
    private final Long clienteId;
    private final TipoReserva tipo;
    private EstadoReserva estado;
    private OffsetDateTime inicio;
    private OffsetDateTime fin;
    private BigDecimal montoTotal;
    private OffsetDateTime fechaLimiteAdelanto;
    private final OrigenReserva origen;
    private String observacion;
    private String motivoCancelacion;
    private final Long creadoPor;
    private final OffsetDateTime createdAt;

    private Reserva(Long id, Long canchaId, Long clienteId, TipoReserva tipo, EstadoReserva estado,
                    OffsetDateTime inicio, OffsetDateTime fin, BigDecimal montoTotal,
                    OffsetDateTime fechaLimiteAdelanto, OrigenReserva origen, String observacion,
                    String motivoCancelacion, Long creadoPor, OffsetDateTime createdAt) {
        this.id = id;
        this.canchaId = canchaId;
        this.clienteId = clienteId;
        this.tipo = tipo;
        this.estado = estado;
        this.inicio = inicio;
        this.fin = fin;
        this.montoTotal = montoTotal;
        this.fechaLimiteAdelanto = fechaLimiteAdelanto;
        this.origen = origen;
        this.observacion = observacion;
        this.motivoCancelacion = motivoCancelacion;
        this.creadoPor = creadoPor;
        this.createdAt = createdAt;
    }

    // ---------------------------------------------------------------- creación

    public static Reserva crearReserva(Long canchaId, Long clienteId, OffsetDateTime inicio, OffsetDateTime fin,
                                       List<TarifaVigente> tarifas, OrigenReserva origen, String observacion,
                                       Long creadoPor, OffsetDateTime ahora) {
        if (canchaId == null || clienteId == null) {
            throw new InvalidReservaException("canchaId y clienteId son obligatorios");
        }
        validarObservacion(observacion);
        BigDecimal monto = validarYCalcularMonto(inicio, fin, tarifas, ahora);
        OffsetDateTime limite = ahora.plusHours(HORAS_PARA_ADELANTO);
        if (limite.isAfter(inicio)) {
            limite = inicio;
        }
        return new Reserva(null, canchaId, clienteId, TipoReserva.RESERVA, EstadoReserva.PENDIENTE, inicio, fin,
                monto, limite, origen == null ? OrigenReserva.PRESENCIAL : origen, observacion(observacion), null,
                creadoPor, null);
    }

    /** RN-RES-20 / RN-RES-21: sin cliente, monto 0, motivo obligatorio y nace CONFIRMADO. */
    public static Reserva crearMantenimiento(Long canchaId, OffsetDateTime inicio, OffsetDateTime fin,
                                             List<TarifaVigente> tarifas, String motivo, Long creadoPor,
                                             OffsetDateTime ahora) {
        if (canchaId == null) {
            throw new InvalidReservaException("canchaId es obligatorio");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new InvalidReservaException("RN-RES-20: el motivo del mantenimiento es obligatorio");
        }
        validarObservacion(motivo);
        validarYCalcularMonto(inicio, fin, tarifas, ahora); // mismas reglas de horario (RN-RES-21)
        return new Reserva(null, canchaId, null, TipoReserva.MANTENIMIENTO, EstadoReserva.CONFIRMADA, inicio, fin,
                Money.ZERO, null, OrigenReserva.PRESENCIAL, motivo.trim(), null, creadoPor, null);
    }

    public static Reserva restaurar(Long id, Long canchaId, Long clienteId, TipoReserva tipo, EstadoReserva estado,
                                    OffsetDateTime inicio, OffsetDateTime fin, BigDecimal montoTotal,
                                    OffsetDateTime fechaLimiteAdelanto, OrigenReserva origen, String observacion,
                                    String motivoCancelacion, Long creadoPor, OffsetDateTime createdAt) {
        return new Reserva(id, canchaId, clienteId, tipo, estado, inicio, fin, montoTotal, fechaLimiteAdelanto,
                origen, observacion, motivoCancelacion, creadoPor, createdAt);
    }

    /**
     * RN-RES-01..04 y RN-RES-08. Devuelve el monto: suma hora por hora del precio de la franja en que cae.
     */
    public static BigDecimal validarYCalcularMonto(OffsetDateTime inicio, OffsetDateTime fin,
                                                   List<TarifaVigente> tarifas, OffsetDateTime ahora) {
        if (inicio == null || fin == null) {
            throw new InvalidReservaException("inicio y fin son obligatorios");
        }
        ZoneId zona = ahora.getOffset() == null ? ZoneOffset.UTC : ZoneId.from(ahora.getOffset());
        ZonedDateTime i = inicio.atZoneSameInstant(zona);
        ZonedDateTime f = fin.atZoneSameInstant(zona);
        if (i.getMinute() != 0 || i.getSecond() != 0 || i.getNano() != 0
                || f.getMinute() != 0 || f.getSecond() != 0 || f.getNano() != 0) {
            throw new InvalidReservaException("RN-RES-01: la reserva empieza y termina en hora en punto");
        }
        long horas = ChronoUnit.HOURS.between(i, f);
        if (!f.isAfter(i) || horas < 1) {
            throw new InvalidReservaException("RN-RES-01: la reserva dura mínimo 1 hora completa");
        }
        boolean mismoDia = i.toLocalDate().equals(f.toLocalDate())
                || (f.toLocalTime().equals(LocalTime.MIDNIGHT) && f.toLocalDate().equals(i.toLocalDate().plusDays(1)));
        if (!mismoDia) {
            throw new InvalidReservaException("RN-RES-02: la reserva debe terminar el mismo día (no cruza la medianoche)");
        }
        if (i.isBefore(ahora.atZoneSameInstant(zona).truncatedTo(ChronoUnit.HOURS))) {
            throw new InvalidReservaException("RN-RES-03: no se crean reservas en el pasado");
        }
        if (inicio.isAfter(ahora.plusDays(MAX_DIAS_ANTICIPACION))) {
            throw new InvalidReservaException("RN-RES-04: se reserva con máximo " + MAX_DIAS_ANTICIPACION
                    + " días de anticipación");
        }
        BigDecimal total = Money.ZERO;
        short dia = (short) i.getDayOfWeek().getValue();
        for (int h = 0; h < horas; h++) {
            ZonedDateTime horaInicio = i.plusHours(h);
            LocalTime desde = horaInicio.toLocalTime();
            BigDecimal precio = precioDeLaHora(tarifas, dia, desde);
            if (precio == null) {
                throw new InvalidReservaException("RN-RES-02: la hora " + desde + " está fuera del horario de atención");
            }
            total = total.add(precio);
        }
        return Money.of(total);
    }

    /** Precio de la hora que empieza en {@code desde}, o null si queda fuera del horario de atención. */
    public static BigDecimal precioDeLaHora(List<TarifaVigente> tarifas, short dia, LocalTime desde) {
        LocalTime hasta = desde.plusHours(1);
        BigDecimal general = null;
        for (TarifaVigente t : tarifas) {
            boolean cubre = hasta.isAfter(desde) ? t.cubre(desde, hasta) : t.cubre(desde, LocalTime.MAX);
            if (!cubre) {
                continue;
            }
            if (t.diaSemana() != null && t.diaSemana() == dia) {
                return t.precioHora(); // RN-TAR-04: la franja del día específico gana
            }
            if (t.diaSemana() == null) {
                general = t.precioHora();
            }
        }
        return general;
    }

    // ---------------------------------------------------------------- comportamiento

    public boolean esMantenimiento() {
        return tipo == TipoReserva.MANTENIMIENTO;
    }

    /** RN-RES-11/13: pasa a CONFIRMADA al alcanzar el adelanto mínimo. */
    public void confirmarSiCorresponde(BigDecimal pagado, BigDecimal porcentajeAdelanto) {
        if (estado == EstadoReserva.PENDIENTE && !esMantenimiento()
                && pagado.compareTo(adelantoMinimo(porcentajeAdelanto)) >= 0) {
            estado = EstadoReserva.CONFIRMADA;
        }
    }

    /** RN-RES-19: se completa cuando el saldo es 0 y la hora ya empezó. */
    public void completarSiCorresponde(BigDecimal pagado, OffsetDateTime ahora) {
        if (estado == EstadoReserva.CONFIRMADA && !esMantenimiento()
                && pagado.compareTo(montoTotal) >= 0 && !ahora.isBefore(inicio)) {
            estado = EstadoReserva.COMPLETADA;
        }
    }

    public BigDecimal adelantoMinimo(BigDecimal porcentaje) {
        return Money.of(montoTotal.multiply(porcentaje));
    }

    /** RN-RES-15 */
    public void cancelar(String motivo) {
        if (!estado.esActiva()) {
            throw new InvalidReservaException("RN-RES-15: solo se cancelan reservas PENDIENTE o CONFIRMADA");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new InvalidReservaException("RN-RES-15: el motivo de cancelación es obligatorio");
        }
        if (motivo.trim().length() > 255) {
            throw new InvalidReservaException("motivo no debe exceder 255 caracteres");
        }
        this.estado = EstadoReserva.CANCELADA;
        this.motivoCancelacion = motivo.trim();
    }

    /** RN-RES-16: con 24 h o más de anticipación el adelanto se devuelve. */
    public boolean cancelacionConDevolucion(OffsetDateTime ahora) {
        return !ahora.plusHours(HORAS_DEVOLUCION).isAfter(inicio);
    }

    /** RN-RES-17 */
    public void reprogramar(OffsetDateTime nuevoInicio, OffsetDateTime nuevoFin, List<TarifaVigente> tarifas,
                            OffsetDateTime ahora) {
        if (!estado.esActiva()) {
            throw new InvalidReservaException("RN-RES-17: solo se reprograman reservas PENDIENTE o CONFIRMADA");
        }
        BigDecimal nuevoMonto = validarYCalcularMonto(nuevoInicio, nuevoFin, tarifas, ahora);
        this.inicio = nuevoInicio;
        this.fin = nuevoFin;
        if (!esMantenimiento()) {
            this.montoTotal = nuevoMonto;
            if (estado == EstadoReserva.PENDIENTE && fechaLimiteAdelanto != null
                    && fechaLimiteAdelanto.isAfter(nuevoInicio)) {
                this.fechaLimiteAdelanto = nuevoInicio;
            }
        }
    }

    /** RN-RES-18 */
    public void marcarNoAsistio(OffsetDateTime ahora) {
        if (estado != EstadoReserva.CONFIRMADA || esMantenimiento()) {
            throw new InvalidReservaException("RN-RES-18: solo una reserva CONFIRMADA pasa a NO_ASISTIO");
        }
        if (ahora.isBefore(inicio.plusMinutes(MINUTOS_TOLERANCIA_NO_ASISTIO))) {
            throw new InvalidReservaException("RN-RES-18: deben pasar " + MINUTOS_TOLERANCIA_NO_ASISTIO
                    + " minutos desde la hora de inicio");
        }
        this.estado = EstadoReserva.NO_ASISTIO;
    }

    /** RN-RES-19: no se completa con deuda. */
    public void completar(BigDecimal pagado, OffsetDateTime ahora) {
        if (estado != EstadoReserva.CONFIRMADA || esMantenimiento()) {
            throw new InvalidReservaException("RN-RES-19: solo una reserva CONFIRMADA se completa");
        }
        if (pagado.compareTo(montoTotal) < 0) {
            throw new InvalidReservaException("RN-RES-19: no se completa con deuda (saldo S/ "
                    + montoTotal.subtract(pagado) + ")");
        }
        if (ahora.isBefore(inicio)) {
            throw new InvalidReservaException("RN-RES-19: la hora de la reserva todavía no empieza");
        }
        this.estado = EstadoReserva.COMPLETADA;
    }

    /** RN-RES-10: solo cambia el ADMIN (controlado en el servicio), con observación obligatoria. */
    public void cambiarMonto(BigDecimal nuevoMonto, String observacion, BigDecimal pagado) {
        if (esMantenimiento()) {
            throw new InvalidReservaException("Un mantenimiento no tiene monto");
        }
        if (estado.esFinal()) {
            throw new InvalidReservaException("RN-RES-12: la reserva está en estado final");
        }
        if (!Money.isPositive(nuevoMonto)) {
            throw new InvalidReservaException("El monto debe ser mayor que cero");
        }
        if (observacion == null || observacion.isBlank()) {
            throw new InvalidReservaException("RN-RES-10: la observación es obligatoria al cambiar el monto");
        }
        validarObservacion(observacion);
        if (Money.of(nuevoMonto).compareTo(pagado) < 0) {
            throw new InvalidReservaException("RN-PAG-03: el nuevo monto no puede ser menor a lo ya pagado (S/ "
                    + pagado + ")");
        }
        this.montoTotal = Money.of(nuevoMonto);
        this.observacion = observacion.trim();
    }

    /** RN-RES-14: la PENDIENTE sin adelanto a tiempo se cancela sola. */
    public boolean adelantoVencido(OffsetDateTime ahora) {
        return estado == EstadoReserva.PENDIENTE && fechaLimiteAdelanto != null && ahora.isAfter(fechaLimiteAdelanto);
    }

    private static void validarObservacion(String observacion) {
        if (observacion != null && observacion.trim().length() > 255) {
            throw new InvalidReservaException("observacion no debe exceder 255 caracteres");
        }
    }

    private static String observacion(String o) {
        return o == null || o.isBlank() ? null : o.trim();
    }

    public Long getId() { return id; }
    public Long getCanchaId() { return canchaId; }
    public Long getClienteId() { return clienteId; }
    public TipoReserva getTipo() { return tipo; }
    public EstadoReserva getEstado() { return estado; }
    public OffsetDateTime getInicio() { return inicio; }
    public OffsetDateTime getFin() { return fin; }
    public BigDecimal getMontoTotal() { return montoTotal; }
    public OffsetDateTime getFechaLimiteAdelanto() { return fechaLimiteAdelanto; }
    public OrigenReserva getOrigen() { return origen; }
    public String getObservacion() { return observacion; }
    public String getMotivoCancelacion() { return motivoCancelacion; }
    public Long getCreadoPor() { return creadoPor; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
