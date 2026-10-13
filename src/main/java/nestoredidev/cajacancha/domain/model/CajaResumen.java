package nestoredidev.cajacancha.domain.model;

import java.math.BigDecimal;

/** Totales calculados por la base (vista v_caja_resumen). */
public record CajaResumen(BigDecimal cobrosEfectivo, BigDecimal cobrosDigitales, BigDecimal otrosIngresosEfectivo,
                          BigDecimal egresosEfectivo, BigDecimal efectivoEsperado, BigDecimal diferencia) {
}
