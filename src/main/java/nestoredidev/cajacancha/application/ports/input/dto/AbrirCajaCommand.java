package nestoredidev.cajacancha.application.ports.input.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AbrirCajaCommand(
        @NotNull(message = "montoApertura es obligatorio")
        @DecimalMin(value = "0.00", message = "montoApertura no puede ser negativo")
        @DecimalMax(value = "99999999.99", message = "montoApertura es demasiado grande")
        BigDecimal montoApertura
) {
}
