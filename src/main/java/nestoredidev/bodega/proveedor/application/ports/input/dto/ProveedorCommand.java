package nestoredidev.bodega.proveedor.application.ports.input.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProveedorCommand(
        @NotBlank(message = "nombre es obligatorio")
        @Size(max = 120, message = "nombre no debe exceder 120 caracteres")
        String nombre,

        @Pattern(regexp = "^[0-9]{11}$", message = "ruc debe tener 11 dígitos")
        String ruc,

        @Size(max = 15, message = "telefono no debe exceder 15 caracteres")
        String telefono
) {
}
