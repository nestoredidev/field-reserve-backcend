package nestoredidev.bodega.compra.application.service;

import lombok.RequiredArgsConstructor;
import nestoredidev.bodega.caja.application.ports.input.CajaBodegaServicePort;
import nestoredidev.bodega.compra.application.ports.input.CompraServicePort;
import nestoredidev.bodega.compra.application.ports.input.dto.AnularCommand;
import nestoredidev.bodega.compra.application.ports.input.dto.CompraDto;
import nestoredidev.bodega.compra.application.ports.input.dto.CreateCompraCommand;
import nestoredidev.bodega.compra.application.ports.input.dto.DetalleCompraCommand;
import nestoredidev.bodega.compra.application.ports.output.CompraPersistencePort;
import nestoredidev.bodega.compra.domain.exception.CompraNotFoundException;
import nestoredidev.bodega.compra.domain.model.Compra;
import nestoredidev.bodega.compra.domain.model.DetalleCompra;
import nestoredidev.bodega.compra.domain.model.EstadoCompra;
import nestoredidev.bodega.producto.application.ports.input.InventarioServicePort;
import nestoredidev.bodega.producto.application.ports.input.dto.ProductoDto;
import nestoredidev.bodega.producto.application.ports.input.dto.RegistrarMovimientoCommand;
import nestoredidev.bodega.producto.domain.exception.StockInsuficienteException;
import nestoredidev.bodega.producto.domain.model.TipoMovimientoInventario;
import nestoredidev.bodega.proveedor.application.ports.input.ProveedorServicePort;
import nestoredidev.bodega.proveedor.application.ports.input.dto.ProveedorDto;
import nestoredidev.common.application.CurrentUserPort;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;
import nestoredidev.common.exception.DomainException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompraService implements CompraServicePort {
    private final CompraPersistencePort persistence;
    private final InventarioServicePort inventario;
    private final ProveedorServicePort proveedorService;
    private final CajaBodegaServicePort cajaBodega;
    private final CurrentUserPort currentUser;
    private final Clock clock;

    /** RN-COM-04: sube el stock, escribe el kardex y actualiza el precio de compra, todo en una transacción. */
    @Override
    @Transactional
    public CompraDto crear(CreateCompraCommand cmd) {
        if (cmd.proveedorId() != null) {
            ProveedorDto proveedor = proveedorService.obtener(cmd.proveedorId());
            if (!proveedor.activo()) {
                throw new DomainException("RN-PRV-02: el proveedor '" + proveedor.nombre() + "' está inactivo");
            }
        }
        boolean pagarDesdeCaja = Boolean.TRUE.equals(cmd.pagadoDesdeCaja());
        if (pagarDesdeCaja) {
            cajaBodega.exigirCajaAbiertaId(); // RN-G05: falla antes de tocar el stock
        }

        List<Long> ids = cmd.detalles().stream().map(DetalleCompraCommand::productoId).toList();
        Map<Long, ProductoDto> productos = inventario.obtenerParaOperar(ids).stream()
                .collect(Collectors.toMap(ProductoDto::id, Function.identity()));

        List<DetalleCompra> detalles = cmd.detalles().stream().map(d -> {
            ProductoDto p = productos.get(d.productoId());
            if (!p.activo()) {
                throw new DomainException("RN-PRO-09: el producto '" + p.nombre() + "' está inactivo");
            }
            return DetalleCompra.crear(p.id(), valor(d.cantidadCajas()), valor(d.cantidadSueltas()),
                    p.unidadesPorCaja(), d.costoUnitario());
        }).toList();

        Compra compra = persistence.guardar(
                Compra.crear(cmd.proveedorId(), cmd.nroComprobante(), detalles, currentUser.get().id()));

        for (DetalleCompra d : compra.getDetalles()) {
            inventario.registrarMovimiento(new RegistrarMovimientoCommand(d.getProductoId(),
                    TipoMovimientoInventario.ENTRADA_COMPRA, d.getCantidadUnidades(), compra.getId(), null,
                    "Compra #" + compra.getId()));
            inventario.actualizarPrecioCompra(d.getProductoId(), d.getCostoUnitario()); // RN-PRO-06
        }
        if (pagarDesdeCaja) { // RN-COM-05
            cajaBodega.registrarEgresoEfectivo("Compra #" + compra.getId(), compra.getTotal());
        }
        return CompraDto.fromDomain(compra, true);
    }

    @Override
    @Transactional(readOnly = true)
    public CompraDto obtener(Long id) {
        return CompraDto.fromDomain(persistence.buscarPorId(id)
                .orElseThrow(() -> new CompraNotFoundException(id)), true);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CompraDto> listar(Long proveedorId, EstadoCompra estado, LocalDate desde, LocalDate hasta,
                                        PageQuery query) {
        OffsetDateTime d = desde == null ? null : desde.atStartOfDay(clock.getZone()).toOffsetDateTime();
        OffsetDateTime h = hasta == null ? null : hasta.plusDays(1).atStartOfDay(clock.getZone()).toOffsetDateTime();
        return persistence.listar(proveedorId, estado, d, h, query).map(c -> CompraDto.fromDomain(c, false));
    }

    /** RN-COM-06: resta el stock; si ya se vendió parte de esa mercadería no se anula. */
    @Override
    @Transactional
    public CompraDto anular(Long id, AnularCommand cmd) {
        Compra compra = persistence.buscarPorIdParaActualizar(id).orElseThrow(() -> new CompraNotFoundException(id));
        compra.anular(cmd.motivo());
        inventario.obtenerParaOperar(compra.getDetalles().stream().map(DetalleCompra::getProductoId).toList());
        try {
            for (DetalleCompra d : compra.getDetalles()) {
                inventario.registrarMovimiento(new RegistrarMovimientoCommand(d.getProductoId(),
                        TipoMovimientoInventario.ANULACION_COMPRA, d.getCantidadUnidades(), compra.getId(), null,
                        "Anulación compra #" + id + ": " + cmd.motivo().trim()));
            }
        } catch (StockInsuficienteException e) {
            throw new DomainException("RN-COM-06: no se puede anular, parte de esa mercadería ya se vendió. "
                    + "Corrige con un ajuste de inventario. " + e.getMessage());
        }
        return CompraDto.fromDomain(persistence.guardar(compra), true);
    }

    private static int valor(Integer v) {
        return v == null ? 0 : v;
    }
}
