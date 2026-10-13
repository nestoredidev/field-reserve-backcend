package nestoredidev.cajacancha.application.ports.input.dto;

import nestoredidev.cajacancha.domain.model.CajaCancha;
import nestoredidev.cajacancha.domain.model.CajaResumen;
import nestoredidev.common.domain.EstadoCaja;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/** efectivoEsperado y diferencia solo se muestran al ADMIN (reglas.md: diferencias de caja). */
public record CajaCanchaDto(
        Long id, LocalDate fecha, EstadoCaja estado, BigDecimal montoApertura, BigDecimal montoCierreContado,
        String observacion, Long abiertaPor, Long cerradaPor, OffsetDateTime abiertaAt, OffsetDateTime cerradaAt,
        BigDecimal cobrosEfectivo, BigDecimal cobrosDigitales, BigDecimal otrosIngresosEfectivo,
        BigDecimal egresosEfectivo, BigDecimal efectivoEsperado, BigDecimal diferencia, String advertencia) {

    public static CajaCanchaDto of(CajaCancha c, CajaResumen r, boolean verDiferencias, String advertencia) {
        return new CajaCanchaDto(c.getId(), c.getFecha(), c.getEstado(), c.getMontoApertura(),
                c.getMontoCierreContado(), c.getObservacion(), c.getAbiertaPor(), c.getCerradaPor(),
                c.getAbiertaAt(), c.getCerradaAt(), r.cobrosEfectivo(), r.cobrosDigitales(),
                r.otrosIngresosEfectivo(), r.egresosEfectivo(),
                verDiferencias ? r.efectivoEsperado() : null,
                verDiferencias ? r.diferencia() : null,
                advertencia);
    }
}
