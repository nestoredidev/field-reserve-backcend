package nestoredidev.bodega.producto.application.service;

import lombok.RequiredArgsConstructor;
import nestoredidev.bodega.categoria.application.ports.input.CategoriaServicePort;
import nestoredidev.bodega.producto.application.ports.input.InventarioServicePort;
import nestoredidev.bodega.producto.application.ports.input.ProductoServicePort;
import nestoredidev.bodega.producto.application.ports.input.dto.ProductoCommand;
import nestoredidev.bodega.producto.application.ports.input.dto.ProductoDto;
import nestoredidev.bodega.producto.application.ports.input.dto.RegistrarMovimientoCommand;
import nestoredidev.bodega.producto.application.ports.output.ProductoPersistencePort;
import nestoredidev.bodega.producto.domain.exception.InvalidProductoException;
import nestoredidev.bodega.producto.domain.exception.ProductoNotFoundException;
import nestoredidev.bodega.producto.domain.model.Producto;
import nestoredidev.bodega.producto.domain.model.TipoMovimientoInventario;
import nestoredidev.bodega.producto.domain.model.TipoProducto;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;
import nestoredidev.common.exception.ConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductoService implements ProductoServicePort {
    private final ProductoPersistencePort persistence;
    private final CategoriaServicePort categoriaService;
    private final InventarioServicePort inventarioService;

    @Override
    @Transactional
    public ProductoDto crear(ProductoCommand cmd) {
        categoriaService.obtener(cmd.categoriaId()); // 404 si no existe
        validarEnvase(cmd.envaseId());
        Producto nuevo = Producto.crear(cmd.codigoBarras(), cmd.nombre(), cmd.categoriaId(), tipo(cmd),
                cmd.precioCompra(), cmd.precioVenta(), valor(cmd.stockMinimo(), 0), valor(cmd.unidadesPorCaja(), 1),
                cmd.envaseId());
        validarCodigoUnico(nuevo.getCodigoBarras(), null);
        Producto guardado = persistence.guardar(nuevo);

        int stockInicial = valor(cmd.stockInicial(), 0);
        if (stockInicial > 0) { // RN-PRO-03
            inventarioService.registrarMovimiento(new RegistrarMovimientoCommand(guardado.getId(),
                    TipoMovimientoInventario.AJUSTE_POSITIVO, stockInicial, null, null, "Inventario inicial"));
            guardado = buscar(guardado.getId());
        }
        return ProductoDto.fromDomain(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoDto obtener(Long id) {
        return ProductoDto.fromDomain(buscar(id));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoDto obtenerPorCodigoBarras(String codigoBarras) {
        return persistence.buscarPorCodigoBarras(codigoBarras == null ? "" : codigoBarras.trim())
                .map(ProductoDto::fromDomain)
                .orElseThrow(() -> new ProductoNotFoundException(
                        "No existe un producto con código de barras " + codigoBarras));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ProductoDto> listar(String q, Long categoriaId, TipoProducto tipo, Boolean activo,
                                          PageQuery query) {
        return persistence.listar(q == null || q.isBlank() ? null : q.trim(), categoriaId, tipo, activo, query)
                .map(ProductoDto::fromDomain);
    }

    @Override
    @Transactional
    public ProductoDto actualizar(Long id, ProductoCommand cmd) {
        Producto actual = buscar(id);
        categoriaService.obtener(cmd.categoriaId());
        validarEnvase(cmd.envaseId());
        TipoProducto nuevoTipo = tipo(cmd);
        if (actual.getTipo() == TipoProducto.ENVASE && nuevoTipo != TipoProducto.ENVASE
                && persistence.esEnvaseDeOtroProducto(id)) {
            throw new InvalidProductoException("RN-PRO-08: el producto es envase de otros productos; no puede cambiar de tipo");
        }
        Producto actualizado = actual.actualizar(cmd.codigoBarras(), cmd.nombre(), cmd.categoriaId(), nuevoTipo,
                cmd.precioCompra(), cmd.precioVenta(), valor(cmd.stockMinimo(), actual.getStockMinimo()),
                valor(cmd.unidadesPorCaja(), actual.getUnidadesPorCaja()), cmd.envaseId());
        validarCodigoUnico(actualizado.getCodigoBarras(), id);
        return ProductoDto.fromDomain(persistence.guardar(actualizado));
    }

    @Override
    @Transactional
    public ProductoDto activar(Long id) {
        return ProductoDto.fromDomain(persistence.guardar(buscar(id).conActivo(true)));
    }

    @Override
    @Transactional
    public ProductoDto desactivar(Long id) {
        return ProductoDto.fromDomain(persistence.guardar(buscar(id).conActivo(false)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoDto> stockBajo() {
        return persistence.stockBajo().stream().map(ProductoDto::fromDomain).toList();
    }

    private Producto buscar(Long id) {
        return persistence.buscarPorId(id).orElseThrow(() -> ProductoNotFoundException.porId(id));
    }

    /** RN-PRO-08: el envase debe existir y ser de tipo ENVASE. */
    private void validarEnvase(Long envaseId) {
        if (envaseId == null) {
            return;
        }
        Producto envase = buscar(envaseId);
        if (envase.getTipo() != TipoProducto.ENVASE) {
            throw new InvalidProductoException("RN-PRO-08: envaseId debe apuntar a un producto de tipo ENVASE");
        }
    }

    private void validarCodigoUnico(String codigo, Long excluirId) {
        if (codigo != null && persistence.existePorCodigoBarras(codigo, excluirId)) {
            throw new ConflictException("RN-PRO-01: ya existe un producto con el código de barras " + codigo);
        }
    }

    private static TipoProducto tipo(ProductoCommand cmd) {
        return cmd.tipo() == null ? TipoProducto.PRODUCTO : cmd.tipo();
    }

    private static int valor(Integer v, int defecto) {
        return v == null ? defecto : v;
    }
}
