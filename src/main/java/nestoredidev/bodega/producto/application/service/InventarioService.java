package nestoredidev.bodega.producto.application.service;

import lombok.RequiredArgsConstructor;
import nestoredidev.bodega.producto.application.ports.input.InventarioServicePort;
import nestoredidev.bodega.producto.application.ports.input.dto.*;
import nestoredidev.bodega.producto.application.ports.output.ProductoPersistencePort;
import nestoredidev.bodega.producto.domain.exception.InvalidProductoException;
import nestoredidev.bodega.producto.domain.exception.ProductoNotFoundException;
import nestoredidev.bodega.producto.domain.model.MovimientoInventario;
import nestoredidev.bodega.producto.domain.model.Producto;
import nestoredidev.bodega.producto.domain.model.TipoMovimientoInventario;
import nestoredidev.common.application.CurrentUserPort;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

@Service
@RequiredArgsConstructor
public class InventarioService implements InventarioServicePort {
    private final ProductoPersistencePort persistence;
    private final CurrentUserPort currentUser;

    @Override
    @Transactional
    public MovimientoInventarioDto ajustar(AjusteInventarioCommand cmd) {
        if (!cmd.tipo().esManual()) {
            throw new InvalidProductoException("El tipo " + cmd.tipo()
                    + " lo generan las compras y ventas; no se registra a mano");
        }
        return registrarMovimiento(new RegistrarMovimientoCommand(cmd.productoId(), cmd.tipo(), cmd.cantidad(),
                null, null, cmd.motivo()));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<MovimientoInventarioDto> listarMovimientos(Long productoId, TipoMovimientoInventario tipo,
                                                                 PageQuery query) {
        return persistence.listarMovimientos(productoId, tipo, query).map(MovimientoInventarioDto::fromDomain);
    }

    @Override
    @Transactional
    public List<ProductoDto> obtenerParaOperar(Collection<Long> ids) {
        return new TreeSet<>(ids).stream() // orden fijo de bloqueo: evita deadlocks entre ventas concurrentes
                .map(id -> persistence.buscarPorIdParaActualizar(id)
                        .orElseThrow(() -> ProductoNotFoundException.porId(id)))
                .map(ProductoDto::fromDomain)
                .toList();
    }

    /** RN-PRO-02: único camino para cambiar el stock; siempre deja rastro en el kardex. */
    @Override
    @Transactional
    public MovimientoInventarioDto registrarMovimiento(RegistrarMovimientoCommand cmd) {
        Producto producto = persistence.buscarPorIdParaActualizar(cmd.productoId())
                .orElseThrow(() -> ProductoNotFoundException.porId(cmd.productoId()));
        int anterior = producto.aplicarMovimiento(cmd.tipo(), cmd.cantidad());
        persistence.guardarStock(producto);
        String motivo = cmd.motivo() == null || cmd.motivo().isBlank() ? null : cmd.motivo().trim();
        MovimientoInventario guardado = persistence.guardarMovimiento(new MovimientoInventario(null,
                producto.getId(), cmd.tipo(), cmd.cantidad(), anterior, producto.getStock(), cmd.compraId(),
                cmd.ventaId(), motivo, currentUser.get().id(), null));
        return MovimientoInventarioDto.fromDomain(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovimientoInventarioDto> movimientosDeVenta(Long ventaId) {
        return persistence.movimientosDeVenta(ventaId).stream().map(MovimientoInventarioDto::fromDomain).toList();
    }

    @Override
    @Transactional
    public void actualizarPrecioCompra(Long productoId, BigDecimal costoUnitario) {
        Producto producto = persistence.buscarPorIdParaActualizar(productoId)
                .orElseThrow(() -> ProductoNotFoundException.porId(productoId));
        producto.actualizarPrecioCompra(costoUnitario);
        persistence.guardar(producto);
    }
}
