package nestoredidev.usuario.application.ports.input.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordCommand(
        @NotBlank(message = "password es obligatorio")
        @Size(min = 8, max = 72, message = "password debe tener entre 8 y 72 caracteres")
        String password
) {
}
