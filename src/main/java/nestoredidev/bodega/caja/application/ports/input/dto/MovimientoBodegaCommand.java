package nestoredidev.bodega.caja.application.ports.input.dto;

import jakarta.validation.constraints.*;
import nestoredidev.common.domain.MetodoPago;
import nestoredidev.common.domain.TipoMovimientoCaja;

import java.math.BigDecimal;

public record MovimientoBodegaCommand(
        @NotNull(message = "tipo es obligatorio") TipoMovimientoCaja tipo,
        @NotNull(message = "metodo es obligatorio") MetodoPago metodo,

        @NotBlank(message = "concepto es obligatorio")
        @Size(max = 150, message = "concepto no debe exceder 150 caracteres")
        String concepto,

        @NotNull(message = "monto es obligatorio")
        @DecimalMin(value = "0.01", message = "monto debe ser mayor que cero")
        @DecimalMax(value = "99999999.99", message = "monto es demasiado grande")
        BigDecimal monto
) {
}
