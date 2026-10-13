package nestoredidev.bodega.compra.application.ports.input.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/** Unidades totales = cantidadCajas × unidades_por_caja + cantidadSueltas (lo calcula el backend). */
public record DetalleCompraCommand(
        @NotNull(message = "productoId es obligatorio") Long productoId,

        @Min(value = 0, message = "cantidadCajas no puede ser negativa")
        @Max(value = 100000, message = "cantidadCajas es demasiado grande")
        Integer cantidadCajas,

        @Min(value = 0, message = "cantidadSueltas no puede ser negativa")
        @Max(value = 100000, message = "cantidadSueltas es demasiado grande")
        Integer cantidadSueltas,

        @NotNull(message = "costoUnitario es obligatorio")
        @DecimalMin(value = "0.00", message = "costoUnitario no puede ser negativo")
        @DecimalMax(value = "99999999.99", message = "costoUnitario es demasiado grande")
        BigDecimal costoUnitario
) {
}
