package nestoredidev.bodega.compra.infrastructure.adapter.input.rest;

import nestoredidev.bodega.compra.application.ports.input.CompraServicePort;
import nestoredidev.bodega.compra.application.ports.input.dto.AnularCommand;
import nestoredidev.bodega.compra.application.ports.input.dto.CompraDto;
import nestoredidev.bodega.compra.application.ports.input.dto.CreateCompraCommand;
import nestoredidev.bodega.compra.application.ports.input.dto.DetalleCompraCommand;
import nestoredidev.bodega.compra.domain.exception.CompraNotFoundException;
import nestoredidev.bodega.compra.domain.model.EstadoCompra;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;
import nestoredidev.common.exception.DomainException;
import nestoredidev.support.AbstractControllerTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CompraController.class)
class CompraControllerTest extends AbstractControllerTest {

    private static final String BASE = "/api/v2/bodega/compras";
    private static final String JSON = """
            {"proveedorId":5,"nroComprobante":"F001-55","pagadoDesdeCaja":true,
             "detalles":[{"productoId":1,"cantidadCajas":2,"cantidadSueltas":3,"costoUnitario":1.5}]}""";

    @MockitoBean
    private CompraServicePort service;

    private static CompraDto compra(Long id, boolean conDetalles) {
        List<CompraDto.DetalleCompraDto> detalles = conDetalles
                ? List.of(new CompraDto.DetalleCompraDto(1L, 1L, 2, 27, new BigDecimal("1.50"), new BigDecimal("40.50")))
                : null;
        return new CompraDto(id, 5L, "F001-55", OffsetDateTime.parse("2026-10-13T10:00:00-05:00"),
                new BigDecimal("40.50"), EstadoCompra.REGISTRADA, 1L, detalles);
    }

    @Test
    void sinToken_devuelve401() throws Exception {
        mvc.perform(get(BASE)).andExpect(status().isUnauthorized());
    }

    @Test
    void crear_encargadoPuede_devuelve201ConDetalles() throws Exception {
        when(service.crear(any(CreateCompraCommand.class))).thenReturn(compra(1L, true));

        mvc.perform(comoEncargado(post(BASE).contentType(MediaType.APPLICATION_JSON).content(JSON)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Compra registrada"))
                .andExpect(jsonPath("$.data[0].total").value(40.5))
                .andExpect(jsonPath("$.data[0].detalles[0].cantidadUnidades").value(27));

        verify(service).crear(new CreateCompraCommand(5L, "F001-55", true,
                List.of(new DetalleCompraCommand(1L, 2, 3, new BigDecimal("1.5")))));
    }

    @Test
    void crear_sinDetallesODetalleInvalido_devuelve400() throws Exception {
        mvc.perform(comoEncargado(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{\"detalles\":[]}")))
                .andExpect(status().isBadRequest());
        mvc.perform(comoEncargado(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"detalles\":[{\"cantidadCajas\":-1,\"costoUnitario\":-2}]}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));

        verify(service, never()).crear(any());
    }

    @Test
    void crear_proveedorInactivo_devuelve422() throws Exception {
        when(service.crear(any())).thenThrow(new DomainException("RN-PRV-02: el proveedor 'Backus' está inactivo"));

        mvc.perform(comoEncargado(post(BASE).contentType(MediaType.APPLICATION_JSON).content(JSON)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("RN-PRV-02")));
    }

    @Test
    void listar_pasaFiltrosFechasYOrdenPorDefecto() throws Exception {
        when(service.listar(any(), any(), any(), any(), any(PageQuery.class)))
                .thenReturn(new PageResult<>(List.of(compra(1L, false)), 0, 10, 1, 1));

        mvc.perform(comoEncargado(get(BASE + "?proveedorId=5&estado=ANULADA&desde=2026-10-01&hasta=2026-10-31")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].detalles").doesNotExist());

        verify(service).listar(5L, EstadoCompra.ANULADA, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31),
                new PageQuery(0, 10, "fecha", "asc"));
    }

    @Test
    void listar_fechaMalFormada_devuelve400() throws Exception {
        mvc.perform(comoEncargado(get(BASE + "?desde=13/10/2026"))).andExpect(status().isBadRequest());
    }

    @Test
    void obtener_existenteEInexistente() throws Exception {
        when(service.obtener(1L)).thenReturn(compra(1L, true));
        when(service.obtener(9L)).thenThrow(new CompraNotFoundException(9L));

        mvc.perform(comoEncargado(get(BASE + "/1"))).andExpect(status().isOk());
        mvc.perform(comoEncargado(get(BASE + "/9"))).andExpect(status().isNotFound());
    }

    @Test
    void anular_admin_devuelve200() throws Exception {
        when(service.anular(any(), any(AnularCommand.class))).thenReturn(compra(1L, true));

        mvc.perform(comoAdmin(post(BASE + "/1/anular").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Error de digitación\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Compra anulada"));

        verify(service).anular(1L, new AnularCommand("Error de digitación"));
    }

    @Test
    void anular_encargado_devuelve403() throws Exception {
        mvc.perform(comoEncargado(post(BASE + "/1/anular").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"x\"}")))
                .andExpect(status().isForbidden());

        verify(service, never()).anular(any(), any());
    }

    @Test
    void anular_sinMotivo_devuelve400() throws Exception {
        mvc.perform(comoAdmin(post(BASE + "/1/anular").contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\" \"}")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anular_mercaderiaYaVendida_devuelve422() throws Exception {
        when(service.anular(any(), any())).thenThrow(new DomainException("RN-COM-06: no se puede anular, parte de esa mercadería ya se vendió"));

        mvc.perform(comoAdmin(post(BASE + "/1/anular").contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"x\"}")))
                .andExpect(status().isUnprocessableEntity());
    }
}
