package nestoredidev.bodega.categoria.application.ports.input;

import nestoredidev.bodega.categoria.application.ports.input.dto.CategoriaCommand;
import nestoredidev.bodega.categoria.application.ports.input.dto.CategoriaDto;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;

public interface CategoriaServicePort {

    CategoriaDto crear(CategoriaCommand command);

    CategoriaDto obtener(Long id);

    PageResult<CategoriaDto> listar(PageQuery query);

    CategoriaDto actualizar(Long id, CategoriaCommand command);

    void eliminar(Long id);
}
