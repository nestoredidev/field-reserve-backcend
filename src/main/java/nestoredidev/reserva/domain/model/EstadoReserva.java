package nestoredidev.reserva.domain.model;

public enum EstadoReserva {
    PENDIENTE, CONFIRMADA, COMPLETADA, CANCELADA, NO_ASISTIO;

    /** RN-RES-12 */
    public boolean esFinal() {
        return this == COMPLETADA || this == CANCELADA || this == NO_ASISTIO;
    }

    public boolean esActiva() {
        return this == PENDIENTE || this == CONFIRMADA;
    }
}
