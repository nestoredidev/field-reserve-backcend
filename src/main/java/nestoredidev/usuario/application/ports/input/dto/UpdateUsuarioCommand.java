package nestoredidev.usuario.application.ports.input.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import nestoredidev.common.domain.Rol;

public record UpdateUsuarioCommand(
        @NotBlank(message = "nombre es obligatorio")
        @Size(max = 100, message = "nombre no debe exceder 100 caracteres")
        String nombre,

        @NotNull(message = "rol es obligatorio")
        Rol rol
) {
}
