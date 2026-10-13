package nestoredidev.bodega.venta.application.ports.input.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * El precio no se envía: lo toma el sistema (RN-DVE-03). {@code envaseEntregado} es obligatorio solo si el
 * producto es retornable (RN-DVE-05).
 */
public record DetalleVentaCommand(
        @NotNull(message = "productoId es obligatorio") Long productoId,

        @NotNull(message = "cantidad es obligatoria")
        @Min(value = 1, message = "cantidad debe ser un entero mayor que cero")
        @Max(value = 100000, message = "cantidad es demasiado grande")
        Integer cantidad,

        Boolean envaseEntregado
) {
}
