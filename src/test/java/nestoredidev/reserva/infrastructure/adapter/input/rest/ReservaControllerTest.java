package nestoredidev.reserva.infrastructure.adapter.input.rest;

import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;
import nestoredidev.common.domain.MetodoPago;
import nestoredidev.common.exception.ConflictException;
import nestoredidev.common.exception.DomainException;
import nestoredidev.common.exception.ForbiddenOperationException;
import nestoredidev.reserva.application.ports.input.ReservaServicePort;
import nestoredidev.reserva.application.ports.input.dto.CambiarMontoCommand;
import nestoredidev.reserva.application.ports.input.dto.CancelarReservaCommand;
import nestoredidev.reserva.application.ports.input.dto.CreateMantenimientoCommand;
import nestoredidev.reserva.application.ports.input.dto.CreateReservaCommand;
import nestoredidev.reserva.application.ports.input.dto.PagoReservaCommand;
import nestoredidev.reserva.application.ports.input.dto.PagoReservaDto;
import nestoredidev.reserva.application.ports.input.dto.ReprogramarCommand;
import nestoredidev.reserva.application.ports.input.dto.ReservaDto;
import nestoredidev.reserva.application.ports.input.dto.ReservaFiltro;
import nestoredidev.reserva.application.ports.input.dto.SlotDto;
import nestoredidev.reserva.domain.exception.InvalidReservaException;
import nestoredidev.reserva.domain.exception.ReservaNotFoundException;
import nestoredidev.reserva.domain.model.EstadoReserva;
import nestoredidev.reserva.domain.model.OrigenReserva;
import nestoredidev.reserva.domain.model.TipoPagoReserva;
import nestoredidev.reserva.domain.model.TipoReserva;
import nestoredidev.support.AbstractControllerTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReservaController.class)
class ReservaControllerTest extends AbstractControllerTest {

    private static final String BASE = "/api/v2";
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-13T10:00:00-05:00");
    private static final String JSON_CREAR = """
            {"canchaId":1,"clienteId":5,"inicio":"2026-10-14T18:00:00","fin":"2026-10-14T20:00:00","origen":"WHATSAPP",
             "observacion":"llamó","pagoInicial":{"metodo":"YAPE","monto":50,"nroOperacion":"OP-1"}}""";

    @MockitoBean
    private ReservaServicePort service;

    private static ReservaDto reserva(Long id, EstadoReserva estado) {
        return new ReservaDto(id, 1L, 5L, "Ana", "911111111", TipoReserva.RESERVA, estado,
                AHORA.plusDays(1).withHour(18), AHORA.plusDays(1).withHour(20), new BigDecimal("100.00"),
                new BigDecimal("50.00"), new BigDecimal("50.00"), new BigDecimal("50.00"), AHORA.plusHours(2),
                OrigenReserva.WHATSAPP, null, null, 9L, AHORA, null, null);
    }

    private static PagoReservaDto pago(Long id, TipoPagoReserva tipo) {
        return new PagoReservaDto(id, 10L, 3L, tipo, MetodoPago.YAPE, new BigDecimal("50.00"), "OP-1", null, 9L, AHORA);
    }

    @Test
    void sinToken_devuelve401() throws Exception {
        mvc.perform(get(BASE + "/reservas")).andExpect(status().isUnauthorized());
    }

    // ================================================================ crear

