package nestoredidev.bodega.venta.infrastructure.adapter.input.rest;

import nestoredidev.bodega.compra.application.ports.input.dto.AnularCommand;
import nestoredidev.bodega.venta.application.ports.input.VentaServicePort;
import nestoredidev.bodega.venta.application.ports.input.dto.CreateVentaCommand;
import nestoredidev.bodega.venta.application.ports.input.dto.DetalleVentaCommand;
import nestoredidev.bodega.venta.application.ports.input.dto.PagoVentaCommand;
import nestoredidev.bodega.venta.application.ports.input.dto.VentaDto;
import nestoredidev.bodega.venta.domain.exception.InvalidVentaException;
import nestoredidev.bodega.venta.domain.exception.VentaNotFoundException;
import nestoredidev.bodega.venta.domain.model.EstadoVenta;
import nestoredidev.bodega.producto.domain.exception.StockInsuficienteException;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;
import nestoredidev.common.domain.MetodoPago;
import nestoredidev.common.exception.ConflictException;
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

@WebMvcTest(VentaController.class)
class VentaControllerTest extends AbstractControllerTest {

    private static final String BASE = "/api/v2/bodega/ventas";
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-13T10:00:00-05:00");
    private static final String JSON = """
            {"detalles":[{"productoId":1,"cantidad":2,"envaseEntregado":true}],
             "pagos":[{"metodo":"YAPE","monto":6,"nroOperacion":"OP-1"}]}""";

    @MockitoBean
    private VentaServicePort service;

    private static VentaDto venta(Long id, EstadoVenta estado, boolean conDetalle) {
        return new VentaDto(id, 3L, AHORA, new BigDecimal("6.00"), estado, null, 2L,
                conDetalle ? List.of(new VentaDto.DetalleVentaDto(1L, 1L, "Cristal", 2, new BigDecimal("3.00"), null,
                        new BigDecimal("6.00"))) : null,
                conDetalle ? List.of(new VentaDto.PagoVentaDto(1L, MetodoPago.YAPE, new BigDecimal("6.00"), "OP-1", AHORA)) : null);
    }

