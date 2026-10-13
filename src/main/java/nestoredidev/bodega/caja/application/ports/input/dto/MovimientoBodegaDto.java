package nestoredidev.bodega.caja.application.ports.input.dto;

import nestoredidev.bodega.caja.domain.model.MovimientoBodega;
import nestoredidev.common.domain.MetodoPago;
import nestoredidev.common.domain.TipoMovimientoCaja;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record MovimientoBodegaDto(Long id, Long cajaId, TipoMovimientoCaja tipo, MetodoPago metodo, String concepto,
                                BigDecimal monto, Long registradoPor, OffsetDateTime fecha) {
    public static MovimientoBodegaDto fromDomain(MovimientoBodega m) {
        return new MovimientoBodegaDto(m.getId(), m.getCajaId(), m.getTipo(), m.getMetodo(), m.getConcepto(),
                m.getMonto(), m.getRegistradoPor(), m.getFecha());
    }
}