    @Test
    void crear_encargadoPuede_devuelve201ConHorasLocalesYPagoInicial() throws Exception {
        when(service.crear(any(CreateReservaCommand.class))).thenReturn(reserva(10L, EstadoReserva.CONFIRMADA));

        mvc.perform(comoEncargado(post(BASE + "/reservas").contentType(MediaType.APPLICATION_JSON).content(JSON_CREAR)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Reserva creada"))
                .andExpect(jsonPath("$.data[0].id").value(10))
                .andExpect(jsonPath("$.data[0].estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$.data[0].saldo").value(50.0));

        verify(service).crear(new CreateReservaCommand(1L, 5L, null, null, LocalDateTime.of(2026, 10, 14, 18, 0),
                LocalDateTime.of(2026, 10, 14, 20, 0), OrigenReserva.WHATSAPP, "llamó",
                new PagoReservaCommand(MetodoPago.YAPE, new BigDecimal("50"), "OP-1", null, null)));
    }

    @Test
    void crear_conClienteNuevoPorNombreYTelefono() throws Exception {
        when(service.crear(any())).thenReturn(reserva(10L, EstadoReserva.PENDIENTE));

        mvc.perform(comoEncargado(post(BASE + "/reservas").contentType(MediaType.APPLICATION_JSON).content("""
                        {"canchaId":1,"clienteNombre":"Luis","clienteTelefono":"922222222",
                         "inicio":"2026-10-14T18:00:00","fin":"2026-10-14T19:00:00"}""")))
                .andExpect(status().isCreated());

        verify(service).crear(new CreateReservaCommand(1L, null, "Luis", "922222222", LocalDateTime.of(2026, 10, 14, 18, 0),
                LocalDateTime.of(2026, 10, 14, 19, 0), null, null, null));
    }

    @Test
    void crear_sinCanchaNiHorario_devuelve400() throws Exception {
        mvc.perform(comoEncargado(post(BASE + "/reservas").contentType(MediaType.APPLICATION_JSON).content("{\"clienteId\":5}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(3)));

        verify(service, never()).crear(any());
    }

    @Test
    void crear_telefonoInvalidoOPagoInicialInvalido_devuelve400() throws Exception {
        mvc.perform(comoEncargado(post(BASE + "/reservas").contentType(MediaType.APPLICATION_JSON).content("""
                        {"canchaId":1,"clienteNombre":"Luis","clienteTelefono":"123",
                         "inicio":"2026-10-14T18:00:00","fin":"2026-10-14T19:00:00"}""")))
                .andExpect(status().isBadRequest());
        mvc.perform(comoEncargado(post(BASE + "/reservas").contentType(MediaType.APPLICATION_JSON).content("""
                        {"canchaId":1,"clienteId":5,"inicio":"2026-10-14T18:00:00","fin":"2026-10-14T19:00:00",
                         "pagoInicial":{"metodo":"EFECTIVO","monto":0}}""")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crear_conFechaMalFormada_devuelve400() throws Exception {
        mvc.perform(comoEncargado(post(BASE + "/reservas").contentType(MediaType.APPLICATION_JSON).content("""
                        {"canchaId":1,"clienteId":5,"inicio":"mañana","fin":"2026-10-14T19:00:00"}""")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request"));
    }

    @Test
    void crear_horarioQueSeCruza_devuelve409ConElDetalle() throws Exception {
        when(service.crear(any())).thenThrow(new ConflictException("RN-RES-06: el horario se cruza con otra reserva o mantenimiento",
                List.of("Reserva #7 2026-10-14T18:00 - 19:00 [CONFIRMADA] Luis (922222222)")));

        mvc.perform(comoEncargado(post(BASE + "/reservas").contentType(MediaType.APPLICATION_JSON).content(JSON_CREAR)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("RN-RES-06")))
                .andExpect(jsonPath("$.errors[0]").value(org.hamcrest.Matchers.containsString("Reserva #7")));
    }

    @Test
    void crear_reglaDeHorario_devuelve422() throws Exception {
        when(service.crear(any())).thenThrow(new InvalidReservaException("RN-RES-03: no se crean reservas en el pasado"));

        mvc.perform(comoEncargado(post(BASE + "/reservas").contentType(MediaType.APPLICATION_JSON).content(JSON_CREAR)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("RN-RES-03")));
    }

    // ================================================================ listar / disponibilidad / obtener

    @Test
    void listar_armaElFiltroConTodosLosParametros() throws Exception {
        when(service.listar(any(ReservaFiltro.class), any(PageQuery.class)))
                .thenReturn(new PageResult<>(List.of(reserva(10L, EstadoReserva.PENDIENTE)), 0, 10, 1, 1));

        mvc.perform(comoEncargado(get(BASE + "/reservas?canchaId=1&clienteId=5&estado=PENDIENTE&tipo=RESERVA"
                        + "&desde=2026-10-01&hasta=2026-10-31&sortBy=montoTotal&direction=desc")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Reservas obtenidas"))
                .andExpect(jsonPath("$.data[0].clienteNombre").value("Ana"));

        verify(service).listar(new ReservaFiltro(1L, 5L, EstadoReserva.PENDIENTE, TipoReserva.RESERVA,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)), new PageQuery(0, 10, "montoTotal", "desc"));
    }

    @Test
    void listar_sinFiltros_ordenaPorInicio() throws Exception {
        when(service.listar(any(), any())).thenReturn(new PageResult<>(List.of(), 0, 10, 0, 0));

        mvc.perform(comoEncargado(get(BASE + "/reservas"))).andExpect(status().isOk());

        verify(service).listar(new ReservaFiltro(null, null, null, null, null, null), new PageQuery(0, 10, "inicio", "asc"));
    }

    @Test
    void listar_estadoDesconocidoOFechaMalFormada_devuelve400() throws Exception {
        mvc.perform(comoEncargado(get(BASE + "/reservas?estado=VOLANDO"))).andExpect(status().isBadRequest());
        mvc.perform(comoEncargado(get(BASE + "/reservas?desde=hoy"))).andExpect(status().isBadRequest());
    }

    @Test
    void disponibilidad_devuelveLosSlots() throws Exception {
        when(service.disponibilidad(1L, LocalDate.of(2026, 10, 14))).thenReturn(List.of(
                new SlotDto(LocalTime.of(18, 0), LocalTime.of(19, 0), new BigDecimal("50.00"), true),
                new SlotDto(LocalTime.of(19, 0), LocalTime.of(20, 0), new BigDecimal("50.00"), false)));

        mvc.perform(comoEncargado(get(BASE + "/reservas/disponibilidad?canchaId=1&fecha=2026-10-14")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Disponibilidad del día"))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].disponible").value(true))
                .andExpect(jsonPath("$.data[1].disponible").value(false));
    }

    @Test
    void disponibilidad_sinParametrosObligatorios_devuelve400() throws Exception {
        mvc.perform(comoEncargado(get(BASE + "/reservas/disponibilidad?canchaId=1"))).andExpect(status().isBadRequest());
        mvc.perform(comoEncargado(get(BASE + "/reservas/disponibilidad?fecha=2026-10-14"))).andExpect(status().isBadRequest());
    }

    @Test
    void obtener_existenteEInexistente() throws Exception {
        when(service.obtener(10L)).thenReturn(reserva(10L, EstadoReserva.CONFIRMADA));
        when(service.obtener(99L)).thenThrow(new ReservaNotFoundException(99L));

        mvc.perform(comoEncargado(get(BASE + "/reservas/10")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].pagado").value(50.0));
        mvc.perform(comoEncargado(get(BASE + "/reservas/99")))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Reserva con id 99 no existe"));
    }

    // ================================================================ cambios de estado

    @Test
    void reprogramar_devuelve200() throws Exception {
        when(service.reprogramar(eq(10L), any(ReprogramarCommand.class))).thenReturn(reserva(10L, EstadoReserva.CONFIRMADA));

        mvc.perform(comoEncargado(put(BASE + "/reservas/10/reprogramar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inicio\":\"2026-10-15T18:00:00\",\"fin\":\"2026-10-15T19:00:00\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Reserva reprogramada"));

        verify(service).reprogramar(10L, new ReprogramarCommand(LocalDateTime.of(2026, 10, 15, 18, 0),
                LocalDateTime.of(2026, 10, 15, 19, 0)));
    }

    @Test
    void reprogramar_sinHorario_devuelve400() throws Exception {
        mvc.perform(comoEncargado(put(BASE + "/reservas/10/reprogramar").contentType(MediaType.APPLICATION_JSON).content("{}")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reprogramar_sinEfectivoParaDevolverElExceso_devuelve422() throws Exception {
        when(service.reprogramar(any(), any())).thenThrow(new DomainException("RN-MCA-03: no hay efectivo suficiente en caja"));

        mvc.perform(comoEncargado(put(BASE + "/reservas/10/reprogramar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inicio\":\"2026-10-15T18:00:00\",\"fin\":\"2026-10-15T19:00:00\"}")))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void cancelar_devuelve200ConLaDevolucionSugerida() throws Exception {
        ReservaDto cancelada = new ReservaDto(10L, 1L, 5L, "Ana", "911111111", TipoReserva.RESERVA, EstadoReserva.CANCELADA,
                AHORA.plusDays(2), AHORA.plusDays(2).plusHours(1), new BigDecimal("100.00"), new BigDecimal("50.00"),
                new BigDecimal("50.00"), new BigDecimal("50.00"), null, OrigenReserva.PRESENCIAL, null, "Lluvia", 9L,
                AHORA, null, new BigDecimal("50.00"));
        when(service.cancelar(eq(10L), any(CancelarReservaCommand.class))).thenReturn(cancelada);

        mvc.perform(comoEncargado(post(BASE + "/reservas/10/cancelar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Lluvia\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Reserva cancelada"))
                .andExpect(jsonPath("$.data[0].estado").value("CANCELADA"))
                .andExpect(jsonPath("$.data[0].devolucionSugerida").value(50.0));

        verify(service).cancelar(10L, new CancelarReservaCommand("Lluvia"));
    }

    @Test
    void cancelar_sinMotivo_devuelve400() throws Exception {
        mvc.perform(comoEncargado(post(BASE + "/reservas/10/cancelar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\" \"}")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancelar_mantenimientoPorEncargado_devuelve403() throws Exception {
        when(service.cancelar(any(), any())).thenThrow(new ForbiddenOperationException("RN-RES-23: solo el ADMIN cancela mantenimientos"));

        mvc.perform(comoEncargado(post(BASE + "/reservas/11/cancelar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"x\"}")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("RN-RES-23")));
    }

    @Test
    void noAsistio_y_completar_devuelven200() throws Exception {
        when(service.marcarNoAsistio(10L)).thenReturn(reserva(10L, EstadoReserva.NO_ASISTIO));
        when(service.completar(10L)).thenReturn(reserva(10L, EstadoReserva.COMPLETADA));

        mvc.perform(comoEncargado(post(BASE + "/reservas/10/no-asistio")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Reserva marcada como NO_ASISTIO"))
                .andExpect(jsonPath("$.data[0].estado").value("NO_ASISTIO"));
        mvc.perform(comoEncargado(post(BASE + "/reservas/10/completar")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].estado").value("COMPLETADA"));
    }

    @Test
    void completar_conDeuda_devuelve422() throws Exception {
        when(service.completar(10L)).thenThrow(new InvalidReservaException("RN-RES-19: no se completa con deuda"));

        mvc.perform(comoEncargado(post(BASE + "/reservas/10/completar"))).andExpect(status().isUnprocessableEntity());
    }

    // ================================================================ monto (ADMIN)

    @Test
    void cambiarMonto_admin_devuelve200() throws Exception {
        when(service.cambiarMonto(eq(10L), any(CambiarMontoCommand.class))).thenReturn(reserva(10L, EstadoReserva.CONFIRMADA));

        mvc.perform(comoAdmin(patch(BASE + "/reservas/10/monto").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":80,\"observacion\":\"Descuento\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Monto actualizado"));

        verify(service).cambiarMonto(10L, new CambiarMontoCommand(new BigDecimal("80"), "Descuento"));
    }

    @Test
    void cambiarMonto_encargado_devuelve403SinLlamarAlServicio() throws Exception {
        mvc.perform(comoEncargado(patch(BASE + "/reservas/10/monto").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":80,\"observacion\":\"Descuento\"}")))
                .andExpect(status().isForbidden());

        verify(service, never()).cambiarMonto(any(), any());
    }

    @Test
    void cambiarMonto_sinObservacionOMontoCero_devuelve400() throws Exception {
        mvc.perform(comoAdmin(patch(BASE + "/reservas/10/monto").contentType(MediaType.APPLICATION_JSON)
                .content("{\"monto\":80}"))).andExpect(status().isBadRequest());
        mvc.perform(comoAdmin(patch(BASE + "/reservas/10/monto").contentType(MediaType.APPLICATION_JSON)
                .content("{\"monto\":0,\"observacion\":\"x\"}"))).andExpect(status().isBadRequest());
    }

    @Test
    void cambiarMonto_menorAlYaPagado_devuelve422() throws Exception {
        when(service.cambiarMonto(any(), any())).thenThrow(new InvalidReservaException("RN-PAG-03: el nuevo monto no puede ser menor a lo ya pagado"));

        mvc.perform(comoAdmin(patch(BASE + "/reservas/10/monto").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":10,\"observacion\":\"x\"}")))
                .andExpect(status().isUnprocessableEntity());
    }

    // ================================================================ pagos

    @Test
    void registrarPago_devuelve201() throws Exception {
        when(service.registrarPago(eq(10L), any(PagoReservaCommand.class))).thenReturn(pago(1L, TipoPagoReserva.ADELANTO));

        mvc.perform(comoEncargado(post(BASE + "/reservas/10/pagos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metodo\":\"YAPE\",\"monto\":50,\"nroOperacion\":\"OP-1\"}")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Pago registrado"))
                .andExpect(jsonPath("$.data[0].tipo").value("ADELANTO"))
                .andExpect(jsonPath("$.data[0].nroOperacion").value("OP-1"));

        verify(service).registrarPago(10L, new PagoReservaCommand(MetodoPago.YAPE, new BigDecimal("50"), "OP-1", null, null));
    }

    @Test
    void registrarPago_devolucionMarcadaConElFlag() throws Exception {
        when(service.registrarPago(eq(10L), any())).thenReturn(pago(2L, TipoPagoReserva.DEVOLUCION));

        mvc.perform(comoAdmin(post(BASE + "/reservas/10/pagos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metodo\":\"EFECTIVO\",\"monto\":20,\"devolucion\":true}")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data[0].tipo").value("DEVOLUCION"));

        verify(service).registrarPago(10L, new PagoReservaCommand(MetodoPago.EFECTIVO, new BigDecimal("20"), null, null, true));
    }

    @Test
    void registrarPago_devolucionPorEncargado_devuelve403() throws Exception {
        when(service.registrarPago(any(), any())).thenThrow(new ForbiddenOperationException("RN-PAG-07: solo el ADMIN registra devoluciones"));

        mvc.perform(comoEncargado(post(BASE + "/reservas/10/pagos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metodo\":\"EFECTIVO\",\"monto\":20,\"devolucion\":true}")))
                .andExpect(status().isForbidden());
    }

    @Test
    void registrarPago_conCuerpoInvalido_devuelve400() throws Exception {
        mvc.perform(comoEncargado(post(BASE + "/reservas/10/pagos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"monto\":50}"))).andExpect(status().isBadRequest());
        mvc.perform(comoEncargado(post(BASE + "/reservas/10/pagos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"metodo\":\"EFECTIVO\",\"monto\":-5}"))).andExpect(status().isBadRequest());

        verify(service, never()).registrarPago(any(), any());
    }

    @Test
    void registrarPago_numeroDeOperacionRepetido_devuelve409() throws Exception {
        when(service.registrarPago(any(), any())).thenThrow(new ConflictException("RN-PAG-05: el número de operación OP-1 ya fue registrado con YAPE"));

        mvc.perform(comoEncargado(post(BASE + "/reservas/10/pagos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metodo\":\"YAPE\",\"monto\":50,\"nroOperacion\":\"OP-1\"}")))
                .andExpect(status().isConflict());
    }

    @Test
    void registrarPago_montoMayorAlSaldo_devuelve422() throws Exception {
        when(service.registrarPago(any(), any())).thenThrow(new DomainException("RN-PAG-03: el monto supera el saldo pendiente"));

        mvc.perform(comoEncargado(post(BASE + "/reservas/10/pagos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metodo\":\"EFECTIVO\",\"monto\":500}")))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void listarPagos_devuelveLaLista() throws Exception {
        when(service.listarPagos(10L)).thenReturn(List.of(pago(1L, TipoPagoReserva.ADELANTO), pago(2L, TipoPagoReserva.SALDO)));

        mvc.perform(comoEncargado(get(BASE + "/reservas/10/pagos")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Pagos obtenidos"))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void listarPagos_reservaInexistente_devuelve404() throws Exception {
        when(service.listarPagos(99L)).thenThrow(new ReservaNotFoundException(99L));

        mvc.perform(comoEncargado(get(BASE + "/reservas/99/pagos"))).andExpect(status().isNotFound());
    }

    // ================================================================ mantenimientos (ADMIN)

    @Test
    void crearMantenimiento_admin_devuelve201() throws Exception {
        when(service.crearMantenimiento(any(CreateMantenimientoCommand.class))).thenReturn(reserva(11L, EstadoReserva.CONFIRMADA));

        mvc.perform(comoAdmin(post(BASE + "/mantenimientos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"canchaId\":1,\"inicio\":\"2026-10-14T18:00:00\",\"fin\":\"2026-10-14T20:00:00\",\"motivo\":\"Riego\"}")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Mantenimiento registrado"));

        verify(service).crearMantenimiento(new CreateMantenimientoCommand(1L, LocalDateTime.of(2026, 10, 14, 18, 0),
                LocalDateTime.of(2026, 10, 14, 20, 0), "Riego"));
    }

    @Test
    void crearMantenimiento_encargado_devuelve403() throws Exception {
        mvc.perform(comoEncargado(post(BASE + "/mantenimientos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"canchaId\":1,\"inicio\":\"2026-10-14T18:00:00\",\"fin\":\"2026-10-14T20:00:00\",\"motivo\":\"Riego\"}")))
                .andExpect(status().isForbidden());

        verify(service, never()).crearMantenimiento(any());
    }

    @Test
    void crearMantenimiento_sinMotivo_devuelve400() throws Exception {
        mvc.perform(comoAdmin(post(BASE + "/mantenimientos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"canchaId\":1,\"inicio\":\"2026-10-14T18:00:00\",\"fin\":\"2026-10-14T20:00:00\"}")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearMantenimiento_conReservasEnElHorario_devuelve409() throws Exception {
        when(service.crearMantenimiento(any())).thenThrow(new ConflictException("RN-RES-06: el horario se cruza",
                List.of("Reserva #7 [CONFIRMADA] Luis (922222222)")));

        mvc.perform(comoAdmin(post(BASE + "/mantenimientos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"canchaId\":1,\"inicio\":\"2026-10-14T18:00:00\",\"fin\":\"2026-10-14T20:00:00\",\"motivo\":\"Riego\"}")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors[0]").value(org.hamcrest.Matchers.containsString("Luis")));
    }
}
