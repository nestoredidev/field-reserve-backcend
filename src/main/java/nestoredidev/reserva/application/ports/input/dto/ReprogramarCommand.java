package nestoredidev.reserva.application.ports.input.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ReprogramarCommand(
        @NotNull(message = "inicio es obligatorio") LocalDateTime inicio,
        @NotNull(message = "fin es obligatorio") LocalDateTime fin
) {
}
