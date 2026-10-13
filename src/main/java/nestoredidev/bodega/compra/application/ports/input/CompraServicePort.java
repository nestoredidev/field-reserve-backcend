package nestoredidev.bodega.compra.application.ports.input;

import nestoredidev.bodega.compra.application.ports.input.dto.AnularCommand;
import nestoredidev.bodega.compra.application.ports.input.dto.CompraDto;
import nestoredidev.bodega.compra.application.ports.input.dto.CreateCompraCommand;
import nestoredidev.bodega.compra.domain.model.EstadoCompra;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;

import java.time.LocalDate;

public interface CompraServicePort {

    CompraDto crear(CreateCompraCommand command);

    CompraDto obtener(Long id);

    PageResult<CompraDto> listar(Long proveedorId, EstadoCompra estado, LocalDate desde, LocalDate hasta,
                                 PageQuery query);

    CompraDto anular(Long id, AnularCommand command);
}
