package nestoredidev.bodega.venta.application.ports.input;

import nestoredidev.bodega.compra.application.ports.input.dto.AnularCommand;
import nestoredidev.bodega.venta.application.ports.input.dto.CreateVentaCommand;
import nestoredidev.bodega.venta.application.ports.input.dto.VentaDto;
import nestoredidev.bodega.venta.domain.model.EstadoVenta;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;

import java.time.LocalDate;

public interface VentaServicePort {

    VentaDto crear(CreateVentaCommand command);

    VentaDto obtener(Long id);

    PageResult<VentaDto> listar(Long cajaId, EstadoVenta estado, LocalDate desde, LocalDate hasta, PageQuery query);

    VentaDto anular(Long id, AnularCommand command);
}
