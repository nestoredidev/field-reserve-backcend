package nestoredidev.bodega.compra.application.ports.input.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/** {@code pagadoDesdeCaja=true}: se registra un EGRESO en efectivo en la caja de la bodega (RN-COM-05). */
public record CreateCompraCommand(
        Long proveedorId,

        @Size(max = 30, message = "nroComprobante no debe exceder 30 caracteres")
        String nroComprobante,

        Boolean pagadoDesdeCaja,

        @NotEmpty(message = "detalles es obligatorio (al menos un producto)")
        @Size(max = 200, message = "detalles no debe exceder 200 líneas")
        List<@Valid DetalleCompraCommand> detalles
) {
}
