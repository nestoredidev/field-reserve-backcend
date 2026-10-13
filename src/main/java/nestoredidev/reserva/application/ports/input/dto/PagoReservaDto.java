package nestoredidev.reserva.application.ports.input.dto;

import nestoredidev.common.domain.MetodoPago;
import nestoredidev.reserva.domain.model.PagoReserva;
import nestoredidev.reserva.domain.model.TipoPagoReserva;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PagoReservaDto(Long id, Long reservaId, Long cajaId, TipoPagoReserva tipo, MetodoPago metodo,
                             BigDecimal monto, String nroOperacion, String voucherUrl, Long registradoPor,
                             OffsetDateTime fecha) {
    public static PagoReservaDto fromDomain(PagoReserva p) {
        return new PagoReservaDto(p.getId(), p.getReservaId(), p.getCajaId(), p.getTipo(), p.getMetodo(),
                p.getMonto(), p.getNroOperacion(), p.getVoucherUrl(), p.getRegistradoPor(), p.getFecha());
    }
}
