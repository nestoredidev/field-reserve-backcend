package nestoredidev.bodega.categoria.application.ports.input.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoriaCommand(
        @NotBlank(message = "nombre es obligatorio")
        @Size(max = 60, message = "nombre no debe exceder 60 caracteres")
        String nombre
) {
}
