package nestoredidev.bodega.producto.application.service;

import nestoredidev.bodega.categoria.application.ports.input.CategoriaServicePort;
import nestoredidev.bodega.categoria.domain.exception.CategoriaNotFoundException;
import nestoredidev.bodega.producto.application.ports.input.InventarioServicePort;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    private static final PageQuery PAGE = new PageQuery(0, 10, "nombre", "asc");

    @Mock
    private ProductoPersistencePort persistence;
    @Mock
    private CategoriaServicePort categoriaService;
    @Mock
    private InventarioServicePort inventarioService;

    @InjectMocks
    private ProductoService service;

    private static Producto producto(Long id, TipoProducto tipo, int stock, Long envaseId) {
        return Producto.restaurar(id, "775" + id, "Producto " + id, 1L, tipo, new BigDecimal("2.00"),
                new BigDecimal("3.50"), stock, 5, 12, envaseId, true, OffsetDateTime.now());
    }

    private static ProductoCommand cmd(String codigo, TipoProducto tipo, Long envaseId, Integer stockInicial) {
        return new ProductoCommand(codigo, "  Cerveza  ", 1L, tipo, new BigDecimal("2"), new BigDecimal("3.5"),
                null, null, envaseId, stockInicial);
    }

    // ---------- crear ----------

    @Test
    void crear_sinTipoNiStockInicial_usaDefaultsYNoRegistraMovimiento() {
        when(persistence.existePorCodigoBarras("123", null)).thenReturn(false);
        when(persistence.guardar(any(Producto.class))).thenAnswer(inv -> {
            Producto p = inv.getArgument(0);
            return Producto.restaurar(10L, p.getCodigoBarras(), p.getNombre(), p.getCategoriaId(), p.getTipo(),
                    p.getPrecioCompra(), p.getPrecioVenta(), p.getStock(), p.getStockMinimo(),
                    p.getUnidadesPorCaja(), p.getEnvaseId(), p.isActivo(), OffsetDateTime.now());
        });

        ProductoDto dto = service.crear(cmd(" 123 ", null, null, null));

        verify(categoriaService).obtener(1L);
        verify(inventarioService, never()).registrarMovimiento(any());
        assertThat(dto.id()).isEqualTo(10L);
        assertThat(dto.codigoBarras()).isEqualTo("123");
        assertThat(dto.nombre()).isEqualTo("Cerveza");
        assertThat(dto.tipo()).isEqualTo(TipoProducto.PRODUCTO);
        assertThat(dto.stock()).isZero();
        assertThat(dto.stockMinimo()).isZero();
        assertThat(dto.unidadesPorCaja()).isEqualTo(1);
        assertThat(dto.activo()).isTrue();
    }

    @Test
    void crear_conStockInicial_registraAjustePositivoYDevuelveProductoRecargado() {
        when(persistence.existePorCodigoBarras("123", null)).thenReturn(false);
        when(persistence.guardar(any(Producto.class))).thenReturn(producto(10L, TipoProducto.PRODUCTO, 0, null));
        when(persistence.buscarPorId(10L)).thenReturn(Optional.of(producto(10L, TipoProducto.PRODUCTO, 24, null)));

        ProductoDto dto = service.crear(cmd("123", null, null, 24));

        ArgumentCaptor<RegistrarMovimientoCommand> captor = ArgumentCaptor.forClass(RegistrarMovimientoCommand.class);
        verify(inventarioService).registrarMovimiento(captor.capture());
        assertThat(captor.getValue().productoId()).isEqualTo(10L);
        assertThat(captor.getValue().tipo()).isEqualTo(TipoMovimientoInventario.AJUSTE_POSITIVO);
        assertThat(captor.getValue().cantidad()).isEqualTo(24);
        assertThat(captor.getValue().motivo()).isEqualTo("Inventario inicial");
        assertThat(dto.stock()).isEqualTo(24);
    }

    @Test
    void crear_stockInicialCero_noRegistraMovimiento() {
        when(persistence.guardar(any(Producto.class))).thenReturn(producto(10L, TipoProducto.PRODUCTO, 0, null));

        service.crear(cmd(null, null, null, 0));

        verify(inventarioService, never()).registrarMovimiento(any());
    }

    @Test
    void crear_categoriaInexistente_propagaNotFoundSinGuardar() {
        doThrow(new CategoriaNotFoundException(1L)).when(categoriaService).obtener(1L);

        assertThatThrownBy(() -> service.crear(cmd("123", null, null, null)))
                .isInstanceOf(CategoriaNotFoundException.class);

        verify(persistence, never()).guardar(any());
    }

    @Test
    void crear_codigoDeBarrasDuplicado_lanzaConflictRNPRO01() {
        when(persistence.existePorCodigoBarras("123", null)).thenReturn(true);

        assertThatThrownBy(() -> service.crear(cmd("123", null, null, null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("RN-PRO-01");

        verify(persistence, never()).guardar(any());
    }

    @Test
    void crear_sinCodigoDeBarras_noValidaUnicidad() {
        when(persistence.guardar(any(Producto.class))).thenReturn(producto(10L, TipoProducto.PRODUCTO, 0, null));

        service.crear(cmd("  ", null, null, null));

        verify(persistence, never()).existePorCodigoBarras(anyString(), any());
    }

    @Test
    void crear_envaseDeTipoEnvase_esValido() {
        when(persistence.buscarPorId(2L)).thenReturn(Optional.of(producto(2L, TipoProducto.ENVASE, 0, null)));
        when(persistence.guardar(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductoDto dto = service.crear(cmd(null, null, 2L, null));

        assertThat(dto.envaseId()).isEqualTo(2L);
    }

    @Test
    void crear_envaseQueNoEsTipoEnvase_lanzaInvalidRNPRO08() {
        when(persistence.buscarPorId(2L)).thenReturn(Optional.of(producto(2L, TipoProducto.PRODUCTO, 0, null)));

        assertThatThrownBy(() -> service.crear(cmd(null, null, 2L, null)))
                .isInstanceOf(InvalidProductoException.class)
                .hasMessageContaining("RN-PRO-08");

        verify(persistence, never()).guardar(any());
    }

    @Test
    void crear_envaseInexistente_lanzaNotFound() {
        when(persistence.buscarPorId(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crear(cmd(null, null, 2L, null)))
                .isInstanceOf(ProductoNotFoundException.class);
    }

    @Test
    void crear_tipoEnvaseConEnvaseAsociado_lanzaInvalidRNPRO08() {
        when(persistence.buscarPorId(2L)).thenReturn(Optional.of(producto(2L, TipoProducto.ENVASE, 0, null)));

        assertThatThrownBy(() -> service.crear(cmd(null, TipoProducto.ENVASE, 2L, null)))
                .isInstanceOf(InvalidProductoException.class)
                .hasMessageContaining("RN-PRO-08");
    }

    @Test
    void crear_precioNegativo_lanzaInvalidProducto() {
        ProductoCommand c = new ProductoCommand(null, "X", 1L, null, new BigDecimal("-1"), BigDecimal.ONE, null,
                null, null, null);

        assertThatThrownBy(() -> service.crear(c)).isInstanceOf(InvalidProductoException.class);
    }

    @Test
    void crear_ventaPorDebajoDelCosto_dtoIncluyeAdvertenciaRNPRO04() {
        when(persistence.guardar(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));
        ProductoCommand c = new ProductoCommand(null, "Oferta", 1L, null, new BigDecimal("5"), new BigDecimal("3"),
                null, null, null, null);

        ProductoDto dto = service.crear(c);

        assertThat(dto.advertencias()).singleElement().asString().contains("RN-PRO-04");
    }

    // ---------- obtener ----------

    @Test
    void obtener_inexistente_lanzaNotFound() {
        when(persistence.buscarPorId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(1L)).isInstanceOf(ProductoNotFoundException.class);
    }

    @Test
    void obtenerPorCodigoBarras_recortaEspacios() {
        when(persistence.buscarPorCodigoBarras("775")).thenReturn(Optional.of(producto(1L, TipoProducto.PRODUCTO, 3, null)));

        assertThat(service.obtenerPorCodigoBarras(" 775 ").id()).isEqualTo(1L);
    }

    @Test
    void obtenerPorCodigoBarras_nulo_buscaCadenaVaciaYLanzaNotFound() {
        when(persistence.buscarPorCodigoBarras("")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerPorCodigoBarras(null)).isInstanceOf(ProductoNotFoundException.class);
    }

    // ---------- listar ----------

    @Test
    void listar_qEnBlanco_seEnviaNuloYSePasanFiltros() {
        when(persistence.listar(null, 1L, TipoProducto.ENVASE, true, PAGE))
                .thenReturn(new PageResult<>(List.of(producto(1L, TipoProducto.ENVASE, 0, null)), 0, 10, 1, 1));

        PageResult<ProductoDto> res = service.listar("  ", 1L, TipoProducto.ENVASE, true, PAGE);

        assertThat(res.content()).extracting(ProductoDto::id).containsExactly(1L);
    }

    // ---------- actualizar ----------

    @Test
    void actualizar_valido_noModificaElStock() {
        when(persistence.buscarPorId(1L)).thenReturn(Optional.of(producto(1L, TipoProducto.PRODUCTO, 40, null)));
        when(persistence.existePorCodigoBarras("999", 1L)).thenReturn(false);
        when(persistence.guardar(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductoDto dto = service.actualizar(1L, cmd("999", null, null, 1000));

        assertThat(dto.stock()).isEqualTo(40);
        assertThat(dto.codigoBarras()).isEqualTo("999");
        // sin stockMinimo/unidadesPorCaja en el comando conserva los actuales
        assertThat(dto.stockMinimo()).isEqualTo(5);
        assertThat(dto.unidadesPorCaja()).isEqualTo(12);
    }

    @Test
    void actualizar_codigoDeOtroProducto_lanzaConflict() {
        when(persistence.buscarPorId(1L)).thenReturn(Optional.of(producto(1L, TipoProducto.PRODUCTO, 0, null)));
        when(persistence.existePorCodigoBarras("999", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.actualizar(1L, cmd("999", null, null, null)))
                .isInstanceOf(ConflictException.class);

        verify(persistence, never()).guardar(any());
    }

    @Test
    void actualizar_envaseUsadoPorOtrosQueCambiaDeTipo_lanzaInvalidRNPRO08() {
        when(persistence.buscarPorId(1L)).thenReturn(Optional.of(producto(1L, TipoProducto.ENVASE, 0, null)));
        when(persistence.esEnvaseDeOtroProducto(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.actualizar(1L, cmd("999", TipoProducto.PRODUCTO, null, null)))
                .isInstanceOf(InvalidProductoException.class)
                .hasMessageContaining("envase de otros productos");

        verify(persistence, never()).guardar(any());
    }

    @Test
    void actualizar_envaseSinUsoQueCambiaDeTipo_esValido() {
        when(persistence.buscarPorId(1L)).thenReturn(Optional.of(producto(1L, TipoProducto.ENVASE, 0, null)));
        when(persistence.esEnvaseDeOtroProducto(1L)).thenReturn(false);
        when(persistence.guardar(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.actualizar(1L, cmd(null, TipoProducto.PRODUCTO, null, null)).tipo())
                .isEqualTo(TipoProducto.PRODUCTO);
    }

    @Test
    void actualizar_envaseQueSigueSiendoEnvase_noConsultaSiEsEnvaseDeOtros() {
        when(persistence.buscarPorId(1L)).thenReturn(Optional.of(producto(1L, TipoProducto.ENVASE, 0, null)));
        when(persistence.guardar(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        service.actualizar(1L, cmd(null, TipoProducto.ENVASE, null, null));

        verify(persistence, never()).esEnvaseDeOtroProducto(any());
    }

    @Test
    void actualizar_productoSiendoSuPropioEnvase_lanzaInvalidProducto() {
        when(persistence.buscarPorId(1L)).thenReturn(Optional.of(producto(1L, TipoProducto.ENVASE, 0, null)));

        assertThatThrownBy(() -> service.actualizar(1L, cmd(null, TipoProducto.PRODUCTO, 1L, null)))
                .isInstanceOf(InvalidProductoException.class);
    }

    @Test
    void actualizar_inexistente_lanzaNotFound() {
        when(persistence.buscarPorId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizar(1L, cmd(null, null, null, null)))
                .isInstanceOf(ProductoNotFoundException.class);
    }

    // ---------- activar / desactivar / stockBajo ----------

    @Test
    void activar_y_desactivar_cambianElEstado() {
        when(persistence.buscarPorId(1L)).thenReturn(Optional.of(producto(1L, TipoProducto.PRODUCTO, 0, null)));
        when(persistence.guardar(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.desactivar(1L).activo()).isFalse();
        assertThat(service.activar(1L).activo()).isTrue();
    }

    @Test
    void stockBajo_mapeaLosProductosDelPuerto() {
        when(persistence.stockBajo()).thenReturn(List.of(producto(1L, TipoProducto.PRODUCTO, 2, null)));

        List<ProductoDto> res = service.stockBajo();

        assertThat(res).hasSize(1);
        assertThat(res.get(0).stockBajo()).isTrue();
    }
}
