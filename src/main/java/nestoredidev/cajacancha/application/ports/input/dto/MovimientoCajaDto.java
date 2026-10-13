package nestoredidev.cajacancha.application.ports.input.dto;

import nestoredidev.cajacancha.domain.model.MovimientoCaja;
import nestoredidev.common.domain.MetodoPago;
import nestoredidev.common.domain.TipoMovimientoCaja;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record MovimientoCajaDto(Long id, Long cajaId, TipoMovimientoCaja tipo, MetodoPago metodo, String concepto,
                                BigDecimal monto, Long registradoPor, OffsetDateTime fecha) {
    public static MovimientoCajaDto fromDomain(MovimientoCaja m) {
        return new MovimientoCajaDto(m.getId(), m.getCajaId(), m.getTipo(), m.getMetodo(), m.getConcepto(),
                m.getMonto(), m.getRegistradoPor(), m.getFecha());
    }
}
