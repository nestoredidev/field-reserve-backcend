package nestoredidev.bodega.venta.application.ports.input.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateVentaCommand(
        @NotEmpty(message = "detalles es obligatorio (al menos un producto)")
        @Size(max = 100, message = "detalles no debe exceder 100 líneas")
        List<@Valid DetalleVentaCommand> detalles,

        @NotEmpty(message = "pagos es obligatorio (al menos un pago)")
        @Size(max = 10, message = "pagos no debe exceder 10 líneas")
        List<@Valid PagoVentaCommand> pagos
) {
}
