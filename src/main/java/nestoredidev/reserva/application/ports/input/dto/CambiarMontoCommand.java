package nestoredidev.reserva.application.ports.input.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CambiarMontoCommand(
        @NotNull(message = "monto es obligatorio")
        @DecimalMin(value = "0.01", message = "monto debe ser mayor que cero")
        @DecimalMax(value = "99999999.99", message = "monto es demasiado grande")
        BigDecimal monto,

        @NotBlank(message = "observacion es obligatoria")
        @Size(max = 255, message = "observacion no debe exceder 255 caracteres")
        String observacion
) {
}
