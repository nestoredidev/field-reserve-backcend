package nestoredidev.reserva.domain.model;

import java.math.BigDecimal;
import java.time.LocalTime;

/** Franja de precio aplicable, ya filtrada por cancha y día. diaSemana null = general. */
public record TarifaVigente(Short diaSemana, LocalTime horaInicio, LocalTime horaFin, BigDecimal precioHora) {
    boolean cubre(LocalTime inicio, LocalTime fin) {
        return !horaInicio.isAfter(inicio) && !horaFin.isBefore(fin);
    }
}
