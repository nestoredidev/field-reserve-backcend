package nestoredidev.usuario.application.ports.input.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import nestoredidev.common.domain.Rol;

public record CreateUsuarioCommand(
        @NotBlank(message = "nombre es obligatorio")
        @Size(max = 100, message = "nombre no debe exceder 100 caracteres")
        String nombre,

        @NotBlank(message = "username es obligatorio")
        @Pattern(regexp = "^[a-z0-9._-]{4,50}$",
                message = "username de 4 a 50 caracteres, en minúsculas y sin espacios")
        String username,

        @NotBlank(message = "password es obligatorio")
        @Size(min = 8, max = 72, message = "password debe tener entre 8 y 72 caracteres")
        String password,

        @NotNull(message = "rol es obligatorio")
        Rol rol
) {
}
