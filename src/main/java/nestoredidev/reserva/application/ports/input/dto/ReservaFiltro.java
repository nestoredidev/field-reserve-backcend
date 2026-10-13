package nestoredidev.reserva.application.ports.input.dto;

import nestoredidev.reserva.domain.model.EstadoReserva;
import nestoredidev.reserva.domain.model.TipoReserva;

import java.time.LocalDate;

/** Todos los campos son opcionales. desde/hasta son fechas de Lima, ambas inclusive. */
public record ReservaFiltro(Long canchaId, Long clienteId, EstadoReserva estado, TipoReserva tipo,
                            LocalDate desde, LocalDate hasta) {
}
