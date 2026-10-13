package nestoredidev.reporte.application.ports.output;

import nestoredidev.reporte.application.ports.input.dto.GananciaProductoDto;
import nestoredidev.reporte.application.ports.input.dto.ResumenGeneralDto;

import java.time.OffsetDateTime;
import java.util.List;

public interface ReportePersistencePort {

    List<GananciaProductoDto> gananciaPorProducto(OffsetDateTime desde, OffsetDateTime hastaExclusivo);

    ResumenGeneralDto.Cancha resumenCancha(OffsetDateTime desde, OffsetDateTime hastaExclusivo);

    ResumenGeneralDto.Bodega resumenBodega(OffsetDateTime desde, OffsetDateTime hastaExclusivo);
}
