package nestoredidev.bodega.compra.application.ports.input.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnularCommand(
        @NotBlank(message = "motivo es obligatorio")
        @Size(max = 255, message = "motivo no debe exceder 255 caracteres")
        String motivo
) {
}
