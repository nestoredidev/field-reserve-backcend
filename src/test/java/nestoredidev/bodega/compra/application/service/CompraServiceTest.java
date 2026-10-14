package nestoredidev.bodega.compra.application.service;

import nestoredidev.bodega.caja.application.ports.input.CajaBodegaServicePort;
import nestoredidev.bodega.compra.application.ports.input.dto.AnularCommand;
import nestoredidev.bodega.compra.application.ports.input.dto.CompraDto;
import nestoredidev.bodega.compra.application.ports.input.dto.CreateCompraCommand;
import nestoredidev.bodega.compra.application.ports.input.dto.DetalleCompraCommand;
import nestoredidev.bodega.compra.application.ports.output.CompraPersistencePort;
import nestoredidev.bodega.compra.domain.exception.CompraNotFoundException;
import nestoredidev.bodega.compra.domain.exception.InvalidCompraException;
import nestoredidev.bodega.compra.domain.model.Compra;
import nestoredidev.bodega.compra.domain.model.DetalleCompra;
import nestoredidev.bodega.compra.domain.model.EstadoCompra;
import nestoredidev.bodega.producto.application.ports.input.InventarioServicePort;
import nestoredidev.bodega.producto.application.ports.input.dto.ProductoDto;
import nestoredidev.bodega.producto.application.ports.input.dto.RegistrarMovimientoCommand;
import nestoredidev.bodega.producto.domain.exception.StockInsuficienteException;
import nestoredidev.bodega.producto.domain.model.Producto;
import nestoredidev.bodega.producto.domain.model.TipoMovimientoInventario;
import nestoredidev.bodega.producto.domain.model.TipoProducto;
import nestoredidev.bodega.proveedor.application.ports.input.ProveedorServicePort;
import nestoredidev.bodega.proveedor.application.ports.input.dto.ProveedorDto;
import nestoredidev.common.application.CurrentUser;
import nestoredidev.common.application.CurrentUserPort;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;
import nestoredidev.common.domain.Rol;
import nestoredidev.common.exception.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CompraServiceTest {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final PageQuery PAGE = new PageQuery(0, 10, "fecha", "desc");

    @Mock
    private CompraPersistencePort persistence;
    @Mock
    private InventarioServicePort inventario;
    @Mock
    private ProveedorServicePort proveedorService;
    @Mock
    private CajaBodegaServicePort cajaBodega;
    @Mock
    private CurrentUserPort currentUser;

    private CompraService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(OffsetDateTime.parse("2026-10-13T10:00:00-05:00").toInstant(), LIMA);
        service = new CompraService(persistence, inventario, proveedorService, cajaBodega, currentUser, clock);
        when(currentUser.get()).thenReturn(new CurrentUser(9L, "admin", Rol.ADMIN));
        when(persistence.guardar(any(Compra.class))).thenAnswer(inv -> {
            Compra c = inv.getArgument(0);
            if (c.getId() != null) {
                return c;
            }
            return Compra.restaurar(100L, c.getProveedorId(), c.getNroComprobante(), OffsetDateTime.now(),
                    c.getTotal(), c.getEstado(), c.getRegistradoPor(), c.getDetalles());
        });
    }

    private static ProductoDto productoDto(Long id, int unidadesPorCaja, boolean activo) {
        return ProductoDto.fromDomain(Producto.restaurar(id, null, "Producto " + id, 1L, TipoProducto.PRODUCTO,
                new BigDecimal("1.00"), new BigDecimal("2.00"), 0, 0, unidadesPorCaja, null, activo,
                OffsetDateTime.now()));
    }

    private static ProveedorDto proveedor(boolean activo) {
        return new ProveedorDto(5L, "Backus", null, null, activo, OffsetDateTime.now());
    }

    private static DetalleCompraCommand det(Long productoId, Integer cajas, Integer sueltas, String costo) {
        return new DetalleCompraCommand(productoId, cajas, sueltas, new BigDecimal(costo));
    }

    private static Compra compraRegistrada(Long id, Long... productos) {
        List<DetalleCompra> detalles = java.util.Arrays.stream(productos)
                .map(p -> DetalleCompra.restaurar(null, p, 1, 12, new BigDecimal("1.50"))).toList();
        return Compra.restaurar(id, 5L, null, OffsetDateTime.now(), new BigDecimal("18.00") ,
                EstadoCompra.REGISTRADA, 9L, detalles);
    }

    // ---------- crear ----------

    @Test
    void crear_calculaUnidadesTotalesSubeStockYActualizaPrecioDeCompra() {
        when(inventario.obtenerParaOperar(any())).thenReturn(List.of(productoDto(1L, 12, true), productoDto(2L, 6, true)));

        CompraDto dto = service.crear(new CreateCompraCommand(null, " F001-55 ", null, List.of(
                det(1L, 2, 3, "1.50"),    // 2*12 + 3 = 27 unidades -> 40.50
                det(2L, null, 10, "2.00")))); // 10 unidades -> 20.00

        assertThat(dto.id()).isEqualTo(100L);
        assertThat(dto.nroComprobante()).isEqualTo("F001-55");
        assertThat(dto.estado()).isEqualTo(EstadoCompra.REGISTRADA);
        assertThat(dto.total()).isEqualByComparingTo("60.50");
        assertThat(dto.registradoPor()).isEqualTo(9L);
        assertThat(dto.detalles()).extracting(CompraDto.DetalleCompraDto::cantidadUnidades).containsExactly(27, 10);

        ArgumentCaptor<RegistrarMovimientoCommand> mov = ArgumentCaptor.forClass(RegistrarMovimientoCommand.class);
        verify(inventario, org.mockito.Mockito.times(2)).registrarMovimiento(mov.capture());
        assertThat(mov.getAllValues()).extracting(RegistrarMovimientoCommand::tipo)
                .containsOnly(TipoMovimientoInventario.ENTRADA_COMPRA);
        assertThat(mov.getAllValues()).extracting(RegistrarMovimientoCommand::cantidad).containsExactly(27, 10);
        assertThat(mov.getAllValues()).extracting(RegistrarMovimientoCommand::compraId).containsOnly(100L);
        assertThat(mov.getAllValues().get(0).motivo()).isEqualTo("Compra #100");

        verify(inventario).actualizarPrecioCompra(1L, new BigDecimal("1.50"));
        verify(inventario).actualizarPrecioCompra(2L, new BigDecimal("2.00"));
        verify(cajaBodega, never()).registrarEgresoEfectivo(any(), any());
    }

    @Test
    void crear_pagadoDesdeCaja_exigeCajaAbiertaAntesDeTocarStockYRegistraEgresoPorElTotal() {
        when(inventario.obtenerParaOperar(any())).thenReturn(List.of(productoDto(1L, 1, true)));

        service.crear(new CreateCompraCommand(null, null, true, List.of(det(1L, null, 4, "2.50"))));

        InOrder orden = inOrder(cajaBodega, inventario);
        orden.verify(cajaBodega).exigirCajaAbiertaId();
        orden.verify(inventario).obtenerParaOperar(any());
        orden.verify(inventario).registrarMovimiento(any());
        orden.verify(cajaBodega).registrarEgresoEfectivo(eq("Compra #100"), eq(new BigDecimal("10.00")));
    }

    @Test
    void crear_pagadoDesdeCajaSinCajaAbierta_fallaSinTocarElStock() {
        doThrow(new DomainException("RN-G05: no hay una caja de bodega abierta")).when(cajaBodega).exigirCajaAbiertaId();

        assertThatThrownBy(() -> service.crear(
                new CreateCompraCommand(null, null, true, List.of(det(1L, null, 4, "2.50")))))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("RN-G05");

        verify(inventario, never()).obtenerParaOperar(any());
        verify(persistence, never()).guardar(any());
    }

    @Test
    void crear_pagadoDesdeCajaFalse_noTocaLaCaja() {
        when(inventario.obtenerParaOperar(any())).thenReturn(List.of(productoDto(1L, 1, true)));

        service.crear(new CreateCompraCommand(null, null, false, List.of(det(1L, null, 1, "1.00"))));

        verify(cajaBodega, never()).exigirCajaAbiertaId();
    }

    @Test
    void crear_proveedorInactivo_lanzaDomainRNPRV02() {
        when(proveedorService.obtener(5L)).thenReturn(proveedor(false));

        assertThatThrownBy(() -> service.crear(
                new CreateCompraCommand(5L, null, null, List.of(det(1L, null, 1, "1.00")))))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("RN-PRV-02");

        verify(inventario, never()).obtenerParaOperar(any());
    }

    @Test
    void crear_proveedorActivo_guardaConElProveedor() {
        when(proveedorService.obtener(5L)).thenReturn(proveedor(true));
        when(inventario.obtenerParaOperar(any())).thenReturn(List.of(productoDto(1L, 1, true)));

        assertThat(service.crear(new CreateCompraCommand(5L, null, null, List.of(det(1L, null, 1, "1.00"))))
                .proveedorId()).isEqualTo(5L);
    }

    @Test
    void crear_sinProveedor_noConsultaProveedores() {
        when(inventario.obtenerParaOperar(any())).thenReturn(List.of(productoDto(1L, 1, true)));

        service.crear(new CreateCompraCommand(null, null, null, List.of(det(1L, null, 1, "1.00"))));

        verify(proveedorService, never()).obtener(any());
    }

    @Test
    void crear_productoInactivo_lanzaDomainRNPRO09SinGuardar() {
        when(inventario.obtenerParaOperar(any())).thenReturn(List.of(productoDto(1L, 1, false)));

        assertThatThrownBy(() -> service.crear(
                new CreateCompraCommand(null, null, null, List.of(det(1L, null, 1, "1.00")))))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("RN-PRO-09");

        verify(persistence, never()).guardar(any());
        verify(inventario, never()).registrarMovimiento(any());
    }

    @Test
    void crear_productoRepetido_lanzaInvalidCompraRNCOM01() {
        when(inventario.obtenerParaOperar(any())).thenReturn(List.of(productoDto(1L, 1, true)));

        assertThatThrownBy(() -> service.crear(new CreateCompraCommand(null, null, null,
                List.of(det(1L, null, 1, "1.00"), det(1L, null, 2, "1.00")))))
                .isInstanceOf(InvalidCompraException.class)
                .hasMessageContaining("RN-COM-01");
    }

    @Test
    void crear_detalleSinUnidades_lanzaInvalidCompra() {
        when(inventario.obtenerParaOperar(any())).thenReturn(List.of(productoDto(1L, 12, true)));

        assertThatThrownBy(() -> service.crear(
                new CreateCompraCommand(null, null, null, List.of(det(1L, 0, 0, "1.00")))))
                .isInstanceOf(InvalidCompraException.class)
                .hasMessageContaining("mayor que cero");
    }

    @Test
    void crear_costoNegativo_lanzaInvalidCompra() {
        when(inventario.obtenerParaOperar(any())).thenReturn(List.of(productoDto(1L, 1, true)));

        assertThatThrownBy(() -> service.crear(
                new CreateCompraCommand(null, null, null, List.of(det(1L, null, 1, "-1")))))
                .isInstanceOf(InvalidCompraException.class);
    }

    // ---------- obtener ----------

    @Test
    void obtener_existente_incluyeDetalles() {
        when(persistence.buscarPorId(1L)).thenReturn(Optional.of(compraRegistrada(1L, 1L)));

        assertThat(service.obtener(1L).detalles()).hasSize(1);
    }

    @Test
    void obtener_inexistente_lanzaNotFound() {
        when(persistence.buscarPorId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(1L)).isInstanceOf(CompraNotFoundException.class);
    }

    // ---------- listar ----------

    @Test
    void listar_convierteFechasAInicioDeDiaYHastaExclusivoEnZonaLima() {
        when(persistence.listar(any(), any(), any(), any(), any())).thenReturn(
                new PageResult<>(List.of(compraRegistrada(1L, 1L)), 0, 10, 1, 1));

        PageResult<CompraDto> res = service.listar(5L, EstadoCompra.REGISTRADA, LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 3), PAGE);

        verify(persistence).listar(5L, EstadoCompra.REGISTRADA,
                LocalDate.of(2026, 10, 1).atStartOfDay(LIMA).toOffsetDateTime(),
                LocalDate.of(2026, 10, 4).atStartOfDay(LIMA).toOffsetDateTime(), PAGE);
        assertThat(res.content().get(0).detalles()).isNull(); // el listado no incluye detalles
    }

    @Test
    void listar_sinFechas_enviaNulos() {
        when(persistence.listar(any(), any(), any(), any(), any())).thenReturn(new PageResult<>(List.of(), 0, 10, 0, 0));

        service.listar(null, null, null, null, PAGE);

        verify(persistence).listar(null, null, null, null, PAGE);
    }

    // ---------- anular ----------

    @Test
    void anular_valida_restaStockPorCadaDetalleYGuardaAnulada() {
        Compra compra = compraRegistrada(1L, 1L, 2L);
        when(persistence.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(compra));

        CompraDto dto = service.anular(1L, new AnularCommand("  Error de digitación "));

        assertThat(dto.estado()).isEqualTo(EstadoCompra.ANULADA);
        ArgumentCaptor<RegistrarMovimientoCommand> mov = ArgumentCaptor.forClass(RegistrarMovimientoCommand.class);
        verify(inventario, org.mockito.Mockito.times(2)).registrarMovimiento(mov.capture());
        assertThat(mov.getAllValues()).extracting(RegistrarMovimientoCommand::tipo)
                .containsOnly(TipoMovimientoInventario.ANULACION_COMPRA);
        assertThat(mov.getAllValues()).extracting(RegistrarMovimientoCommand::cantidad).containsOnly(12);
        assertThat(mov.getAllValues().get(0).motivo()).isEqualTo("Anulación compra #1: Error de digitación");
        verify(inventario).obtenerParaOperar(List.of(1L, 2L));
    }

    @Test
    void anular_mercaderiaYaVendida_convierteStockInsuficienteEnDomainRNCOM06() {
        when(persistence.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(compraRegistrada(1L, 1L)));
        doThrow(new StockInsuficienteException("Producto 1", 2, 12)).when(inventario)
                .registrarMovimiento(any(RegistrarMovimientoCommand.class));

        assertThatThrownBy(() -> service.anular(1L, new AnularCommand("Error")))
                .isInstanceOf(DomainException.class)
                .isNotInstanceOf(StockInsuficienteException.class)
                .hasMessageContaining("RN-COM-06")
                .hasMessageContaining("ya se vendió");

        verify(persistence, never()).guardar(any());
    }

    @Test
    void anular_yaAnulada_lanzaInvalidCompra() {
        Compra anulada = Compra.restaurar(1L, null, null, OffsetDateTime.now(), BigDecimal.TEN,
                EstadoCompra.ANULADA, 9L, List.of());
        when(persistence.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(anulada));

        assertThatThrownBy(() -> service.anular(1L, new AnularCommand("x")))
                .isInstanceOf(InvalidCompraException.class)
                .hasMessageContaining("ya está anulada");

        verify(inventario, never()).registrarMovimiento(any());
    }

    @Test
    void anular_motivoEnBlanco_lanzaInvalidCompraRNCOM06() {
        when(persistence.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(compraRegistrada(1L, 1L)));

        assertThatThrownBy(() -> service.anular(1L, new AnularCommand("  ")))
                .isInstanceOf(InvalidCompraException.class)
                .hasMessageContaining("RN-COM-06");

        verify(inventario, never()).registrarMovimiento(any());
    }

    @Test
    void anular_inexistente_lanzaNotFound() {
        when(persistence.buscarPorIdParaActualizar(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.anular(1L, new AnularCommand("x")))
                .isInstanceOf(CompraNotFoundException.class);
    }
}
