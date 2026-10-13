package nestoredidev.bodega.producto.application.ports.input.dto;

import jakarta.validation.constraints.*;
import nestoredidev.bodega.producto.domain.model.TipoMovimientoInventario;

public record AjusteInventarioCommand(
        @NotNull(message = "productoId es obligatorio") Long productoId,
        @NotNull(message = "tipo es obligatorio") TipoMovimientoInventario tipo,

        @NotNull(message = "cantidad es obligatoria")
        @Min(value = 1, message = "cantidad debe ser un entero mayor que cero")
        @Max(value = 1000000, message = "cantidad es demasiado grande")
        Integer cantidad,

        @NotBlank(message = "motivo es obligatorio")
        @Size(max = 255, message = "motivo no debe exceder 255 caracteres")
        String motivo
) {
}
