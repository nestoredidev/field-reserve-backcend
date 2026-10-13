package nestoredidev.cancha.application.ports.input.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalTime;

public record CreateTarifaCommand(
        @Min(value = 1, message = "diaSemana debe estar entre 1 y 7")
        @Max(value = 7, message = "diaSemana debe estar entre 1 y 7")
        Short diaSemana,

        @NotNull(message = "horaInicio es obligatoria") LocalTime horaInicio,
        @NotNull(message = "horaFin es obligatoria") LocalTime horaFin,

        @NotNull(message = "precioHora es obligatorio")
        @DecimalMin(value = "0.01", message = "precioHora debe ser mayor que cero")
        @DecimalMax(value = "99999999.99", message = "precioHora es demasiado grande")
        BigDecimal precioHora
) {
}
