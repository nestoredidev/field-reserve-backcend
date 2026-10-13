package nestoredidev.reserva.application.ports.input.dto;

import java.math.BigDecimal;
import java.time.LocalTime;

public record SlotDto(LocalTime horaInicio, LocalTime horaFin, BigDecimal precioHora, boolean disponible) {
}
