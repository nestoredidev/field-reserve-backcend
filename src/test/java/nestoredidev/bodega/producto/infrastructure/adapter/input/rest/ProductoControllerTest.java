package nestoredidev.bodega.producto.infrastructure.adapter.input.rest;

import nestoredidev.bodega.producto.application.ports.input.InventarioServicePort;
import nestoredidev.bodega.producto.application.ports.input.ProductoServicePort;
import nestoredidev.bodega.producto.application.ports.input.dto.AjusteInventarioCommand;
import nestoredidev.bodega.producto.application.ports.input.dto.MovimientoInventarioDto;
import nestoredidev.bodega.producto.application.ports.input.dto.ProductoCommand;
import nestoredidev.bodega.producto.application.ports.input.dto.ProductoDto;
import nestoredidev.bodega.producto.domain.exception.InvalidProductoException;
import nestoredidev.bodega.producto.domain.exception.ProductoNotFoundException;
import nestoredidev.bodega.producto.domain.exception.StockInsuficienteException;
import nestoredidev.bodega.producto.domain.model.TipoMovimientoInventario;
import nestoredidev.bodega.producto.domain.model.TipoProducto;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;
import nestoredidev.common.exception.ConflictException;
import nestoredidev.support.AbstractControllerTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductoController.class)
class ProductoControllerTest extends AbstractControllerTest {

    private static final String BASE = "/api/v2/bodega";
    private static final String JSON = """
            {"codigoBarras":"775001","nombre":"Cristal","categoriaId":1,"tipo":"PRODUCTO","precioCompra":2,
             "precioVenta":3.5,"stockMinimo":5,"unidadesPorCaja":12,"stockInicial":24}""";

    @MockitoBean
    private ProductoServicePort productoService;
    @MockitoBean
    private InventarioServicePort inventarioService;

    private static ProductoDto producto(Long id, boolean activo) {
        return new ProductoDto(id, "775001", "Cristal", 1L, TipoProducto.PRODUCTO, new BigDecimal("2.00"),
                new BigDecimal("3.50"), 24, 5, 12, null, activo, false, null, OffsetDateTime.parse("2026-10-01T10:00:00-05:00"));
    }

    private static MovimientoInventarioDto movimiento(Long id) {
        return new MovimientoInventarioDto(id, 1L, TipoMovimientoInventario.MERMA, 2, 24, 22, null, null, "Rota", 1L,
                OffsetDateTime.parse("2026-10-13T10:00:00-05:00"));
    }

    @Test
    void sinToken_devuelve401() throws Exception {
        mvc.perform(get(BASE + "/productos")).andExpect(status().isUnauthorized());
    }

    // ================================================================ productos