    @Test
    void sinToken_devuelve401() throws Exception {
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(JSON)).andExpect(status().isUnauthorized());
    }

    @Test
    void crear_encargadoPuede_devuelve201ConDetallesYPagos() throws Exception {
        when(service.crear(any(CreateVentaCommand.class))).thenReturn(venta(1L, EstadoVenta.REGISTRADA, true));

        mvc.perform(comoEncargado(post(BASE).contentType(MediaType.APPLICATION_JSON).content(JSON)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Venta registrada"))
                .andExpect(jsonPath("$.data[0].total").value(6.0))
                .andExpect(jsonPath("$.data[0].detalles[0].productoNombre").value("Cristal"))
                .andExpect(jsonPath("$.data[0].detalles[0].costoUnitario").doesNotExist())
                .andExpect(jsonPath("$.data[0].pagos[0].nroOperacion").value("OP-1"));

        verify(service).crear(new CreateVentaCommand(List.of(new DetalleVentaCommand(1L, 2, true)),
                List.of(new PagoVentaCommand(MetodoPago.YAPE, new BigDecimal("6"), "OP-1"))));
    }

    @Test
    void crear_sinDetallesNiPagos_devuelve400() throws Exception {
        mvc.perform(comoEncargado(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{}")))
                .andExpect(status().isBadRequest());
        mvc.perform(comoEncargado(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"detalles\":[{\"productoId\":1,\"cantidad\":0}],\"pagos\":[{\"metodo\":\"EFECTIVO\",\"monto\":0}]}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));

        verify(service, never()).crear(any());
    }

    @Test
    void crear_metodoDePagoDesconocido_devuelve400() throws Exception {
        mvc.perform(comoEncargado(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"detalles\":[{\"productoId\":1,\"cantidad\":1}],\"pagos\":[{\"metodo\":\"BITCOIN\",\"monto\":3}]}")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crear_stockInsuficiente_devuelve422() throws Exception {
        when(service.crear(any())).thenThrow(new StockInsuficienteException("Cristal", 1, 2));

        mvc.perform(comoEncargado(post(BASE).contentType(MediaType.APPLICATION_JSON).content(JSON)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("RN-PRO-02")));
    }

    @Test
    void crear_pagosQueNoSumanElTotal_devuelve422() throws Exception {
        when(service.crear(any())).thenThrow(new InvalidVentaException("RN-VEN-03: la suma de los pagos debe ser igual al total"));

        mvc.perform(comoEncargado(post(BASE).contentType(MediaType.APPLICATION_JSON).content(JSON)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void crear_numeroDeOperacionRepetido_devuelve409() throws Exception {
        when(service.crear(any())).thenThrow(new ConflictException("RN-PVE-03: el número de operación OP-1 ya fue registrado con YAPE"));

        mvc.perform(comoEncargado(post(BASE).contentType(MediaType.APPLICATION_JSON).content(JSON)))
                .andExpect(status().isConflict());
    }

    @Test
    void crear_sinCajaAbierta_devuelve422() throws Exception {
        when(service.crear(any())).thenThrow(new DomainException("RN-G05: no hay una caja de bodega abierta; ábrela primero"));

        mvc.perform(comoEncargado(post(BASE).contentType(MediaType.APPLICATION_JSON).content(JSON)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("RN-G05")));
    }

    @Test
    void listar_pasaFiltrosFechasYOrdenPorDefecto() throws Exception {
        when(service.listar(any(), any(), any(), any(), any(PageQuery.class)))
                .thenReturn(new PageResult<>(List.of(venta(1L, EstadoVenta.REGISTRADA, false)), 0, 10, 1, 1));

        mvc.perform(comoEncargado(get(BASE + "?cajaId=3&estado=REGISTRADA&desde=2026-10-13&hasta=2026-10-13")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].detalles").doesNotExist());

        verify(service).listar(3L, EstadoVenta.REGISTRADA, LocalDate.of(2026, 10, 13), LocalDate.of(2026, 10, 13),
                new PageQuery(0, 10, "fecha", "asc"));
    }

    @Test
    void listar_estadoDesconocido_devuelve400() throws Exception {
        mvc.perform(comoEncargado(get(BASE + "?estado=PERDIDA"))).andExpect(status().isBadRequest());
    }

    @Test
    void obtener_existenteEInexistente() throws Exception {
        when(service.obtener(1L)).thenReturn(venta(1L, EstadoVenta.REGISTRADA, true));
        when(service.obtener(9L)).thenThrow(new VentaNotFoundException(9L));

        mvc.perform(comoEncargado(get(BASE + "/1"))).andExpect(status().isOk());
        mvc.perform(comoEncargado(get(BASE + "/9")))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Venta con id 9 no existe"));
    }

    @Test
    void anular_admin_devuelve200() throws Exception {
        when(service.anular(any(), any(AnularCommand.class))).thenReturn(venta(1L, EstadoVenta.ANULADA, true));

        mvc.perform(comoAdmin(post(BASE + "/1/anular").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Cliente se arrepintió\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Venta anulada"))
                .andExpect(jsonPath("$.data[0].estado").value("ANULADA"));

        verify(service).anular(1L, new AnularCommand("Cliente se arrepintió"));
    }

    @Test
    void anular_encargado_devuelve403() throws Exception {
        mvc.perform(comoEncargado(post(BASE + "/1/anular").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"x\"}")))
                .andExpect(status().isForbidden());

        verify(service, never()).anular(any(), any());
    }

    @Test
    void anular_cajaCerrada_devuelve422() throws Exception {
        when(service.anular(any(), any())).thenThrow(new DomainException("RN-VEN-05: la caja de esta venta ya está cerrada"));

        mvc.perform(comoAdmin(post(BASE + "/1/anular").contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"x\"}")))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void anular_sinMotivo_devuelve400() throws Exception {
        mvc.perform(comoAdmin(post(BASE + "/1/anular").contentType(MediaType.APPLICATION_JSON).content("{}")))
                .andExpect(status().isBadRequest());
    }
}
