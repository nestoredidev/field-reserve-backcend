package nestoredidev.reserva.application.ports.input.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreateMantenimientoCommand(
        @NotNull(message = "canchaId es obligatorio") Long canchaId,
        @NotNull(message = "inicio es obligatorio") LocalDateTime inicio,
        @NotNull(message = "fin es obligatorio") LocalDateTime fin,

        @NotBlank(message = "motivo es obligatorio")
        @Size(max = 255, message = "motivo no debe exceder 255 caracteres")
        String motivo
) {
}