    @Test
    void crear_admin_devuelve201ConElComandoCompleto() throws Exception {
        when(productoService.crear(any(ProductoCommand.class))).thenReturn(producto(1L, true));

        mvc.perform(comoAdmin(post(BASE + "/productos").contentType(MediaType.APPLICATION_JSON).content(JSON)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Producto creado"))
                .andExpect(jsonPath("$.data[0].nombre").value("Cristal"))
                .andExpect(jsonPath("$.data[0].stock").value(24));

        verify(productoService).crear(new ProductoCommand("775001", "Cristal", 1L, TipoProducto.PRODUCTO,
                new BigDecimal("2"), new BigDecimal("3.5"), 5, 12, null, 24));
    }

    @Test
    void crear_encargado_devuelve403() throws Exception {
        mvc.perform(comoEncargado(post(BASE + "/productos").contentType(MediaType.APPLICATION_JSON).content(JSON)))
                .andExpect(status().isForbidden());

        verify(productoService, never()).crear(any());
    }

    @Test
    void crear_conDatosInvalidos_devuelve400() throws Exception {
        mvc.perform(comoAdmin(post(BASE + "/productos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\",\"precioCompra\":-1,\"unidadesPorCaja\":0,\"stockInicial\":-5}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(4)));

        verify(productoService, never()).crear(any());
    }

    @Test
    void crear_codigoDuplicado_devuelve409() throws Exception {
        when(productoService.crear(any())).thenThrow(new ConflictException("RN-PRO-01: ya existe un producto con el código de barras 775001"));

        mvc.perform(comoAdmin(post(BASE + "/productos").contentType(MediaType.APPLICATION_JSON).content(JSON)))
                .andExpect(status().isConflict());
    }

    @Test
    void crear_envaseInvalido_devuelve422() throws Exception {
        when(productoService.crear(any())).thenThrow(new InvalidProductoException("RN-PRO-08: envaseId debe apuntar a un producto de tipo ENVASE"));

        mvc.perform(comoAdmin(post(BASE + "/productos").contentType(MediaType.APPLICATION_JSON).content(JSON)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void listar_encargadoPuede_yPasaTodosLosFiltros() throws Exception {
        when(productoService.listar(any(), any(), any(), any(), any(PageQuery.class)))
                .thenReturn(new PageResult<>(List.of(producto(1L, true)), 0, 10, 1, 1));

        mvc.perform(comoEncargado(get(BASE + "/productos?q=cris&categoriaId=1&tipo=ENVASE&activo=true&sortBy=stock&direction=desc")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1));

        verify(productoService).listar("cris", 1L, TipoProducto.ENVASE, true, new PageQuery(0, 10, "stock", "desc"));
    }

    @Test
    void listar_conTipoDesconocido_devuelve400() throws Exception {
        mvc.perform(comoEncargado(get(BASE + "/productos?tipo=OTRO"))).andExpect(status().isBadRequest());
    }

    @Test
    void stockBajo_devuelveLaLista() throws Exception {
        when(productoService.stockBajo()).thenReturn(List.of(producto(1L, true)));

        mvc.perform(comoEncargado(get(BASE + "/productos/stock-bajo")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Productos para reponer"))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void porCodigoBarras_existenteEInexistente() throws Exception {
        when(productoService.obtenerPorCodigoBarras("775001")).thenReturn(producto(1L, true));
        when(productoService.obtenerPorCodigoBarras("000")).thenThrow(new ProductoNotFoundException("No existe"));

        mvc.perform(comoEncargado(get(BASE + "/productos/codigo-barras/775001")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].codigoBarras").value("775001"));
        mvc.perform(comoEncargado(get(BASE + "/productos/codigo-barras/000"))).andExpect(status().isNotFound());
    }

    @Test
    void obtener_inexistente_devuelve404() throws Exception {
        when(productoService.obtener(9L)).thenThrow(ProductoNotFoundException.porId(9L));

        mvc.perform(comoEncargado(get(BASE + "/productos/9"))).andExpect(status().isNotFound());
    }

    @Test
    void actualizar_soloAdmin() throws Exception {
        when(productoService.actualizar(any(), any())).thenReturn(producto(1L, true));

        mvc.perform(comoAdmin(put(BASE + "/productos/1").contentType(MediaType.APPLICATION_JSON).content(JSON)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.message").value("Producto actualizado"));
        mvc.perform(comoEncargado(put(BASE + "/productos/1").contentType(MediaType.APPLICATION_JSON).content(JSON)))
                .andExpect(status().isForbidden());
    }

    @Test
    void activarYDesactivar_soloAdmin() throws Exception {
        when(productoService.activar(1L)).thenReturn(producto(1L, true));
        when(productoService.desactivar(1L)).thenReturn(producto(1L, false));

        mvc.perform(comoAdmin(patch(BASE + "/productos/1/activar"))).andExpect(status().isOk());
        mvc.perform(comoAdmin(patch(BASE + "/productos/1/desactivar")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].activo").value(false));
        mvc.perform(comoEncargado(patch(BASE + "/productos/1/activar"))).andExpect(status().isForbidden());
        mvc.perform(comoEncargado(patch(BASE + "/productos/1/desactivar"))).andExpect(status().isForbidden());
    }

    // ================================================================ kardex

    @Test
    void ajustar_admin_devuelve201() throws Exception {
        when(inventarioService.ajustar(any(AjusteInventarioCommand.class))).thenReturn(movimiento(5L));

        mvc.perform(comoAdmin(post(BASE + "/inventario/ajustes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productoId\":1,\"tipo\":\"MERMA\",\"cantidad\":2,\"motivo\":\"Rota\"}")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Movimiento de inventario registrado"))
                .andExpect(jsonPath("$.data[0].stockResultante").value(22));

        verify(inventarioService).ajustar(new AjusteInventarioCommand(1L, TipoMovimientoInventario.MERMA, 2, "Rota"));
    }

    @Test
    void ajustar_encargado_devuelve403() throws Exception {
        mvc.perform(comoEncargado(post(BASE + "/inventario/ajustes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productoId\":1,\"tipo\":\"MERMA\",\"cantidad\":2,\"motivo\":\"Rota\"}")))
                .andExpect(status().isForbidden());
    }

    @Test
    void ajustar_conCuerpoInvalido_devuelve400() throws Exception {
        mvc.perform(comoAdmin(post(BASE + "/inventario/ajustes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productoId\":1,\"tipo\":\"MERMA\",\"cantidad\":0,\"motivo\":\"\"}")))
                .andExpect(status().isBadRequest());
        mvc.perform(comoAdmin(post(BASE + "/inventario/ajustes").contentType(MediaType.APPLICATION_JSON).content("{}")))
                .andExpect(status().isBadRequest());

        verify(inventarioService, never()).ajustar(any());
    }

    @Test
    void ajustar_stockInsuficiente_devuelve422() throws Exception {
        when(inventarioService.ajustar(any())).thenThrow(new StockInsuficienteException("Cristal", 1, 5));

        mvc.perform(comoAdmin(post(BASE + "/inventario/ajustes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productoId\":1,\"tipo\":\"MERMA\",\"cantidad\":5,\"motivo\":\"Rota\"}")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("RN-PRO-02")));
    }

    @Test
    void ajustar_tipoQueGeneranComprasYVentas_devuelve422() throws Exception {
        when(inventarioService.ajustar(any())).thenThrow(new InvalidProductoException("El tipo SALIDA_VENTA lo generan las compras y ventas"));

        mvc.perform(comoAdmin(post(BASE + "/inventario/ajustes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productoId\":1,\"tipo\":\"SALIDA_VENTA\",\"cantidad\":1,\"motivo\":\"x\"}")))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void movimientos_encargadoPuedeVerElKardex() throws Exception {
        when(inventarioService.listarMovimientos(any(), any(), any(PageQuery.class)))
                .thenReturn(new PageResult<>(List.of(movimiento(5L)), 0, 10, 1, 1));

        mvc.perform(comoEncargado(get(BASE + "/inventario/movimientos?productoId=1&tipo=MERMA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Kardex obtenido"))
                .andExpect(jsonPath("$.data[0].tipo").value("MERMA"));

        verify(inventarioService).listarMovimientos(1L, TipoMovimientoInventario.MERMA, new PageQuery(0, 10, "fecha", "asc"));
    }

    @Test
    void movimientos_ordenNoPermitido_devuelve400() throws Exception {
        mvc.perform(comoEncargado(get(BASE + "/inventario/movimientos?sortBy=motivo"))).andExpect(status().isBadRequest());
    }
}
