package nestoredidev.bodega.producto.application.ports.input;

import nestoredidev.bodega.producto.application.ports.input.dto.ProductoCommand;
import nestoredidev.bodega.producto.application.ports.input.dto.ProductoDto;
import nestoredidev.bodega.producto.domain.model.TipoProducto;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;

import java.util.List;

public interface ProductoServicePort {

    ProductoDto crear(ProductoCommand command);

    ProductoDto obtener(Long id);

    ProductoDto obtenerPorCodigoBarras(String codigoBarras);

    PageResult<ProductoDto> listar(String q, Long categoriaId, TipoProducto tipo, Boolean activo, PageQuery query);

    ProductoDto actualizar(Long id, ProductoCommand command);

    ProductoDto activar(Long id);

    ProductoDto desactivar(Long id);

    /** RN-PRO-10: productos activos con stock <= stock mínimo. */
    List<ProductoDto> stockBajo();
}
