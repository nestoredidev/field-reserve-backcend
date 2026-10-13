package nestoredidev.reserva.application.ports.input.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import nestoredidev.reserva.domain.model.OrigenReserva;

import java.time.LocalDateTime;

/**
 * Hora local de Lima. Cliente: o bien {@code clienteId}, o bien {@code clienteNombre} + {@code clienteTelefono}
 * (se reutiliza el cliente si el teléfono ya existe, RN-CLI-02).
 */
public record CreateReservaCommand(
        @NotNull(message = "canchaId es obligatorio") Long canchaId,
        Long clienteId,

        @Size(max = 100, message = "clienteNombre no debe exceder 100 caracteres")
        String clienteNombre,

        @Pattern(regexp = "^9\\d{8}$", message = "clienteTelefono debe ser un celular peruano de 9 dígitos que empieza con 9")
        String clienteTelefono,

        @NotNull(message = "inicio es obligatorio") LocalDateTime inicio,
        @NotNull(message = "fin es obligatorio") LocalDateTime fin,
        OrigenReserva origen,

        @Size(max = 255, message = "observacion no debe exceder 255 caracteres")
        String observacion,

        @Valid PagoReservaCommand pagoInicial
) {
}
