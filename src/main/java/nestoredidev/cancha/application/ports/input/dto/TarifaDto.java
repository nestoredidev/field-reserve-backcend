package nestoredidev.cancha.application.ports.input.dto;

import nestoredidev.cancha.domain.model.Tarifa;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;

public record TarifaDto(Long id, Long canchaId, Short diaSemana, LocalTime horaInicio, LocalTime horaFin,
                        BigDecimal precioHora, boolean activa, OffsetDateTime createdAt) {
    public static TarifaDto fromDomain(Tarifa t) {
        return new TarifaDto(t.getId(), t.getCanchaId(), t.getDiaSemana(), t.getHoraInicio(), t.getHoraFin(),
                t.getPrecioHora(), t.isActiva(), t.getCreatedAt());
    }
}
