package nestoredidev.cancha.application.ports.input.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cuerpo para crear y actualizar una cancha. */
public record CanchaCommand(
        @NotBlank(message = "nombre es obligatorio")
        @Size(max = 80, message = "nombre no debe exceder 80 caracteres")
        String nombre,

        @Size(max = 255, message = "descripcion no debe exceder 255 caracteres")
        String descripcion
) {
}
