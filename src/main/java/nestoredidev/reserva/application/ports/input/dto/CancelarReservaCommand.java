package nestoredidev.reserva.application.ports.input.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelarReservaCommand(
        @NotBlank(message = "motivo es obligatorio")
        @Size(max = 255, message = "motivo no debe exceder 255 caracteres")
        String motivo
) {
}
