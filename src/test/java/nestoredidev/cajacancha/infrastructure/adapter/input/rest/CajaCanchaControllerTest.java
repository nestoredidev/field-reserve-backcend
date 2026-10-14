package nestoredidev.cajacancha.infrastructure.adapter.input.rest;

import nestoredidev.cajacancha.application.ports.input.CajaCanchaServicePort;
import nestoredidev.cajacancha.application.ports.input.dto.AbrirCajaCommand;
import nestoredidev.cajacancha.application.ports.input.dto.CajaCanchaDto;
import nestoredidev.cajacancha.application.ports.input.dto.CerrarCajaCommand;
import nestoredidev.cajacancha.application.ports.input.dto.MovimientoCajaCommand;
import nestoredidev.cajacancha.application.ports.input.dto.MovimientoCajaDto;
import nestoredidev.cajacancha.domain.exception.CajaCanchaNotFoundException;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;
import nestoredidev.common.domain.EstadoCaja;
import nestoredidev.common.domain.MetodoPago;
import nestoredidev.common.domain.TipoMovimientoCaja;
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

@WebMvcTest(CajaCanchaController.class)
class CajaCanchaControllerTest extends AbstractControllerTest {

    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-13T10:00:00-05:00");

    @MockitoBean
    private CajaCanchaServicePort service;

    private static CajaCanchaDto caja(Long id, EstadoCaja estado) {
        return new CajaCanchaDto(id, LocalDate.of(2026, 10, 13), estado, new BigDecimal("100.00"), null, null, 1L, null,
                AHORA, null, new BigDecimal("0.00"), new BigDecimal("0.00"), new BigDecimal("0.00"),
                new BigDecimal("0.00"), null, null, null);
    }

    private static MovimientoCajaDto movimiento(Long id) {
        return new MovimientoCajaDto(id, 1L, TipoMovimientoCaja.EGRESO, MetodoPago.EFECTIVO, "Luz",
                new BigDecimal("20.00"), 1L, AHORA);
    }

    @Test
    void sinToken_devuelve401() throws Exception {
        mvc.perform(get("/api/v2/cancha/cajas/actual")).andExpect(status().isUnauthorized());
    }

    @Test
    void abrir_devuelve201() throws Exception {
        when(service.abrir(any(AbrirCajaCommand.class))).thenReturn(caja(1L, EstadoCaja.ABIERTA));

        mvc.perform(comoEncargado(post("/api/v2/cancha/cajas/abrir").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montoApertura\":100}")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Caja abierta"))
                .andExpect(jsonPath("$.data[0].estado").value("ABIERTA"))
                .andExpect(jsonPath("$.data[0].montoApertura").value(100.0));

        verify(service).abrir(new AbrirCajaCommand(new BigDecimal("100")));
    }

    @Test
    void abrir_montoNegativoOAusente_devuelve400() throws Exception {
        mvc.perform(comoEncargado(post("/api/v2/cancha/cajas/abrir").contentType(MediaType.APPLICATION_JSON)
                .content("{\"montoApertura\":-1}"))).andExpect(status().isBadRequest());
        mvc.perform(comoEncargado(post("/api/v2/cancha/cajas/abrir").contentType(MediaType.APPLICATION_JSON)
                .content("{}"))).andExpect(status().isBadRequest());

        verify(service, never()).abrir(any());
    }

    @Test
    void abrir_yaExisteCajaAbierta_devuelve409() throws Exception {
        when(service.abrir(any())).thenThrow(new ConflictException("RN-CAJ-01: ya existe una caja de cancha abierta"));

        mvc.perform(comoEncargado(post("/api/v2/cancha/cajas/abrir").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montoApertura\":0}")))
                .andExpect(status().isConflict());
    }

    @Test
    void actual_conCajaAbierta_devuelve200() throws Exception {
        when(service.actual()).thenReturn(caja(1L, EstadoCaja.ABIERTA));

        mvc.perform(comoEncargado(get("/api/v2/cancha/cajas/actual")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Caja abierta actual"))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].efectivoEsperado").doesNotExist());
    }

