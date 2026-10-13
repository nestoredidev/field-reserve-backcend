package nestoredidev.cajacancha.application.ports.input.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CerrarCajaCommand(
        @NotNull(message = "montoCierreContado es obligatorio")
        @DecimalMin(value = "0.00", message = "montoCierreContado no puede ser negativo")
        @DecimalMax(value = "99999999.99", message = "montoCierreContado es demasiado grande")
        BigDecimal montoCierreContado,

        @Size(max = 255, message = "observacion no debe exceder 255 caracteres")
        String observacion
) {
}
