package nestoredidev.reporte.application.ports.input.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** RN-G01: cancha y bodega se muestran lado a lado, pero nunca se mezclan ni se suman entre sí. */
public record ResumenGeneralDto(LocalDate desde, LocalDate hasta, Cancha cancha, Bodega bodega) {

    public record Cancha(BigDecimal cobrosEfectivo, BigDecimal cobrosDigitales, BigDecimal otrosIngresos,
                         BigDecimal egresos, long reservasCompletadas) {
    }

    public record Bodega(BigDecimal ventasTotal, long ventasRegistradas, BigDecimal gananciaBruta,
                         BigDecimal comprasTotal, BigDecimal otrosIngresos, BigDecimal egresos) {
    }
}