    @Test
    void actual_sinCajaAbierta_devuelve404() throws Exception {
        when(service.actual()).thenThrow(new CajaCanchaNotFoundException("No hay una caja de cancha abierta"));

        mvc.perform(comoEncargado(get("/api/v2/cancha/cajas/actual")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No hay una caja de cancha abierta"));
    }

    @Test
    void cerrar_devuelve200() throws Exception {
        when(service.cerrar(any(CerrarCajaCommand.class))).thenReturn(caja(1L, EstadoCaja.CERRADA));

        mvc.perform(comoEncargado(post("/api/v2/cancha/cajas/cerrar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montoCierreContado\":150,\"observacion\":\"ok\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Caja cerrada"));

        verify(service).cerrar(new CerrarCajaCommand(new BigDecimal("150"), "ok"));
    }

    @Test
    void cerrar_sinMontoContado_devuelve400() throws Exception {
        mvc.perform(comoEncargado(post("/api/v2/cancha/cajas/cerrar").contentType(MediaType.APPLICATION_JSON)
                .content("{\"observacion\":\"x\"}"))).andExpect(status().isBadRequest());
    }

    @Test
    void cerrar_conDiferenciaSinObservacion_devuelve422() throws Exception {
        when(service.cerrar(any())).thenThrow(new DomainException("RN-CAJ-04: la observación es obligatoria"));

        mvc.perform(comoEncargado(post("/api/v2/cancha/cajas/cerrar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montoCierreContado\":140}")))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void listar_usaOrdenPorDefectoAbiertaAt() throws Exception {
        when(service.listar(any(PageQuery.class)))
                .thenReturn(new PageResult<>(List.of(caja(1L, EstadoCaja.ABIERTA)), 0, 10, 1, 1));

        mvc.perform(comoEncargado(get("/api/v2/cancha/cajas")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cajas obtenidas"));

        verify(service).listar(new PageQuery(0, 10, "abiertaAt", "asc"));
    }

    @Test
    void obtener_existenteEInexistente() throws Exception {
        when(service.obtener(1L)).thenReturn(caja(1L, EstadoCaja.CERRADA));
        when(service.obtener(9L)).thenThrow(new CajaCanchaNotFoundException("Caja con id 9 no existe"));

        mvc.perform(comoEncargado(get("/api/v2/cancha/cajas/1"))).andExpect(status().isOk());
        mvc.perform(comoEncargado(get("/api/v2/cancha/cajas/9"))).andExpect(status().isNotFound());
    }

    @Test
    void registrarMovimiento_devuelve201() throws Exception {
        when(service.registrarMovimiento(any(MovimientoCajaCommand.class))).thenReturn(movimiento(5L));

        mvc.perform(comoEncargado(post("/api/v2/cancha/cajas/movimientos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"EGRESO\",\"metodo\":\"EFECTIVO\",\"concepto\":\"Luz\",\"monto\":20}")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Movimiento registrado"))
                .andExpect(jsonPath("$.data[0].concepto").value("Luz"));

        verify(service).registrarMovimiento(new MovimientoCajaCommand(TipoMovimientoCaja.EGRESO, MetodoPago.EFECTIVO,
                "Luz", new BigDecimal("20")));
    }

    @Test
    void registrarMovimiento_conCuerpoInvalido_devuelve400() throws Exception {
        mvc.perform(comoEncargado(post("/api/v2/cancha/cajas/movimientos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"EGRESO\",\"metodo\":\"EFECTIVO\",\"concepto\":\"\",\"monto\":0}")))
                .andExpect(status().isBadRequest());
        mvc.perform(comoEncargado(post("/api/v2/cancha/cajas/movimientos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"OTRO\",\"metodo\":\"EFECTIVO\",\"concepto\":\"x\",\"monto\":1}")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registrarMovimiento_egresoMayorAlEfectivo_devuelve422() throws Exception {
        when(service.registrarMovimiento(any())).thenThrow(new DomainException("RN-MCA-03: el egreso supera el efectivo"));

        mvc.perform(comoEncargado(post("/api/v2/cancha/cajas/movimientos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"EGRESO\",\"metodo\":\"EFECTIVO\",\"concepto\":\"Luz\",\"monto\":999}")))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void movimientos_devuelveLaPaginaDeLaCaja() throws Exception {
        when(service.listarMovimientos(any(), any(PageQuery.class)))
                .thenReturn(new PageResult<>(List.of(movimiento(5L)), 0, 10, 1, 1));

        mvc.perform(comoEncargado(get("/api/v2/cancha/cajas/1/movimientos?sortBy=monto&direction=desc")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Movimientos obtenidos"))
                .andExpect(jsonPath("$.data[0].id").value(5));

        verify(service).listarMovimientos(1L, new PageQuery(0, 10, "monto", "desc"));
    }

    @Test
    void movimientos_ordenNoPermitido_devuelve400() throws Exception {
        mvc.perform(comoEncargado(get("/api/v2/cancha/cajas/1/movimientos?sortBy=concepto")))
                .andExpect(status().isBadRequest());
    }
}
