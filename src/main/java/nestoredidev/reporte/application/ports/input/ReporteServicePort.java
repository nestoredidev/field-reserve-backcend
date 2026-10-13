package nestoredidev.reporte.application.ports.input;

import nestoredidev.reporte.application.ports.input.dto.GananciaProductoDto;
import nestoredidev.reporte.application.ports.input.dto.ResumenGeneralDto;

import java.time.LocalDate;
import java.util.List;

public interface ReporteServicePort {

    List<GananciaProductoDto> gananciaPorProducto(LocalDate desde, LocalDate hasta);

    ResumenGeneralDto resumenGeneral(LocalDate desde, LocalDate hasta);
}
