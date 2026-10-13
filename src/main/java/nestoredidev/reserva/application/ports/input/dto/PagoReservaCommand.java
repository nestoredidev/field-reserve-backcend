package nestoredidev.reserva.application.ports.input.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import nestoredidev.common.domain.MetodoPago;

import java.math.BigDecimal;

/** El tipo (ADELANTO/SALDO) lo decide el sistema (RN-PAG-04). {@code devolucion=true} solo para ADMIN. */
public record PagoReservaCommand(
        @NotNull(message = "metodo es obligatorio") MetodoPago metodo,

        @NotNull(message = "monto es obligatorio")
        @DecimalMin(value = "0.01", message = "monto debe ser mayor que cero")
        @DecimalMax(value = "99999999.99", message = "monto es demasiado grande")
        BigDecimal monto,

        @Size(max = 30, message = "nroOperacion no debe exceder 30 caracteres")
        String nroOperacion,

        @Size(max = 500, message = "voucherUrl no debe exceder 500 caracteres")
        String voucherUrl,

        Boolean devolucion
) {
    public boolean esDevolucion() {
        return Boolean.TRUE.equals(devolucion);
    }
}
