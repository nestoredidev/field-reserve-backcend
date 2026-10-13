package nestoredidev.bodega.venta.application.ports.input.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import nestoredidev.common.domain.MetodoPago;

import java.math.BigDecimal;

public record PagoVentaCommand(
        @NotNull(message = "metodo es obligatorio") MetodoPago metodo,

        @NotNull(message = "monto es obligatorio")
        @DecimalMin(value = "0.01", message = "monto debe ser mayor que cero")
        @DecimalMax(value = "99999999.99", message = "monto es demasiado grande")
        BigDecimal monto,

        @Size(max = 30, message = "nroOperacion no debe exceder 30 caracteres")
        String nroOperacion
) {
}
