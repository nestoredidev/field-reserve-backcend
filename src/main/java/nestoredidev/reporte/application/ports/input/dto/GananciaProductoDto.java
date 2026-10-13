package nestoredidev.reporte.application.ports.input.dto;

import java.math.BigDecimal;

public record GananciaProductoDto(Long productoId, String nombre, long unidadesVendidas, BigDecimal totalVendido,
                                  BigDecimal costoTotal, BigDecimal ganancia) {
}
