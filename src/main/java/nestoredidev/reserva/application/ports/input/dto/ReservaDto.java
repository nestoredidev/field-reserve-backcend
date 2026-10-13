package nestoredidev.reserva.application.ports.input.dto;

import nestoredidev.reserva.domain.model.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record ReservaDto(
        Long id, Long canchaId, Long clienteId, String clienteNombre, String clienteTelefono, TipoReserva tipo,
        EstadoReserva estado, OffsetDateTime inicio, OffsetDateTime fin, BigDecimal montoTotal, BigDecimal pagado,
        BigDecimal saldo, BigDecimal adelantoMinimo, OffsetDateTime fechaLimiteAdelanto, OrigenReserva origen,
        String observacion, String motivoCancelacion, Long creadoPor, OffsetDateTime createdAt,
        List<String> advertencias, BigDecimal devolucionSugerida) {

    public static ReservaDto of(Reserva r, BigDecimal pagado, BigDecimal adelantoMinimo, String clienteNombre,
                                String clienteTelefono, List<String> advertencias, BigDecimal devolucionSugerida) {
        return new ReservaDto(r.getId(), r.getCanchaId(), r.getClienteId(), clienteNombre, clienteTelefono,
                r.getTipo(), r.getEstado(), r.getInicio(), r.getFin(), r.getMontoTotal(), pagado,
                r.getMontoTotal().subtract(pagado), adelantoMinimo, r.getFechaLimiteAdelanto(), r.getOrigen(),
                r.getObservacion(), r.getMotivoCancelacion(), r.getCreadoPor(), r.getCreatedAt(),
                advertencias == null || advertencias.isEmpty() ? null : advertencias, devolucionSugerida);
    }
}
