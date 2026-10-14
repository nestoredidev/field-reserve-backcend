package nestoredidev.bodega.venta.application.service;

import nestoredidev.bodega.caja.application.ports.input.CajaBodegaServicePort;
import nestoredidev.bodega.compra.application.ports.input.dto.AnularCommand;
import nestoredidev.bodega.producto.application.ports.input.InventarioServicePort;
import nestoredidev.bodega.producto.application.ports.input.ProductoServicePort;
import nestoredidev.bodega.producto.application.ports.input.dto.MovimientoInventarioDto;
import nestoredidev.bodega.producto.application.ports.input.dto.ProductoDto;
import nestoredidev.bodega.producto.application.ports.input.dto.RegistrarMovimientoCommand;
import nestoredidev.bodega.producto.domain.model.Producto;
import nestoredidev.bodega.producto.domain.model.TipoMovimientoInventario;
import nestoredidev.bodega.producto.domain.model.TipoProducto;
import nestoredidev.bodega.venta.application.ports.input.dto.CreateVentaCommand;
import nestoredidev.bodega.venta.application.ports.input.dto.DetalleVentaCommand;
import nestoredidev.bodega.venta.application.ports.input.dto.PagoVentaCommand;
import nestoredidev.bodega.venta.application.ports.input.dto.VentaDto;
import nestoredidev.bodega.venta.application.ports.output.VentaPersistencePort;
import nestoredidev.bodega.venta.domain.exception.InvalidVentaException;
import nestoredidev.bodega.venta.domain.exception.VentaNotFoundException;
import nestoredidev.bodega.venta.domain.model.DetalleVenta;
import nestoredidev.bodega.venta.domain.model.EstadoVenta;
import nestoredidev.bodega.venta.domain.model.PagoVenta;
import nestoredidev.bodega.venta.domain.model.Venta;
import nestoredidev.common.application.CurrentUser;
import nestoredidev.common.application.CurrentUserPort;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;
import nestoredidev.common.domain.MetodoPago;
import nestoredidev.common.domain.Rol;
import nestoredidev.common.exception.ConflictException;
import nestoredidev.common.exception.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VentaServiceTest {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final PageQuery PAGE = new PageQuery(0, 10, "fecha", "desc");
    private static final Long CAJA = 3L;
    private static final Long ENVASE = 9L;

    @Mock
    private VentaPersistencePort persistence;
    @Mock
    private InventarioServicePort inventario;
    @Mock
    private ProductoServicePort productoService;
    @Mock
    private CajaBodegaServicePort cajaBodega;
    @Mock
    private CurrentUserPort currentUser;

    private VentaService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(OffsetDateTime.parse("2026-10-13T10:00:00-05:00").toInstant(), LIMA);
        service = new VentaService(persistence, inventario, productoService, cajaBodega, currentUser, clock);
        when(currentUser.get()).thenReturn(new CurrentUser(9L, "enc", Rol.ENCARGADO));
        when(cajaBodega.exigirCajaAbiertaId()).thenReturn(CAJA);
        when(persistence.existePagoConOperacion(any(), any())).thenReturn(false);
        when(persistence.guardar(any(Venta.class))).thenAnswer(inv -> {
            Venta v = inv.getArgument(0);
            if (v.getId() != null) {
                return v;
            }
            return Venta.restaurar(200L, v.getCajaId(), OffsetDateTime.now(), v.getTotal(), v.getEstado(), null,
                    v.getRegistradoPor(), v.getDetalles(), v.getPagos());
        });
    }

    private static ProductoDto producto(Long id, String venta, String compra, Long envaseId, boolean activo) {
        return ProductoDto.fromDomain(Producto.restaurar(id, null, "Producto " + id, 1L,
                id.equals(ENVASE) ? TipoProducto.ENVASE : TipoProducto.PRODUCTO, new BigDecimal(compra),
                new BigDecimal(venta), 100, 0, 1, envaseId, activo, OffsetDateTime.now()));
    }

    /** Producto 1 (venta 3.00, costo 2.00) y envase 9 (garantía 1.50, costo 1.00). */
    private void catalogo(Long envaseDeP1) {
        ProductoDto p1 = producto(1L, "3.00", "2.00", envaseDeP1, true);
        ProductoDto env = producto(ENVASE, "1.50", "1.00", null, true);
        when(productoService.obtener(1L)).thenReturn(p1);
        when(productoService.obtener(ENVASE)).thenReturn(env);
        when(inventario.obtenerParaOperar(anyCollection())).thenAnswer(inv -> {
            java.util.Collection<Long> ids = inv.getArgument(0);
            return ids.stream().map(id -> id.equals(1L) ? p1 : env).toList();
        });
    }

    private static PagoVentaCommand efectivo(String monto) {
        return new PagoVentaCommand(MetodoPago.EFECTIVO, new BigDecimal(monto), null);
    }

    private List<RegistrarMovimientoCommand> movimientos(int veces) {
        ArgumentCaptor<RegistrarMovimientoCommand> captor = ArgumentCaptor.forClass(RegistrarMovimientoCommand.class);
        verify(inventario, times(veces)).registrarMovimiento(captor.capture());
        return captor.getAllValues();
    }

    // ---------- crear: caso feliz ----------

    @Test
    void crear_ventaSimple_tomaPrecioYCostoDelSistemaYGrabaSalidaDeKardex() {
        catalogo(null);

        VentaDto dto = service.crear(new CreateVentaCommand(
                List.of(new DetalleVentaCommand(1L, 2, null)), List.of(efectivo("6.00"))));

        assertThat(dto.id()).isEqualTo(200L);
        assertThat(dto.cajaId()).isEqualTo(CAJA);
        assertThat(dto.total()).isEqualByComparingTo("6.00");
        assertThat(dto.estado()).isEqualTo(EstadoVenta.REGISTRADA);
        assertThat(dto.registradoPor()).isEqualTo(9L);
        assertThat(dto.detalles()).singleElement().satisfies(d -> {
            assertThat(d.precioUnitario()).isEqualByComparingTo("3.00");
            assertThat(d.subtotal()).isEqualByComparingTo("6.00");
            assertThat(d.productoNombre()).isEqualTo("Producto 1");
        });
        assertThat(dto.pagos()).singleElement().satisfies(p -> assertThat(p.metodo()).isEqualTo(MetodoPago.EFECTIVO));

        List<RegistrarMovimientoCommand> movs = movimientos(1);
        assertThat(movs.get(0).tipo()).isEqualTo(TipoMovimientoInventario.SALIDA_VENTA);
        assertThat(movs.get(0).productoId()).isEqualTo(1L);
        assertThat(movs.get(0).cantidad()).isEqualTo(2);
        assertThat(movs.get(0).ventaId()).isEqualTo(200L);
        assertThat(movs.get(0).motivo()).isEqualTo("Venta #200");
    }

    @Test
    void crear_encargadoNoVeElCostoUnitario() {
        catalogo(null);

        VentaDto dto = service.crear(new CreateVentaCommand(
                List.of(new DetalleVentaCommand(1L, 1, null)), List.of(efectivo("3.00"))));

        assertThat(dto.detalles().get(0).costoUnitario()).isNull();
    }

    @Test
    void crear_adminVeElCostoUnitario() {
        when(currentUser.get()).thenReturn(new CurrentUser(1L, "admin", Rol.ADMIN));
        catalogo(null);

        VentaDto dto = service.crear(new CreateVentaCommand(
                List.of(new DetalleVentaCommand(1L, 1, null)), List.of(efectivo("3.00"))));

        assertThat(dto.detalles().get(0).costoUnitario()).isEqualByComparingTo("2.00");
    }

    @Test
    void crear_pagoDigitalConNumeroDeOperacion_seRegistra() {
        catalogo(null);

        VentaDto dto = service.crear(new CreateVentaCommand(List.of(new DetalleVentaCommand(1L, 1, null)),
                List.of(new PagoVentaCommand(MetodoPago.YAPE, new BigDecimal("3.00"), " OP-1 "))));

        assertThat(dto.pagos().get(0).nroOperacion()).isEqualTo("OP-1");
        verify(persistence).existePagoConOperacion(MetodoPago.YAPE, "OP-1");
    }

    @Test
    void crear_pagoMixto_sumaDebeIgualarElTotal() {
        catalogo(null);

        VentaDto dto = service.crear(new CreateVentaCommand(List.of(new DetalleVentaCommand(1L, 2, null)),
                List.of(efectivo("2.00"), new PagoVentaCommand(MetodoPago.PLIN, new BigDecimal("4.00"), "P-9"))));

        assertThat(dto.pagos()).hasSize(2);
    }

    // ---------- crear: validaciones ----------

    @Test
    void crear_sinCajaAbierta_propagaLaExcepcionSinTocarStockNiGuardar() {
        doThrow(new DomainException("RN-G05: no hay una caja de bodega abierta")).when(cajaBodega).exigirCajaAbiertaId();

        assertThatThrownBy(() -> service.crear(new CreateVentaCommand(
                List.of(new DetalleVentaCommand(1L, 1, null)), List.of(efectivo("3.00")))))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("RN-G05");

        verify(inventario, never()).obtenerParaOperar(any());
        verify(persistence, never()).guardar(any());
    }

    @Test
    void crear_productoRepetido_lanzaInvalidVentaRNDVE01() {
        catalogo(null);

        assertThatThrownBy(() -> service.crear(new CreateVentaCommand(
                List.of(new DetalleVentaCommand(1L, 1, null), new DetalleVentaCommand(1L, 2, null)),
                List.of(efectivo("9.00")))))
                .isInstanceOf(InvalidVentaException.class)
                .hasMessageContaining("RN-DVE-01");

        verify(persistence, never()).guardar(any());
    }

    @Test
    void crear_productoInactivo_lanzaDomainRNPRO09() {
        ProductoDto inactivo = producto(1L, "3.00", "2.00", null, false);
        when(productoService.obtener(1L)).thenReturn(inactivo);
        when(inventario.obtenerParaOperar(anyCollection())).thenReturn(List.of(inactivo));

        assertThatThrownBy(() -> service.crear(new CreateVentaCommand(
                List.of(new DetalleVentaCommand(1L, 1, null)), List.of(efectivo("3.00")))))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("RN-PRO-09");

        verify(persistence, never()).guardar(any());
    }

    @Test
    void crear_pagosNoSumanElTotal_lanzaInvalidVentaRNVEN03() {
        catalogo(null);

        assertThatThrownBy(() -> service.crear(new CreateVentaCommand(
                List.of(new DetalleVentaCommand(1L, 2, null)), List.of(efectivo("5.00")))))
                .isInstanceOf(InvalidVentaException.class)
                .hasMessageContaining("RN-VEN-03");

        verify(inventario, never()).registrarMovimiento(any());
    }

    @Test
    void crear_pagoDigitalSinNumeroDeOperacion_lanzaInvalidVentaRNPVE03() {
        catalogo(null);

        assertThatThrownBy(() -> service.crear(new CreateVentaCommand(List.of(new DetalleVentaCommand(1L, 1, null)),
                List.of(new PagoVentaCommand(MetodoPago.YAPE, new BigDecimal("3.00"), " ")))))
                .isInstanceOf(InvalidVentaException.class)
                .hasMessageContaining("RN-PVE-03");
    }

    @Test
    void crear_numeroDeOperacionYaRegistrado_lanzaConflictRNPVE03() {
        catalogo(null);
        when(persistence.existePagoConOperacion(MetodoPago.YAPE, "OP-1")).thenReturn(true);

        assertThatThrownBy(() -> service.crear(new CreateVentaCommand(List.of(new DetalleVentaCommand(1L, 1, null)),
                List.of(new PagoVentaCommand(MetodoPago.YAPE, new BigDecimal("3.00"), "OP-1")))))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("RN-PVE-03");

        verify(persistence, never()).guardar(any());
    }

    @Test
    void crear_numeroDeOperacionRepetidoDentroDeLaVenta_lanzaInvalidVentaRNPVE03() {
        catalogo(null);

        assertThatThrownBy(() -> service.crear(new CreateVentaCommand(List.of(new DetalleVentaCommand(1L, 2, null)),
                List.of(new PagoVentaCommand(MetodoPago.YAPE, new BigDecimal("3.00"), "OP-1"),
                        new PagoVentaCommand(MetodoPago.YAPE, new BigDecimal("3.00"), "OP-1")))))
                .isInstanceOf(InvalidVentaException.class)
                .hasMessageContaining("repetido dentro de la venta");
    }

    @Test
    void crear_mismoNumeroDeOperacionEnMetodosDistintos_esValido() {
        catalogo(null);

        VentaDto dto = service.crear(new CreateVentaCommand(List.of(new DetalleVentaCommand(1L, 2, null)),
                List.of(new PagoVentaCommand(MetodoPago.YAPE, new BigDecimal("3.00"), "OP-1"),
                        new PagoVentaCommand(MetodoPago.PLIN, new BigDecimal("3.00"), "OP-1"))));

        assertThat(dto.pagos()).hasSize(2);
    }

    @Test
    void crear_pagoEnEfectivoIgnoraNumeroDeOperacionYNoLoConsulta() {
        catalogo(null);

        VentaDto dto = service.crear(new CreateVentaCommand(List.of(new DetalleVentaCommand(1L, 1, null)),
                List.of(new PagoVentaCommand(MetodoPago.EFECTIVO, new BigDecimal("3.00"), "123"))));

        assertThat(dto.pagos().get(0).nroOperacion()).isNull();
        verify(persistence, never()).existePagoConOperacion(any(), any());
    }

    // ---------- crear: envases retornables ----------

    @Test
    void crear_productoRetornableSinIndicarEnvaseEntregado_lanzaInvalidVentaRNDVE05() {
        catalogo(ENVASE);

        assertThatThrownBy(() -> service.crear(new CreateVentaCommand(
                List.of(new DetalleVentaCommand(1L, 1, null)), List.of(efectivo("3.00")))))
                .isInstanceOf(InvalidVentaException.class)
                .hasMessageContaining("RN-DVE-05");

        verify(persistence, never()).guardar(any());
    }

    @Test
    void crear_envaseNoEntregado_agregaLineaDeGarantiaPorLasUnidadesNoDevueltas() {
        catalogo(ENVASE);

        // 2 x 3.00 + garantía de 2 envases x 1.50 = 9.00
        VentaDto dto = service.crear(new CreateVentaCommand(
                List.of(new DetalleVentaCommand(1L, 2, false)), List.of(efectivo("9.00"))));

        assertThat(dto.total()).isEqualByComparingTo("9.00");
        assertThat(dto.detalles()).extracting(VentaDto.DetalleVentaDto::productoId).containsExactly(1L, ENVASE);
        assertThat(dto.detalles().get(1).cantidad()).isEqualTo(2);
        assertThat(dto.detalles().get(1).precioUnitario()).isEqualByComparingTo("1.50");

        List<RegistrarMovimientoCommand> movs = movimientos(2);
        assertThat(movs).extracting(RegistrarMovimientoCommand::tipo).containsOnly(TipoMovimientoInventario.SALIDA_VENTA);
        assertThat(movs).extracting(RegistrarMovimientoCommand::productoId).containsExactly(1L, ENVASE);
    }

    @Test
    void crear_envaseEntregado_noCobraGarantiaYRegistraDevolucionDeEnvaseDespuesDeLasSalidas() {
        catalogo(ENVASE);

        VentaDto dto = service.crear(new CreateVentaCommand(
                List.of(new DetalleVentaCommand(1L, 2, true)), List.of(efectivo("6.00"))));

        assertThat(dto.total()).isEqualByComparingTo("6.00");
        assertThat(dto.detalles()).extracting(VentaDto.DetalleVentaDto::productoId).containsExactly(1L);

        List<RegistrarMovimientoCommand> movs = movimientos(2);
        assertThat(movs.get(0).tipo()).isEqualTo(TipoMovimientoInventario.SALIDA_VENTA);
        assertThat(movs.get(0).productoId()).isEqualTo(1L);
        assertThat(movs.get(1).tipo()).isEqualTo(TipoMovimientoInventario.DEVOLUCION_ENVASE);
        assertThat(movs.get(1).productoId()).isEqualTo(ENVASE);
        assertThat(movs.get(1).cantidad()).isEqualTo(2);
        assertThat(movs.get(1).ventaId()).isEqualTo(200L);

        // el envase recibido también se bloquea para operar
        ArgumentCaptor<java.util.Collection<Long>> ids = ArgumentCaptor.forClass(java.util.Collection.class);
        verify(inventario).obtenerParaOperar(ids.capture());
        assertThat(Set.copyOf(ids.getValue())).containsExactlyInAnyOrder(1L, ENVASE);
    }

    @Test
    void crear_envaseNoEntregadoYEnvaseVendidoAparte_sumaLasCantidadesEnUnaSolaLinea() {
        catalogo(ENVASE);

        // 1 x 3.00 (garantía 1 x 1.50) + 2 envases sueltos x 1.50 => envase: 3 unidades = 4.50; total 7.50
        VentaDto dto = service.crear(new CreateVentaCommand(
                List.of(new DetalleVentaCommand(1L, 1, false), new DetalleVentaCommand(ENVASE, 2, null)),
                List.of(efectivo("7.50"))));

        assertThat(dto.detalles()).hasSize(2);
        assertThat(dto.detalles().stream().filter(d -> d.productoId().equals(ENVASE)).findFirst().orElseThrow()
                .cantidad()).isEqualTo(3);
    }

    // ---------- obtener / listar ----------

    @Test
    void obtener_inexistente_lanzaNotFound() {
        when(persistence.buscarPorId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(1L)).isInstanceOf(VentaNotFoundException.class);
    }

    @Test
    void obtener_existente_incluyeDetallesYPagos() {
        catalogo(null);
        when(persistence.buscarPorId(1L)).thenReturn(Optional.of(ventaRegistrada(1L)));

        VentaDto dto = service.obtener(1L);

        assertThat(dto.detalles()).hasSize(1);
        assertThat(dto.pagos()).hasSize(1);
    }

    @Test
    void listar_convierteFechasEnZonaLimaYNoIncluyeDetalles() {
        when(persistence.listar(any(), any(), any(), any(), any()))
                .thenReturn(new PageResult<>(List.of(ventaRegistrada(1L)), 0, 10, 1, 1));

        PageResult<VentaDto> res = service.listar(CAJA, EstadoVenta.REGISTRADA, LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 3), PAGE);

        verify(persistence).listar(CAJA, EstadoVenta.REGISTRADA,
                LocalDate.of(2026, 10, 1).atStartOfDay(LIMA).toOffsetDateTime(),
                LocalDate.of(2026, 10, 4).atStartOfDay(LIMA).toOffsetDateTime(), PAGE);
        assertThat(res.content().get(0).detalles()).isNull();
        assertThat(res.content().get(0).pagos()).isNull();
    }

    @Test
    void listar_sinFechas_enviaNulos() {
        when(persistence.listar(any(), any(), any(), any(), any())).thenReturn(new PageResult<>(List.of(), 0, 10, 0, 0));

        service.listar(null, null, null, null, PAGE);

        verify(persistence).listar(null, null, null, null, PAGE);
    }

    // ---------- anular ----------

    private static Venta ventaRegistrada(Long id) {
        return Venta.restaurar(id, CAJA, OffsetDateTime.now(), new BigDecimal("3.00"), EstadoVenta.REGISTRADA, null, 9L,
                List.of(DetalleVenta.restaurar(1L, 1L, 1, new BigDecimal("3.00"), new BigDecimal("2.00"))),
                List.of(PagoVenta.restaurar(1L, MetodoPago.EFECTIVO, new BigDecimal("3.00"), null, OffsetDateTime.now())));
    }

    private static MovimientoInventarioDto mov(Long productoId, TipoMovimientoInventario tipo, int cantidad) {
        return new MovimientoInventarioDto(1L, productoId, tipo, cantidad, 10, 8, null, 1L, null, 9L, OffsetDateTime.now());
    }

    @Test
    void anular_conCajaAbierta_revierteSalidasYEnvasesRecibidos() {
        catalogo(null);
        when(persistence.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(ventaRegistrada(1L)));
        when(cajaBodega.estaAbierta(CAJA)).thenReturn(true);
        when(inventario.movimientosDeVenta(1L)).thenReturn(List.of(
                mov(1L, TipoMovimientoInventario.SALIDA_VENTA, 2),
                mov(ENVASE, TipoMovimientoInventario.DEVOLUCION_ENVASE, 2),
                mov(ENVASE, TipoMovimientoInventario.ANULACION_VENTA, 5))); // otros tipos se ignoran

        VentaDto dto = service.anular(1L, new AnularCommand("  Cliente se arrepintió "));

        assertThat(dto.estado()).isEqualTo(EstadoVenta.ANULADA);
        assertThat(dto.motivoAnulacion()).isEqualTo("Cliente se arrepintió");
        List<RegistrarMovimientoCommand> movs = movimientos(2);
        assertThat(movs.get(0).tipo()).isEqualTo(TipoMovimientoInventario.ANULACION_VENTA);
        assertThat(movs.get(0).productoId()).isEqualTo(1L);
        assertThat(movs.get(0).cantidad()).isEqualTo(2);
        assertThat(movs.get(1).tipo()).isEqualTo(TipoMovimientoInventario.AJUSTE_NEGATIVO);
        assertThat(movs.get(1).productoId()).isEqualTo(ENVASE);
        assertThat(movs.get(0).ventaId()).isEqualTo(1L);
        assertThat(movs.get(0).motivo()).isEqualTo("Anulación venta #1: Cliente se arrepintió");
    }

    @Test
    void anular_cajaCerrada_lanzaDomainRNVEN05SinMoverStock() {
        when(persistence.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(ventaRegistrada(1L)));
        when(cajaBodega.estaAbierta(CAJA)).thenReturn(false);

        assertThatThrownBy(() -> service.anular(1L, new AnularCommand("x")))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("RN-VEN-05");

        verify(inventario, never()).registrarMovimiento(any());
        verify(persistence, never()).guardar(any());
    }

    @Test
    void anular_motivoEnBlanco_lanzaInvalidVentaRNVEN04() {
        when(persistence.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(ventaRegistrada(1L)));

        assertThatThrownBy(() -> service.anular(1L, new AnularCommand("  ")))
                .isInstanceOf(InvalidVentaException.class)
                .hasMessageContaining("RN-VEN-04");
    }

    @Test
    void anular_yaAnulada_lanzaInvalidVenta() {
        Venta anulada = Venta.restaurar(1L, CAJA, OffsetDateTime.now(), new BigDecimal("3.00"), EstadoVenta.ANULADA,
                "x", 9L, List.of(), List.of());
        when(persistence.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(anulada));

        assertThatThrownBy(() -> service.anular(1L, new AnularCommand("otra")))
                .isInstanceOf(InvalidVentaException.class)
                .hasMessageContaining("ya está anulada");
    }

    @Test
    void anular_inexistente_lanzaNotFound() {
        when(persistence.buscarPorIdParaActualizar(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.anular(1L, new AnularCommand("x"))).isInstanceOf(VentaNotFoundException.class);
    }
}
