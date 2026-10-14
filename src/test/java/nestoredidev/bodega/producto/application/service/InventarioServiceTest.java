package nestoredidev.bodega.producto.application.service;

import nestoredidev.bodega.producto.application.ports.input.dto.AjusteInventarioCommand;
import nestoredidev.bodega.producto.application.ports.input.dto.MovimientoInventarioDto;
import nestoredidev.bodega.producto.application.ports.input.dto.ProductoDto;
import nestoredidev.bodega.producto.application.ports.input.dto.RegistrarMovimientoCommand;
import nestoredidev.bodega.producto.application.ports.output.ProductoPersistencePort;
import nestoredidev.bodega.producto.domain.exception.InvalidProductoException;
import nestoredidev.bodega.producto.domain.exception.ProductoNotFoundException;
import nestoredidev.bodega.producto.domain.exception.StockInsuficienteException;
import nestoredidev.bodega.producto.domain.model.MovimientoInventario;
import nestoredidev.bodega.producto.domain.model.Producto;
import nestoredidev.bodega.producto.domain.model.TipoMovimientoInventario;
import nestoredidev.bodega.producto.domain.model.TipoProducto;
import nestoredidev.common.application.CurrentUser;
import nestoredidev.common.application.CurrentUserPort;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;
import nestoredidev.common.domain.Rol;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventarioServiceTest {

    private static final PageQuery PAGE = new PageQuery(0, 10, "fecha", "desc");

    @Mock
    private ProductoPersistencePort persistence;
    @Mock
    private CurrentUserPort currentUser;

    @InjectMocks
    private InventarioService service;

    private static Producto producto(Long id, int stock) {
        return Producto.restaurar(id, null, "Producto " + id, 1L, TipoProducto.PRODUCTO, new BigDecimal("2.00"),
                new BigDecimal("3.00"), stock, 5, 1, null, true, OffsetDateTime.now());
    }

    private void usuario() {
        when(currentUser.get()).thenReturn(new CurrentUser(9L, "admin", Rol.ADMIN));
    }

    // ---------- registrarMovimiento ----------

    @Test
    void registrarMovimiento_entrada_sumaStockYGrabaKardexConStockAnteriorYResultante() {
        Producto p = producto(1L, 10);
        when(persistence.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(p));
        when(persistence.guardarMovimiento(any(MovimientoInventario.class))).thenAnswer(inv -> inv.getArgument(0));
        usuario();

        MovimientoInventarioDto dto = service.registrarMovimiento(new RegistrarMovimientoCommand(1L,
                TipoMovimientoInventario.ENTRADA_COMPRA, 5, 33L, null, "  Compra  "));

        InOrder orden = inOrder(persistence);
        orden.verify(persistence).guardarStock(p);
        orden.verify(persistence).guardarMovimiento(any(MovimientoInventario.class));
        assertThat(p.getStock()).isEqualTo(15);
        assertThat(dto.stockAnterior()).isEqualTo(10);
        assertThat(dto.stockResultante()).isEqualTo(15);
        assertThat(dto.cantidad()).isEqualTo(5);
        assertThat(dto.compraId()).isEqualTo(33L);
        assertThat(dto.motivo()).isEqualTo("Compra");
        assertThat(dto.registradoPor()).isEqualTo(9L);
    }

    @Test
    void registrarMovimiento_salida_restaStock() {
        Producto p = producto(1L, 10);
        when(persistence.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(p));
        when(persistence.guardarMovimiento(any(MovimientoInventario.class))).thenAnswer(inv -> inv.getArgument(0));
        usuario();

        MovimientoInventarioDto dto = service.registrarMovimiento(new RegistrarMovimientoCommand(1L,
                TipoMovimientoInventario.SALIDA_VENTA, 4, null, 77L, null));

        assertThat(p.getStock()).isEqualTo(6);
        assertThat(dto.stockResultante()).isEqualTo(6);
        assertThat(dto.ventaId()).isEqualTo(77L);
        assertThat(dto.motivo()).isNull();
    }

    @Test
    void registrarMovimiento_motivoEnBlanco_seGuardaNulo() {
        when(persistence.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(producto(1L, 10)));
        when(persistence.guardarMovimiento(any(MovimientoInventario.class))).thenAnswer(inv -> inv.getArgument(0));
        usuario();

        MovimientoInventarioDto dto = service.registrarMovimiento(new RegistrarMovimientoCommand(1L,
                TipoMovimientoInventario.AJUSTE_POSITIVO, 1, null, null, "   "));

        assertThat(dto.motivo()).isNull();
    }

    @Test
    void registrarMovimiento_stockInsuficiente_lanzaYNoGuardaNada() {
        when(persistence.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(producto(1L, 3)));

        assertThatThrownBy(() -> service.registrarMovimiento(new RegistrarMovimientoCommand(1L,
                TipoMovimientoInventario.SALIDA_VENTA, 4, null, 1L, null)))
                .isInstanceOf(StockInsuficienteException.class)
                .hasMessageContaining("RN-PRO-02");

        verify(persistence, never()).guardarStock(any());
        verify(persistence, never()).guardarMovimiento(any());
    }

    @Test
    void registrarMovimiento_cantidadCero_lanzaInvalidProducto() {
        when(persistence.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(producto(1L, 3)));

        assertThatThrownBy(() -> service.registrarMovimiento(new RegistrarMovimientoCommand(1L,
                TipoMovimientoInventario.AJUSTE_POSITIVO, 0, null, null, null)))
                .isInstanceOf(InvalidProductoException.class);
    }

    @Test
    void registrarMovimiento_productoInexistente_lanzaNotFound() {
        when(persistence.buscarPorIdParaActualizar(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrarMovimiento(new RegistrarMovimientoCommand(1L,
                TipoMovimientoInventario.AJUSTE_POSITIVO, 1, null, null, null)))
                .isInstanceOf(ProductoNotFoundException.class);
    }

    // ---------- ajustar ----------

    @Test
    void ajustar_tipoManual_registraElMovimientoConElMotivo() {
        Producto p = producto(1L, 10);
        when(persistence.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(p));
        when(persistence.guardarMovimiento(any(MovimientoInventario.class))).thenAnswer(inv -> inv.getArgument(0));
        usuario();

        MovimientoInventarioDto dto = service.ajustar(
                new AjusteInventarioCommand(1L, TipoMovimientoInventario.MERMA, 2, "Botella rota"));

        assertThat(dto.tipo()).isEqualTo(TipoMovimientoInventario.MERMA);
        assertThat(dto.stockResultante()).isEqualTo(8);
        assertThat(dto.motivo()).isEqualTo("Botella rota");
        assertThat(dto.compraId()).isNull();
        assertThat(dto.ventaId()).isNull();
    }

    @Test
    void ajustar_tiposGeneradosPorComprasYVentas_lanzaInvalidProducto() {
        for (TipoMovimientoInventario tipo : List.of(TipoMovimientoInventario.ENTRADA_COMPRA,
                TipoMovimientoInventario.SALIDA_VENTA, TipoMovimientoInventario.ANULACION_VENTA,
                TipoMovimientoInventario.ANULACION_COMPRA)) {
            assertThatThrownBy(() -> service.ajustar(new AjusteInventarioCommand(1L, tipo, 1, "x")))
                    .as(tipo.name())
                    .isInstanceOf(InvalidProductoException.class);
        }

        verify(persistence, never()).buscarPorIdParaActualizar(any());
    }

    // ---------- obtenerParaOperar ----------

    @Test
    void obtenerParaOperar_bloqueaEnOrdenAscendenteSinRepetidos() {
        when(persistence.buscarPorIdParaActualizar(any())).thenAnswer(inv -> Optional.of(producto(inv.getArgument(0), 5)));

        List<ProductoDto> res = service.obtenerParaOperar(List.of(3L, 1L, 2L, 3L));

        InOrder orden = inOrder(persistence);
        orden.verify(persistence).buscarPorIdParaActualizar(1L);
        orden.verify(persistence).buscarPorIdParaActualizar(2L);
        orden.verify(persistence).buscarPorIdParaActualizar(3L);
        assertThat(res).extracting(ProductoDto::id).containsExactly(1L, 2L, 3L);
    }

    @Test
    void obtenerParaOperar_unoInexistente_lanzaNotFound() {
        when(persistence.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(producto(1L, 5)));
        when(persistence.buscarPorIdParaActualizar(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerParaOperar(List.of(2L, 1L)))
                .isInstanceOf(ProductoNotFoundException.class);
    }

    // ---------- listarMovimientos / movimientosDeVenta ----------

    @Test
    void listarMovimientos_delegaYMapea() {
        MovimientoInventario mov = new MovimientoInventario(1L, 1L, TipoMovimientoInventario.MERMA, 1, 5, 4, null,
                null, "x", 9L, OffsetDateTime.now());
        when(persistence.listarMovimientos(1L, TipoMovimientoInventario.MERMA, PAGE))
                .thenReturn(new PageResult<>(List.of(mov), 0, 10, 1, 1));

        assertThat(service.listarMovimientos(1L, TipoMovimientoInventario.MERMA, PAGE).content())
                .extracting(MovimientoInventarioDto::id).containsExactly(1L);
    }

    @Test
    void movimientosDeVenta_mapeaMovimientos() {
        MovimientoInventario mov = new MovimientoInventario(1L, 1L, TipoMovimientoInventario.SALIDA_VENTA, 1, 5, 4,
                null, 8L, null, 9L, OffsetDateTime.now());
        when(persistence.movimientosDeVenta(8L)).thenReturn(List.of(mov));

        assertThat(service.movimientosDeVenta(8L)).extracting(MovimientoInventarioDto::ventaId).containsExactly(8L);
    }

    // ---------- actualizarPrecioCompra ----------

    @Test
    void actualizarPrecioCompra_valido_guardaProductoConNuevoCosto() {
        Producto p = producto(1L, 5);
        when(persistence.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(p));

        service.actualizarPrecioCompra(1L, new BigDecimal("2.555"));

        ArgumentCaptor<Producto> captor = ArgumentCaptor.forClass(Producto.class);
        verify(persistence).guardar(captor.capture());
        assertThat(captor.getValue().getPrecioCompra()).isEqualByComparingTo("2.56");
    }

    @Test
    void actualizarPrecioCompra_negativo_lanzaInvalidProductoSinGuardar() {
        when(persistence.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(producto(1L, 5)));

        assertThatThrownBy(() -> service.actualizarPrecioCompra(1L, new BigDecimal("-1")))
                .isInstanceOf(InvalidProductoException.class);

        verify(persistence, never()).guardar(any());
    }

    @Test
    void actualizarPrecioCompra_productoInexistente_lanzaNotFound() {
        when(persistence.buscarPorIdParaActualizar(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizarPrecioCompra(1L, BigDecimal.ONE))
                .isInstanceOf(ProductoNotFoundException.class);
    }
}
