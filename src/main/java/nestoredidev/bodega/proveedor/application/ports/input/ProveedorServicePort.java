package nestoredidev.bodega.proveedor.application.ports.input;

import nestoredidev.bodega.proveedor.application.ports.input.dto.ProveedorCommand;
import nestoredidev.bodega.proveedor.application.ports.input.dto.ProveedorDto;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;

public interface ProveedorServicePort {

    ProveedorDto crear(ProveedorCommand command);

    ProveedorDto obtener(Long id);

    PageResult<ProveedorDto> listar(String q, Boolean activo, PageQuery query);

    ProveedorDto actualizar(Long id, ProveedorCommand command);

    ProveedorDto activar(Long id);

    ProveedorDto desactivar(Long id);
}
