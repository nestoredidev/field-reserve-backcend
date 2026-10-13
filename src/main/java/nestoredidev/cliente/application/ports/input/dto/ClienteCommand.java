package nestoredidev.cliente.application.ports.input.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Cuerpo para crear y actualizar un cliente. */
public record ClienteCommand(
        @NotBlank(message = "nombre es obligatorio")
        @Size(max = 100, message = "nombre no debe exceder 100 caracteres")
        String nombre,

        @NotBlank(message = "telefono es obligatorio")
        @Pattern(regexp = "^9\\d{8}$", message = "telefono debe ser un celular peruano de 9 dígitos que empieza con 9")
        String telefono,

        @Size(max = 255, message = "notas no debe exceder 255 caracteres")
        String notas
) {
}
