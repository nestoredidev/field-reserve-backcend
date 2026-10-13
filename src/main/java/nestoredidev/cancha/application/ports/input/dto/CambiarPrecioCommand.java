package nestoredidev.cancha.application.ports.input.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CambiarPrecioCommand(
        @NotNull(message = "precioHora es obligatorio")
        @DecimalMin(value = "0.01", message = "precioHora debe ser mayor que cero")
        @DecimalMax(value = "99999999.99", message = "precioHora es demasiado grande")
        BigDecimal precioHora
) {
}
