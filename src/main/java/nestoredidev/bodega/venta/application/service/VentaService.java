package nestoredidev.bodega.venta.application.service;

import lombok.RequiredArgsConstructor;
import nestoredidev.bodega.caja.application.ports.input.CajaBodegaServicePort;
import nestoredidev.bodega.compra.application.ports.input.dto.AnularCommand;
import nestoredidev.bodega.producto.application.ports.input.InventarioServicePort;
import nestoredidev.bodega.producto.application.ports.input.ProductoServicePort;
import nestoredidev.bodega.producto.application.ports.input.dto.MovimientoInventarioDto;
import nestoredidev.bodega.producto.application.ports.input.dto.ProductoDto;
import nestoredidev.bodega.producto.application.ports.input.dto.RegistrarMovimientoCommand;
import nestoredidev.bodega.producto.domain.model.TipoMovimientoInventario;
import nestoredidev.bodega.venta.application.ports.input.VentaServicePort;
import nestoredidev.bodega.venta.application.ports.input.dto.CreateVentaCommand;
import nestoredidev.bodega.venta.application.ports.input.dto.DetalleVentaCommand;
import nestoredidev.bodega.venta.application.ports.input.dto.PagoVentaCommand;
import nestoredidev.bodega.venta.application.ports.input.dto.VentaDto;
import nestoredidev.bodega.venta.application.ports.output.VentaPersistencePort;
import nestoredidev.bodega.venta.domain.exception.InvalidVentaException;
import nestoredidev.bodega.venta.domain.exception.VentaNotFoundException;
import nestoredidev.bodega.venta.domain.model.*;
import nestoredidev.common.application.CurrentUser;
import nestoredidev.common.application.CurrentUserPort;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;
import nestoredidev.common.exception.ConflictException;
import nestoredidev.common.exception.DomainException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VentaService implements VentaServicePort {
    private final VentaPersistencePort persistence;
    private final InventarioServicePort inventario;
    private final ProductoServicePort productoService;
    private final CajaBodegaServicePort cajaBodega;
    private final CurrentUserPort currentUser;
    private final Clock clock;

    /**
     * RN-G08: venta + stock + kardex + pagos en una sola transacción. Si una línea no alcanza de stock
     * (RN-DVE-02) se lanza la excepción y no se guarda nada.
     */
    @Override
    @Transactional
    public VentaDto crear(CreateVentaCommand cmd) {
        Long cajaId = cajaBodega.exigirCajaAbiertaId(); // RN-VEN-01 / RN-G05

        // 1) Líneas pedidas (sin repetidos) y envases retornables (RN-DVE-05)
        Map<Long, Integer> lineas = new LinkedHashMap<>();
        for (DetalleVentaCommand d : cmd.detalles()) {
            if (lineas.putIfAbsent(d.productoId(), d.cantidad()) != null) {
                throw new InvalidVentaException("RN-DVE-01: el producto " + d.productoId()
                        + " aparece más de una vez en la venta");
            }
        }
        Map<Long, Integer> envasesCobrados = new LinkedHashMap<>();
        Map<Long, Integer> envasesRecibidos = new LinkedHashMap<>();
        for (DetalleVentaCommand d : cmd.detalles()) {
            ProductoDto p = productoService.obtener(d.productoId());
            if (p.envaseId() == null) {
                continue;
            }
            if (d.envaseEntregado() == null) {
                throw new InvalidVentaException("RN-DVE-05: indica si el cliente entregó el envase vacío de '"
                        + p.nombre() + "' (envaseEntregado)");
            }
            (d.envaseEntregado() ? envasesRecibidos : envasesCobrados).merge(p.envaseId(), d.cantidad(), Integer::sum);
        }

        // 2) Línea final = pedidas + garantía de envases no devueltos
        Map<Long, Integer> finales = new LinkedHashMap<>(lineas);
        envasesCobrados.forEach((envaseId, cant) -> finales.merge(envaseId, cant, Integer::sum));

        // 3) Bloquea productos en orden fijo y arma los detalles con precio/costo del sistema
        Set<Long> ids = new TreeSet<>(finales.keySet());
        ids.addAll(envasesRecibidos.keySet());
        Map<Long, ProductoDto> productos = inventario.obtenerParaOperar(ids).stream()
                .collect(Collectors.toMap(ProductoDto::id, Function.identity()));
        List<DetalleVenta> detalles = finales.entrySet().stream().map(e -> {
            ProductoDto p = productos.get(e.getKey());
            if (!p.activo()) {
                throw new DomainException("RN-PRO-09: el producto '" + p.nombre() + "' está inactivo");
            }
            return DetalleVenta.crear(p.id(), e.getValue(), p.precioVenta(), p.precioCompra());
        }).toList();

        // 4) Pagos
        List<PagoVenta> pagos = cmd.pagos().stream()
                .map(p -> PagoVenta.crear(p.metodo(), p.monto(), p.nroOperacion())).toList();
        for (PagoVenta p : pagos) {
            if (p.getNroOperacion() != null && persistence.existePagoConOperacion(p.getMetodo(), p.getNroOperacion())) {
                throw new ConflictException("RN-PVE-03: el número de operación " + p.getNroOperacion()
                        + " ya fue registrado con " + p.getMetodo());
            }
        }
        Set<String> operaciones = new HashSet<>();
        for (PagoVenta p : pagos) {
            if (p.getNroOperacion() != null && !operaciones.add(p.getMetodo() + ":" + p.getNroOperacion())) {
                throw new InvalidVentaException("RN-PVE-03: número de operación repetido dentro de la venta");
            }
        }

        Venta venta = persistence.guardar(Venta.crear(cajaId, detalles, pagos, currentUser.get().id()));

        // 5) Kardex: salidas primero, luego los envases recibidos (RN-DVE-04)
        for (DetalleVenta d : venta.getDetalles()) {
            inventario.registrarMovimiento(new RegistrarMovimientoCommand(d.getProductoId(),
                    TipoMovimientoInventario.SALIDA_VENTA, d.getCantidad(), null, venta.getId(),
                    "Venta #" + venta.getId()));
        }
        envasesRecibidos.forEach((envaseId, cant) -> inventario.registrarMovimiento(new RegistrarMovimientoCommand(
                envaseId, TipoMovimientoInventario.DEVOLUCION_ENVASE, cant, null, venta.getId(),
                "Envase vacío recibido en venta #" + venta.getId())));
        return toDto(venta, true);
    }

    @Override
    @Transactional(readOnly = true)
    public VentaDto obtener(Long id) {
        return toDto(persistence.buscarPorId(id).orElseThrow(() -> new VentaNotFoundException(id)), true);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<VentaDto> listar(Long cajaId, EstadoVenta estado, LocalDate desde, LocalDate hasta,
                                       PageQuery query) {
        OffsetDateTime d = desde == null ? null : desde.atStartOfDay(clock.getZone()).toOffsetDateTime();
        OffsetDateTime h = hasta == null ? null : hasta.plusDays(1).atStartOfDay(clock.getZone()).toOffsetDateTime();
        return persistence.listar(cajaId, estado, d, h, query).map(v -> toDto(v, false));
    }

    /** RN-VEN-04 / RN-VEN-05 */
    @Override
    @Transactional
    public VentaDto anular(Long id, AnularCommand cmd) {
        Venta venta = persistence.buscarPorIdParaActualizar(id).orElseThrow(() -> new VentaNotFoundException(id));
        venta.anular(cmd.motivo());
        if (!cajaBodega.estaAbierta(venta.getCajaId())) {
            throw new DomainException("RN-VEN-05: la caja de esta venta ya está cerrada; el reembolso se registra "
                    + "como EGRESO en la caja actual y el producto devuelto como AJUSTE_POSITIVO");
        }
        List<MovimientoInventarioDto> movimientos = inventario.movimientosDeVenta(id);
        inventario.obtenerParaOperar(movimientos.stream().map(MovimientoInventarioDto::productoId).toList());
        String motivo = "Anulación venta #" + id + ": " + cmd.motivo().trim();
        for (MovimientoInventarioDto m : movimientos) {
            TipoMovimientoInventario inverso = switch (m.tipo()) {
                case SALIDA_VENTA -> TipoMovimientoInventario.ANULACION_VENTA;
                case DEVOLUCION_ENVASE -> TipoMovimientoInventario.AJUSTE_NEGATIVO;
                default -> null;
            };
            if (inverso != null) {
                inventario.registrarMovimiento(new RegistrarMovimientoCommand(m.productoId(), inverso, m.cantidad(),
                        null, id, motivo));
            }
        }
        return toDto(persistence.guardar(venta), true);
    }

    private VentaDto toDto(Venta v, boolean conDetalle) {
        boolean admin = currentUser.get().isAdmin();
        List<VentaDto.DetalleVentaDto> detalles = null;
        List<VentaDto.PagoVentaDto> pagos = null;
        if (conDetalle) {
            Map<Long, String> nombres = new HashMap<>();
            detalles = v.getDetalles().stream().map(d -> new VentaDto.DetalleVentaDto(d.getId(), d.getProductoId(),
                    nombres.computeIfAbsent(d.getProductoId(), pid -> productoService.obtener(pid).nombre()),
                    d.getCantidad(), d.getPrecioUnitario(), admin ? d.getCostoUnitario() : null, d.subtotal())).toList();
            pagos = v.getPagos().stream().map(p -> new VentaDto.PagoVentaDto(p.getId(), p.getMetodo(), p.getMonto(),
                    p.getNroOperacion(), p.getFecha())).toList();
        }
        return new VentaDto(v.getId(), v.getCajaId(), v.getFecha(), v.getTotal(), v.getEstado(),
                v.getMotivoAnulacion(), v.getRegistradoPor(), detalles, pagos);
    }
}
