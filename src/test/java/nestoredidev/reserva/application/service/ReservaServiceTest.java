package nestoredidev.reserva.application.service;

import nestoredidev.cajacancha.application.ports.input.CajaCanchaServicePort;
import nestoredidev.cancha.application.ports.input.CanchaServicePort;
import nestoredidev.cancha.application.ports.input.TarifaServicePort;
import nestoredidev.cancha.application.ports.input.dto.CanchaDto;
import nestoredidev.cancha.application.ports.input.dto.TarifaDto;
import nestoredidev.cancha.domain.exception.CanchaNotFoundException;
import nestoredidev.cliente.application.ports.input.ClienteServicePort;
import nestoredidev.cliente.application.ports.input.dto.ClienteDto;
import nestoredidev.common.application.CurrentUser;
import nestoredidev.common.application.CurrentUserPort;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;
import nestoredidev.common.domain.MetodoPago;
import nestoredidev.common.domain.Rol;
import nestoredidev.common.exception.ConflictException;
import nestoredidev.common.exception.DomainException;
import nestoredidev.common.exception.ForbiddenOperationException;
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
import nestoredidev.reserva.application.ports.output.ReservaPersistencePort;
import nestoredidev.reserva.domain.exception.InvalidReservaException;
import nestoredidev.reserva.domain.exception.ReservaNotFoundException;
import nestoredidev.reserva.domain.model.EstadoReserva;
import nestoredidev.reserva.domain.model.OrigenReserva;
import nestoredidev.reserva.domain.model.PagoReserva;
import nestoredidev.reserva.domain.model.Reserva;
import nestoredidev.reserva.domain.model.TipoPagoReserva;
import nestoredidev.reserva.domain.model.TipoReserva;
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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Hoy en el reloj de prueba: martes 2026-10-13 10:00 (Lima). Tarifa general 06:00-23:00 a S/ 50 la hora, salvo
 * que el test defina otras franjas.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReservaServiceTest {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-13T10:00:00-05:00");
    private static final BigDecimal PORCENTAJE = new BigDecimal("0.50");
    private static final PageQuery PAGE = new PageQuery(0, 10, "inicio", "asc");
    private static final Long CANCHA = 1L;
    private static final Long CLIENTE = 5L;
    private static final Long CAJA = 3L;

    @Mock
    private ReservaPersistencePort persistence;
    @Mock
    private CanchaServicePort canchaService;
    @Mock
    private TarifaServicePort tarifaService;
    @Mock
    private ClienteServicePort clienteService;
    @Mock
    private CajaCanchaServicePort cajaService;
    @Mock
    private CurrentUserPort currentUser;

    private ReservaService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(AHORA.toInstant(), LIMA);
        service = new ReservaService(persistence, canchaService, tarifaService, clienteService, cajaService,
                currentUser, clock, PORCENTAJE);

        when(currentUser.get()).thenReturn(new CurrentUser(9L, "enc", Rol.ENCARGADO));
        when(canchaService.obtener(CANCHA)).thenReturn(cancha(true));
        when(tarifaService.listar(CANCHA, true)).thenReturn(List.of(tarifa(null, "06:00", "23:00", "50.00")));
        when(clienteService.obtener(CLIENTE)).thenReturn(cliente(CLIENTE, "Ana", "911111111"));
        when(clienteService.obtenerPorIds(anyCollection())).thenReturn(List.of());
        when(persistence.buscarCruces(any(), any(), any(), any())).thenReturn(List.of());
        when(persistence.contarPorClienteYEstado(any(), any())).thenReturn(0L);
        when(persistence.pagadoNeto(any())).thenReturn(new BigDecimal("0.00"));
        when(persistence.existePagoConOperacion(any(), any())).thenReturn(false);
        when(persistence.guardar(any(Reserva.class))).thenAnswer(inv -> {
            Reserva r = inv.getArgument(0);
            return r.getId() != null ? r : conId(r, 50L);
        });
        when(persistence.guardarPago(any(PagoReserva.class))).thenAnswer(inv -> {
            PagoReserva p = inv.getArgument(0);
            return PagoReserva.restaurar(70L, p.getReservaId(), p.getCajaId(), p.getTipo(), p.getMetodo(),
                    p.getMonto(), p.getNroOperacion(), p.getVoucherUrl(), p.getRegistradoPor(), AHORA);
        });
        when(cajaService.exigirCajaAbiertaId()).thenReturn(CAJA);
        when(cajaService.efectivoEsperado(CAJA)).thenReturn(new BigDecimal("1000.00"));
    }

    // ---------------------------------------------------------------- helpers

    private static CanchaDto cancha(boolean activa) {
        return new CanchaDto(CANCHA, "Cancha 1", null, activa, AHORA);
    }

    private static TarifaDto tarifa(Integer dia, String desde, String hasta, String precio) {
        return new TarifaDto(1L, CANCHA, dia == null ? null : dia.shortValue(), LocalTime.parse(desde),
                LocalTime.parse(hasta), new BigDecimal(precio), true, AHORA);
    }

    private static ClienteDto cliente(Long id, String nombre, String telefono) {
        return new ClienteDto(id, nombre, telefono, null, AHORA);
    }

    private static OffsetDateTime at(int dia, int hora) {
        return LocalDateTime.of(2026, 10, dia, hora, 0).atZone(LIMA).toOffsetDateTime();
    }

    private static LocalDateTime local(int dia, int hora) {
        return LocalDateTime.of(2026, 10, dia, hora, 0);
    }

    private static Reserva conId(Reserva r, Long id) {
        return Reserva.restaurar(id, r.getCanchaId(), r.getClienteId(), r.getTipo(), r.getEstado(), r.getInicio(),
                r.getFin(), r.getMontoTotal(), r.getFechaLimiteAdelanto(), r.getOrigen(), r.getObservacion(),
                r.getMotivoCancelacion(), r.getCreadoPor(), AHORA);
    }

    private static Reserva reserva(Long id, EstadoReserva estado, OffsetDateTime inicio, OffsetDateTime fin,
                                   String monto) {
        return Reserva.restaurar(id, CANCHA, CLIENTE, TipoReserva.RESERVA, estado, inicio, fin, new BigDecimal(monto),
                estado == EstadoReserva.PENDIENTE ? AHORA.plusHours(2) : null, OrigenReserva.PRESENCIAL, null, null,
                9L, AHORA.minusDays(1));
    }

    /** Reserva de 2 h mañana (14/10) 18:00-20:00 = S/ 100. */
    private static Reserva reservaDeManiana(Long id, EstadoReserva estado) {
        return reserva(id, estado, at(14, 18), at(14, 20), "100.00");
    }

    private static Reserva mantenimiento(Long id) {
        return Reserva.restaurar(id, CANCHA, null, TipoReserva.MANTENIMIENTO, EstadoReserva.CONFIRMADA, at(14, 18),
                at(14, 20), new BigDecimal("0.00"), null, OrigenReserva.PRESENCIAL, "Riego", null, 1L, AHORA);
    }

    private void admin() {
        when(currentUser.get()).thenReturn(new CurrentUser(1L, "admin", Rol.ADMIN));
    }

    private void existe(Reserva r) {
        when(persistence.buscarPorId(r.getId())).thenReturn(Optional.of(r));
        when(persistence.buscarPorIdParaActualizar(r.getId())).thenReturn(Optional.of(r));
    }

    private CreateReservaCommand crearCmd(LocalDateTime inicio, LocalDateTime fin, PagoReservaCommand pago) {
        return new CreateReservaCommand(CANCHA, CLIENTE, null, null, inicio, fin, null, null, pago);
    }

    private static PagoReservaCommand pago(MetodoPago metodo, String monto, String nro, boolean devolucion) {
        return new PagoReservaCommand(metodo, new BigDecimal(monto), nro, null, devolucion);
    }

    private PagoReserva pagoGuardado() {
        ArgumentCaptor<PagoReserva> captor = ArgumentCaptor.forClass(PagoReserva.class);
        verify(persistence).guardarPago(captor.capture());
        return captor.getValue();
    }

    // ================================================================ crear

    @Test
    void crear_reservaValida_nacePendienteConMontoPorHorasYFechaLimiteDeAdelanto() {
        ReservaDto dto = service.crear(crearCmd(local(14, 18), local(14, 20), null));

        assertThat(dto.id()).isEqualTo(50L);
        assertThat(dto.tipo()).isEqualTo(TipoReserva.RESERVA);
        assertThat(dto.estado()).isEqualTo(EstadoReserva.PENDIENTE);
        assertThat(dto.montoTotal()).isEqualByComparingTo("100.00");
        assertThat(dto.adelantoMinimo()).isEqualByComparingTo("50.00");
        assertThat(dto.saldo()).isEqualByComparingTo("100.00");
        assertThat(dto.fechaLimiteAdelanto()).isEqualTo(AHORA.plusHours(2));
        assertThat(dto.origen()).isEqualTo(OrigenReserva.PRESENCIAL);
        assertThat(dto.clienteNombre()).isEqualTo("Ana");
        assertThat(dto.clienteTelefono()).isEqualTo("911111111");
        assertThat(dto.creadoPor()).isEqualTo(9L);
        assertThat(dto.advertencias()).isNull();
        verify(cajaService, never()).exigirCajaAbiertaId();
    }

    @Test
    void crear_reservaParaDentroDeMenosDe2Horas_fechaLimiteEsElInicio() {
        ReservaDto dto = service.crear(crearCmd(local(13, 11), local(13, 12), null));

        assertThat(dto.fechaLimiteAdelanto()).isEqualTo(at(13, 11));
    }

    @Test
    void crear_conNombreYTelefono_reutilizaOCreaElClientePorTelefono() {
        when(clienteService.buscarOCrear("Luis", "922222222")).thenReturn(cliente(6L, "Luis", "922222222"));
        when(clienteService.obtener(6L)).thenReturn(cliente(6L, "Luis", "922222222"));

        ReservaDto dto = service.crear(new CreateReservaCommand(CANCHA, null, "Luis", "922222222",
                local(14, 18), local(14, 19), OrigenReserva.WHATSAPP, "  llamó ", null));

        assertThat(dto.clienteId()).isEqualTo(6L);
        assertThat(dto.origen()).isEqualTo(OrigenReserva.WHATSAPP);
        assertThat(dto.observacion()).isEqualTo("llamó");
        verify(clienteService, never()).obtener(CLIENTE);
    }

    @Test
    void crear_sinClienteIdNiDatosDeCliente_lanzaInvalidReserva() {
        assertThatThrownBy(() -> service.crear(new CreateReservaCommand(CANCHA, null, " ", null,
                local(14, 18), local(14, 19), null, null, null)))
                .isInstanceOf(InvalidReservaException.class)
                .hasMessageContaining("clienteId");

        verify(persistence, never()).guardar(any());
    }

    @Test
    void crear_canchaInactiva_lanzaDomainRNCAN02() {
        when(canchaService.obtener(CANCHA)).thenReturn(cancha(false));

        assertThatThrownBy(() -> service.crear(crearCmd(local(14, 18), local(14, 19), null)))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("RN-CAN-02");

        verify(persistence, never()).guardar(any());
    }

    @Test
    void crear_canchaInexistente_propagaNotFound() {
        when(canchaService.obtener(CANCHA)).thenThrow(new CanchaNotFoundException(CANCHA));

        assertThatThrownBy(() -> service.crear(crearCmd(local(14, 18), local(14, 19), null)))
                .isInstanceOf(CanchaNotFoundException.class);
    }

    @Test
    void crear_horarioQueSeCruza_lanzaConflictConDetalleDeLaReservaYElClienteRNRES06() {
        Reserva existente = Reserva.restaurar(7L, CANCHA, 6L, TipoReserva.RESERVA, EstadoReserva.CONFIRMADA,
                at(14, 18), at(14, 19), new BigDecimal("50.00"), null, OrigenReserva.PRESENCIAL, null, null, 9L, AHORA);
        when(persistence.buscarCruces(eq(CANCHA), eq(at(14, 18)), eq(at(14, 20)), eq(null))).thenReturn(List.of(existente));
        when(clienteService.obtenerPorIds(anyCollection())).thenReturn(List.of(cliente(6L, "Luis", "922222222")));

        assertThatThrownBy(() -> service.crear(crearCmd(local(14, 18), local(14, 20), null)))
                .isInstanceOfSatisfying(ConflictException.class, e -> {
                    assertThat(e.getMessage()).contains("RN-RES-06");
                    assertThat(e.getDetails()).singleElement().asString()
                            .contains("Reserva #7").contains("CONFIRMADA").contains("Luis").contains("922222222");
                });

        verify(persistence, never()).guardar(any());
    }

    @Test
    void crear_cruceConMantenimiento_detalleIndicaMantenimientoSinCliente() {
        when(persistence.buscarCruces(any(), any(), any(), any())).thenReturn(List.of(mantenimiento(8L)));

        assertThatThrownBy(() -> service.crear(crearCmd(local(14, 18), local(14, 19), null)))
                .isInstanceOfSatisfying(ConflictException.class,
                        e -> assertThat(e.getDetails()).singleElement().asString().contains("Mantenimiento #8"));
    }

    @Test
    void crear_clienteConDosInasistencias_incluyeAdvertenciaRNCLI04() {
        when(persistence.contarPorClienteYEstado(CLIENTE, EstadoReserva.NO_ASISTIO)).thenReturn(2L);

        ReservaDto dto = service.crear(crearCmd(local(14, 18), local(14, 19), null));

        assertThat(dto.advertencias()).singleElement().asString().contains("RN-CLI-04").contains("2 inasistencias");
    }

    @Test
    void crear_clienteConUnaInasistencia_sinAdvertencias() {
        when(persistence.contarPorClienteYEstado(CLIENTE, EstadoReserva.NO_ASISTIO)).thenReturn(1L);

        assertThat(service.crear(crearCmd(local(14, 18), local(14, 19), null)).advertencias()).isNull();
    }

    @Test
    void crear_conPagoInicialIgualAlAdelanto_registraAdelantoYQuedaConfirmada() {
        when(persistence.pagadoNeto(any())).thenReturn(new BigDecimal("0.00"), new BigDecimal("50.00"));

        ReservaDto dto = service.crear(crearCmd(local(14, 18), local(14, 20),
                pago(MetodoPago.EFECTIVO, "50", null, false)));

        PagoReserva p = pagoGuardado();
        assertThat(p.getTipo()).isEqualTo(TipoPagoReserva.ADELANTO);
        assertThat(p.getCajaId()).isEqualTo(CAJA);
        assertThat(p.getReservaId()).isEqualTo(50L);
        assertThat(p.getMonto()).isEqualByComparingTo("50.00");
        assertThat(dto.estado()).isEqualTo(EstadoReserva.CONFIRMADA);
        assertThat(dto.pagado()).isEqualByComparingTo("50.00");
    }

    @Test
    void crear_conPagoInicialSinCajaAbierta_propagaDomainRNG05() {
        doThrow(new DomainException("RN-G05: no hay una caja de cancha abierta")).when(cajaService).exigirCajaAbiertaId();

        assertThatThrownBy(() -> service.crear(crearCmd(local(14, 18), local(14, 20),
                pago(MetodoPago.EFECTIVO, "50", null, false))))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("RN-G05");

        verify(persistence, never()).guardarPago(any());
    }

    @Test
    void crear_conPagoInicialDevolucion_lanzaInvalidReserva() {
        assertThatThrownBy(() -> service.crear(crearCmd(local(14, 18), local(14, 20),
                pago(MetodoPago.EFECTIVO, "50", null, true))))
                .isInstanceOf(InvalidReservaException.class)
                .hasMessageContaining("devolución");

        verify(persistence, never()).guardarPago(any());
    }

    @Test
    void crear_horaQueNoEsEnPunto_lanzaInvalidReservaRNRES01() {
        assertThatThrownBy(() -> service.crear(crearCmd(LocalDateTime.of(2026, 10, 14, 18, 30), local(14, 20), null)))
                .isInstanceOf(InvalidReservaException.class)
                .hasMessageContaining("RN-RES-01");
    }

    @Test
    void crear_finNoPosteriorAlInicio_lanzaInvalidReservaRNRES01() {
        assertThatThrownBy(() -> service.crear(crearCmd(local(14, 18), local(14, 18), null)))
                .isInstanceOf(InvalidReservaException.class)
                .hasMessageContaining("RN-RES-01");
    }

    @Test
    void crear_queCruzaLaMedianoche_lanzaInvalidReservaRNRES02() {
        assertThatThrownBy(() -> service.crear(crearCmd(local(14, 22), local(15, 1), null)))
                .isInstanceOf(InvalidReservaException.class)
                .hasMessageContaining("RN-RES-02");
    }

    @Test
    void crear_fueraDelHorarioDeAtencion_lanzaInvalidReservaRNRES02() {
        assertThatThrownBy(() -> service.crear(crearCmd(local(14, 4), local(14, 5), null)))
                .isInstanceOf(InvalidReservaException.class)
                .hasMessageContaining("fuera del horario de atención");
    }

    @Test
    void crear_enElPasado_lanzaInvalidReservaRNRES03() {
        assertThatThrownBy(() -> service.crear(crearCmd(local(13, 8), local(13, 9), null)))
                .isInstanceOf(InvalidReservaException.class)
                .hasMessageContaining("RN-RES-03");
    }

    @Test
    void crear_enLaHoraActual_esValido() {
        assertThat(service.crear(crearCmd(local(13, 10), local(13, 11), null)).estado())
                .isEqualTo(EstadoReserva.PENDIENTE);
    }

    @Test
    void crear_conMasDe30DiasDeAnticipacion_lanzaInvalidReservaRNRES04() {
        assertThatThrownBy(() -> service.crear(crearCmd(LocalDateTime.of(2026, 11, 20, 18, 0),
                LocalDateTime.of(2026, 11, 20, 19, 0), null)))
                .isInstanceOf(InvalidReservaException.class)
                .hasMessageContaining("RN-RES-04");
    }

    @Test
    void crear_tarifaDelDiaEspecificoGanaALaGeneralYSeIgnoranOtrosDias() {
        // 13/10/2026 es martes (2): 80 la hora de 18 a 23; la general es 50; la de miércoles (3) no aplica
        when(tarifaService.listar(CANCHA, true)).thenReturn(List.of(
                tarifa(null, "06:00", "23:00", "50.00"),
                tarifa(2, "18:00", "23:00", "80.00"),
                tarifa(3, "18:00", "23:00", "999.00")));

        assertThat(service.crear(crearCmd(local(13, 18), local(13, 20), null)).montoTotal())
                .isEqualByComparingTo("160.00");
        assertThat(service.crear(crearCmd(local(13, 16), local(13, 18), null)).montoTotal())
                .isEqualByComparingTo("100.00");
    }

    // ================================================================ mantenimiento

    @Test
    void crearMantenimiento_encargado_lanzaForbiddenRNRES20() {
        assertThatThrownBy(() -> service.crearMantenimiento(new CreateMantenimientoCommand(CANCHA, local(14, 18),
                local(14, 20), "Riego")))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("RN-RES-20");

        verify(persistence, never()).guardar(any());
    }

    @Test
    void crearMantenimiento_admin_nacConfirmadoSinClienteNiMonto() {
        admin();

        ReservaDto dto = service.crearMantenimiento(new CreateMantenimientoCommand(CANCHA, local(14, 18),
                local(14, 20), " Riego "));

        assertThat(dto.tipo()).isEqualTo(TipoReserva.MANTENIMIENTO);
        assertThat(dto.estado()).isEqualTo(EstadoReserva.CONFIRMADA);
        assertThat(dto.clienteId()).isNull();
        assertThat(dto.clienteNombre()).isNull();
        assertThat(dto.montoTotal()).isEqualByComparingTo("0.00");
        assertThat(dto.observacion()).isEqualTo("Riego");
        assertThat(dto.fechaLimiteAdelanto()).isNull();
    }

    @Test
    void crearMantenimiento_motivoVacio_lanzaInvalidReservaRNRES20() {
        admin();

        assertThatThrownBy(() -> service.crearMantenimiento(new CreateMantenimientoCommand(CANCHA, local(14, 18),
                local(14, 20), " ")))
                .isInstanceOf(InvalidReservaException.class)
                .hasMessageContaining("RN-RES-20");
    }

    @Test
    void crearMantenimiento_conReservasEnElHorario_lanzaConflictRNRES22ConDetalle() {
        admin();
        Reserva existente = reservaDeManiana(7L, EstadoReserva.CONFIRMADA);
        when(persistence.buscarCruces(any(), any(), any(), any())).thenReturn(List.of(existente));
        when(clienteService.obtenerPorIds(anyCollection())).thenReturn(List.of(cliente(CLIENTE, "Ana", "911111111")));

        assertThatThrownBy(() -> service.crearMantenimiento(new CreateMantenimientoCommand(CANCHA, local(14, 18),
                local(14, 20), "Riego")))
                .isInstanceOfSatisfying(ConflictException.class,
                        e -> assertThat(e.getDetails()).singleElement().asString().contains("Ana").contains("911111111"));
    }

    @Test
    void crearMantenimiento_canchaInactiva_lanzaDomainRNCAN02() {
        admin();
        when(canchaService.obtener(CANCHA)).thenReturn(cancha(false));

        assertThatThrownBy(() -> service.crearMantenimiento(new CreateMantenimientoCommand(CANCHA, local(14, 18),
                local(14, 20), "Riego")))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("RN-CAN-02");
    }

    // ================================================================ consultas

    @Test
    void obtener_existente_devuelveDtoConPagadoYSaldo() {
        existe(reservaDeManiana(10L, EstadoReserva.CONFIRMADA));
        when(persistence.pagadoNeto(10L)).thenReturn(new BigDecimal("60.00"));

        ReservaDto dto = service.obtener(10L);

        assertThat(dto.pagado()).isEqualByComparingTo("60.00");
        assertThat(dto.saldo()).isEqualByComparingTo("40.00");
        assertThat(dto.clienteNombre()).isEqualTo("Ana");
    }

    @Test
    void obtener_inexistente_lanzaNotFound() {
        when(persistence.buscarPorId(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(10L)).isInstanceOf(ReservaNotFoundException.class);
    }

    @Test
    void listar_armaDtosConPagadoNetoYClienteSinConsultarUnoPorUno() {
        ReservaFiltro filtro = new ReservaFiltro(CANCHA, null, null, null, null, null);
        Reserva r1 = reservaDeManiana(10L, EstadoReserva.CONFIRMADA);
        Reserva r2 = mantenimiento(11L);
        when(persistence.listar(filtro, PAGE)).thenReturn(new PageResult<>(List.of(r1, r2), 0, 10, 2, 1));
        when(persistence.pagadoNetoPorReservas(anyCollection())).thenReturn(Map.of(10L, new BigDecimal("30.00")));
        when(clienteService.obtenerPorIds(anyCollection())).thenReturn(List.of(cliente(CLIENTE, "Ana", "911111111")));

        PageResult<ReservaDto> res = service.listar(filtro, PAGE);

        assertThat(res.totalElements()).isEqualTo(2);
        ReservaDto d1 = res.content().get(0);
        assertThat(d1.pagado()).isEqualByComparingTo("30.00");
        assertThat(d1.saldo()).isEqualByComparingTo("70.00");
        assertThat(d1.clienteNombre()).isEqualTo("Ana");
        ReservaDto d2 = res.content().get(1);
        assertThat(d2.pagado()).isEqualByComparingTo("0.00");
        assertThat(d2.clienteNombre()).isNull();
        verify(clienteService, never()).obtener(any());
    }

    // ================================================================ disponibilidad

    @Test
    void disponibilidad_diaFuturoSinReservas_tieneUnSlotLibrePorHoraDeAtencion() {
        List<SlotDto> slots = service.disponibilidad(CANCHA, LocalDate.of(2026, 10, 14));

        assertThat(slots).hasSize(17); // 06:00 .. 22:00 (la última hora termina a las 23:00)
        assertThat(slots.get(0).horaInicio()).isEqualTo(LocalTime.of(6, 0));
        assertThat(slots.get(16).horaFin()).isEqualTo(LocalTime.of(23, 0));
        assertThat(slots).allMatch(SlotDto::disponible);
        assertThat(slots).allMatch(s -> s.precioHora().compareTo(new BigDecimal("50.00")) == 0);
    }

    @Test
    void disponibilidad_horasOcupadasQuedanNoDisponibles() {
        when(persistence.buscarCruces(eq(CANCHA), any(), any(), eq(null)))
                .thenReturn(List.of(reservaDeManiana(10L, EstadoReserva.CONFIRMADA)));

        List<SlotDto> slots = service.disponibilidad(CANCHA, LocalDate.of(2026, 10, 14));

        assertThat(slots).filteredOn(s -> !s.disponible()).extracting(SlotDto::horaInicio)
                .containsExactly(LocalTime.of(18, 0), LocalTime.of(19, 0));
    }

    @Test
    void disponibilidad_hoy_lasHorasPasadasNoEstanDisponiblesPeroLaActualSi() {
        List<SlotDto> slots = service.disponibilidad(CANCHA, LocalDate.of(2026, 10, 13));

        assertThat(slots).filteredOn(s -> s.horaInicio().isBefore(LocalTime.of(10, 0)))
                .isNotEmpty().noneMatch(SlotDto::disponible);
        assertThat(slots).filteredOn(s -> !s.horaInicio().isBefore(LocalTime.of(10, 0))).allMatch(SlotDto::disponible);
    }

    @Test
    void disponibilidad_canchaInactiva_ningunSlotDisponible() {
        when(canchaService.obtener(CANCHA)).thenReturn(cancha(false));

        assertThat(service.disponibilidad(CANCHA, LocalDate.of(2026, 10, 14))).isNotEmpty().noneMatch(SlotDto::disponible);
    }

    @Test
    void disponibilidad_sinTarifas_devuelveListaVacia() {
        when(tarifaService.listar(CANCHA, true)).thenReturn(List.of());

        assertThat(service.disponibilidad(CANCHA, LocalDate.of(2026, 10, 14))).isEmpty();
    }

    // ================================================================ reprogramar

    @Test
    void reprogramar_pendienteSinPagos_actualizaHorarioYRecalculaMonto() {
        existe(reservaDeManiana(10L, EstadoReserva.PENDIENTE));

        ReservaDto dto = service.reprogramar(10L, new ReprogramarCommand(local(15, 18), local(15, 19)));

        assertThat(dto.inicio()).isEqualTo(at(15, 18));
        assertThat(dto.fin()).isEqualTo(at(15, 19));
        assertThat(dto.montoTotal()).isEqualByComparingTo("50.00");
        assertThat(dto.estado()).isEqualTo(EstadoReserva.PENDIENTE);
        verify(persistence, never()).guardarPago(any());
        verify(persistence).buscarCruces(CANCHA, at(15, 18), at(15, 19), 10L);
    }

    @Test
    void reprogramar_aUnHorarioMasBarato_devuelveElExcesoEnEfectivoYMantieneConfirmada() {
        existe(reservaDeManiana(10L, EstadoReserva.CONFIRMADA));
        when(persistence.pagadoNeto(10L)).thenReturn(new BigDecimal("100.00"));

        ReservaDto dto = service.reprogramar(10L, new ReprogramarCommand(local(15, 18), local(15, 19)));

        PagoReserva devolucion = pagoGuardado();
        assertThat(devolucion.getTipo()).isEqualTo(TipoPagoReserva.DEVOLUCION);
        assertThat(devolucion.getMetodo()).isEqualTo(MetodoPago.EFECTIVO);
        assertThat(devolucion.getMonto()).isEqualByComparingTo("50.00");
        assertThat(devolucion.getCajaId()).isEqualTo(CAJA);
        assertThat(dto.estado()).isEqualTo(EstadoReserva.CONFIRMADA);
    }

    @Test
    void reprogramar_excesoMayorAlEfectivoEnCaja_lanzaDomainRNMCA03() {
        existe(reservaDeManiana(10L, EstadoReserva.CONFIRMADA));
        when(persistence.pagadoNeto(10L)).thenReturn(new BigDecimal("100.00"));
        when(cajaService.efectivoEsperado(CAJA)).thenReturn(new BigDecimal("20.00"));

        assertThatThrownBy(() -> service.reprogramar(10L, new ReprogramarCommand(local(15, 18), local(15, 19))))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("RN-MCA-03");

        verify(persistence, never()).guardarPago(any());
        verify(persistence, never()).guardar(any());
    }

    @Test
    void reprogramar_aUnHorarioMasCaro_noDevuelveNada() {
        existe(reservaDeManiana(10L, EstadoReserva.CONFIRMADA));
        when(persistence.pagadoNeto(10L)).thenReturn(new BigDecimal("100.00"));

        ReservaDto dto = service.reprogramar(10L, new ReprogramarCommand(local(15, 18), local(15, 21)));

        assertThat(dto.montoTotal()).isEqualByComparingTo("150.00");
        verify(persistence, never()).guardarPago(any());
        verify(cajaService, never()).exigirCajaAbiertaId();
    }

    @Test
    void reprogramar_cuyoNuevoHorarioSeCruza_lanzaConflictExcluyendoLaPropiaReserva() {
        existe(reservaDeManiana(10L, EstadoReserva.PENDIENTE));
        when(persistence.buscarCruces(CANCHA, at(15, 18), at(15, 19), 10L))
                .thenReturn(List.of(reserva(11L, EstadoReserva.CONFIRMADA, at(15, 18), at(15, 19), "50.00")));

        assertThatThrownBy(() -> service.reprogramar(10L, new ReprogramarCommand(local(15, 18), local(15, 19))))
                .isInstanceOf(ConflictException.class);

        verify(persistence, never()).guardar(any());
    }

    @Test
    void reprogramar_reservaEnEstadoFinal_lanzaInvalidReservaRNRES17() {
        existe(reservaDeManiana(10L, EstadoReserva.CANCELADA));

        assertThatThrownBy(() -> service.reprogramar(10L, new ReprogramarCommand(local(15, 18), local(15, 19))))
                .isInstanceOf(InvalidReservaException.class)
                .hasMessageContaining("RN-RES-17");
    }

    @Test
    void reprogramar_canchaInactiva_lanzaDomainRNCAN02() {
        existe(reservaDeManiana(10L, EstadoReserva.PENDIENTE));
        when(canchaService.obtener(CANCHA)).thenReturn(cancha(false));

        assertThatThrownBy(() -> service.reprogramar(10L, new ReprogramarCommand(local(15, 18), local(15, 19))))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("RN-CAN-02");
    }

    @Test
    void reprogramar_inexistente_lanzaNotFound() {
        when(persistence.buscarPorIdParaActualizar(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reprogramar(10L, new ReprogramarCommand(local(15, 18), local(15, 19))))
                .isInstanceOf(ReservaNotFoundException.class);
    }

    @Test
    void reprogramar_mantenimiento_cambiaElHorarioSinPagosNiDevolucion() {
        existe(mantenimiento(11L));

        ReservaDto dto = service.reprogramar(11L, new ReprogramarCommand(local(15, 18), local(15, 19)));

        assertThat(dto.inicio()).isEqualTo(at(15, 18));
        assertThat(dto.montoTotal()).isEqualByComparingTo("0.00");
        verify(persistence, never()).guardarPago(any());
    }

    // ================================================================ cancelar

    @Test
    void cancelar_con24HorasOMasDeAnticipacion_sugiereDevolverLoPagado() {
        existe(reservaDeManiana(10L, EstadoReserva.CONFIRMADA)); // empieza mañana 18:00 (32 h)
        when(persistence.pagadoNeto(10L)).thenReturn(new BigDecimal("50.00"));

        ReservaDto dto = service.cancelar(10L, new CancelarReservaCommand("  Lluvia "));

        assertThat(dto.estado()).isEqualTo(EstadoReserva.CANCELADA);
        assertThat(dto.motivoCancelacion()).isEqualTo("Lluvia");
        assertThat(dto.devolucionSugerida()).isEqualByComparingTo("50.00");
        verify(persistence).guardar(any(Reserva.class));
    }

    @Test
    void cancelar_conMenosDe24Horas_noSugiereDevolucion() {
        existe(reserva(10L, EstadoReserva.CONFIRMADA, at(14, 9), at(14, 10), "50.00")); // en 23 h
        when(persistence.pagadoNeto(10L)).thenReturn(new BigDecimal("25.00"));

        ReservaDto dto = service.cancelar(10L, new CancelarReservaCommand("Cliente avisó tarde"));

        assertThat(dto.devolucionSugerida()).isEqualByComparingTo("0.00");
    }

    @Test
    void cancelar_exactamente24HorasAntes_aunSugiereDevolucion() {
        existe(reserva(10L, EstadoReserva.CONFIRMADA, at(14, 10), at(14, 11), "50.00"));
        when(persistence.pagadoNeto(10L)).thenReturn(new BigDecimal("25.00"));

        assertThat(service.cancelar(10L, new CancelarReservaCommand("x")).devolucionSugerida())
                .isEqualByComparingTo("25.00");
    }

    @Test
    void cancelar_mantenimientoPorEncargado_lanzaForbiddenRNRES23() {
        existe(mantenimiento(11L));

        assertThatThrownBy(() -> service.cancelar(11L, new CancelarReservaCommand("x")))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("RN-RES-23");

        verify(persistence, never()).guardar(any());
    }

    @Test
    void cancelar_mantenimientoPorAdmin_cancelaSinDevolucionSugerida() {
        admin();
        existe(mantenimiento(11L));

        ReservaDto dto = service.cancelar(11L, new CancelarReservaCommand("Se suspendió"));

        assertThat(dto.estado()).isEqualTo(EstadoReserva.CANCELADA);
        assertThat(dto.devolucionSugerida()).isNull();
    }

    @Test
    void cancelar_sinMotivo_lanzaInvalidReservaRNRES15() {
        existe(reservaDeManiana(10L, EstadoReserva.PENDIENTE));

        assertThatThrownBy(() -> service.cancelar(10L, new CancelarReservaCommand(" ")))
                .isInstanceOf(InvalidReservaException.class)
                .hasMessageContaining("RN-RES-15");

        verify(persistence, never()).guardar(any());
    }

    @Test
    void cancelar_yaCompletada_lanzaInvalidReservaRNRES15() {
        existe(reservaDeManiana(10L, EstadoReserva.COMPLETADA));

        assertThatThrownBy(() -> service.cancelar(10L, new CancelarReservaCommand("x")))
                .isInstanceOf(InvalidReservaException.class)
                .hasMessageContaining("RN-RES-15");
    }

    // ================================================================ no asistió / completar

    @Test
    void marcarNoAsistio_confirmadaPasadosLos15Minutos_cambiaDeEstado() {
        existe(reserva(10L, EstadoReserva.CONFIRMADA, at(13, 9), at(13, 10), "50.00"));

        assertThat(service.marcarNoAsistio(10L).estado()).isEqualTo(EstadoReserva.NO_ASISTIO);
    }

    @Test
    void marcarNoAsistio_antesDeLos15Minutos_lanzaInvalidReservaRNRES18() {
        existe(reserva(10L, EstadoReserva.CONFIRMADA, AHORA.minusMinutes(10), AHORA.plusMinutes(50), "50.00"));

        assertThatThrownBy(() -> service.marcarNoAsistio(10L))
                .isInstanceOf(InvalidReservaException.class)
                .hasMessageContaining("15 minutos");
    }

    @Test
    void marcarNoAsistio_reservaPendiente_lanzaInvalidReservaRNRES18() {
        existe(reserva(10L, EstadoReserva.PENDIENTE, at(13, 8), at(13, 9), "50.00"));

        assertThatThrownBy(() -> service.marcarNoAsistio(10L))
                .isInstanceOf(InvalidReservaException.class)
                .hasMessageContaining("RN-RES-18");
    }

    @Test
    void completar_confirmadaPagadaYYaEmpezada_pasaACompletada() {
        existe(reserva(10L, EstadoReserva.CONFIRMADA, at(13, 9), at(13, 10), "50.00"));
        when(persistence.pagadoNeto(10L)).thenReturn(new BigDecimal("50.00"));

        assertThat(service.completar(10L).estado()).isEqualTo(EstadoReserva.COMPLETADA);
    }

    @Test
    void completar_conDeuda_lanzaInvalidReservaRNRES19() {
        existe(reserva(10L, EstadoReserva.CONFIRMADA, at(13, 9), at(13, 10), "50.00"));
        when(persistence.pagadoNeto(10L)).thenReturn(new BigDecimal("20.00"));

        assertThatThrownBy(() -> service.completar(10L))
                .isInstanceOf(InvalidReservaException.class)
                .hasMessageContaining("no se completa con deuda");

        verify(persistence, never()).guardar(any());
    }

    @Test
    void completar_antesDeLaHoraDeInicio_lanzaInvalidReservaRNRES19() {
        existe(reservaDeManiana(10L, EstadoReserva.CONFIRMADA));
        when(persistence.pagadoNeto(10L)).thenReturn(new BigDecimal("100.00"));

        assertThatThrownBy(() -> service.completar(10L))
                .isInstanceOf(InvalidReservaException.class)
                .hasMessageContaining("todavía no empieza");
    }

    // ================================================================ cambiar monto

    @Test
    void cambiarMonto_encargado_lanzaForbiddenRNRES10SinBuscarLaReserva() {
        assertThatThrownBy(() -> service.cambiarMonto(10L, new CambiarMontoCommand(new BigDecimal("80"), "Descuento")))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("RN-RES-10");

        verify(persistence, never()).buscarPorIdParaActualizar(any());
    }

    @Test
    void cambiarMonto_adminQueBajaElMontoHastaCubrirElAdelanto_confirmaLaReserva() {
        admin();
        existe(reservaDeManiana(10L, EstadoReserva.PENDIENTE));
        when(persistence.pagadoNeto(10L)).thenReturn(new BigDecimal("40.00"));

        ReservaDto dto = service.cambiarMonto(10L, new CambiarMontoCommand(new BigDecimal("80"), "  Descuento "));

        assertThat(dto.montoTotal()).isEqualByComparingTo("80.00");
        assertThat(dto.observacion()).isEqualTo("Descuento");
        assertThat(dto.estado()).isEqualTo(EstadoReserva.CONFIRMADA); // 40 >= 50% de 80
    }

    @Test
    void cambiarMonto_adminQueDejaLaReservaYaEmpezadaTotalmentePagada_laCompleta() {
        admin();
        existe(reserva(10L, EstadoReserva.CONFIRMADA, at(13, 9), at(13, 10), "100.00"));
        when(persistence.pagadoNeto(10L)).thenReturn(new BigDecimal("80.00"));

        assertThat(service.cambiarMonto(10L, new CambiarMontoCommand(new BigDecimal("80"), "Descuento")).estado())
                .isEqualTo(EstadoReserva.COMPLETADA);
    }

    @Test
    void cambiarMonto_menorAlYaPagado_lanzaInvalidReservaRNPAG03() {
        admin();
        existe(reservaDeManiana(10L, EstadoReserva.CONFIRMADA));
        when(persistence.pagadoNeto(10L)).thenReturn(new BigDecimal("70.00"));

        assertThatThrownBy(() -> service.cambiarMonto(10L, new CambiarMontoCommand(new BigDecimal("60"), "x")))
                .isInstanceOf(InvalidReservaException.class)
                .hasMessageContaining("RN-PAG-03");

        verify(persistence, never()).guardar(any());
    }

    @Test
    void cambiarMonto_sinObservacion_lanzaInvalidReservaRNRES10() {
        admin();
        existe(reservaDeManiana(10L, EstadoReserva.PENDIENTE));

        assertThatThrownBy(() -> service.cambiarMonto(10L, new CambiarMontoCommand(new BigDecimal("60"), " ")))
                .isInstanceOf(InvalidReservaException.class)
                .hasMessageContaining("RN-RES-10");
    }

    @Test
    void cambiarMonto_reservaEnEstadoFinal_lanzaInvalidReservaRNRES12() {
        admin();
        existe(reservaDeManiana(10L, EstadoReserva.CANCELADA));

        assertThatThrownBy(() -> service.cambiarMonto(10L, new CambiarMontoCommand(new BigDecimal("60"), "x")))
                .isInstanceOf(InvalidReservaException.class)
                .hasMessageContaining("RN-RES-12");
    }

    // ================================================================ pagos

    @Test
    void registrarPago_adelantoParcial_quedaPendiente() {
        existe(reservaDeManiana(10L, EstadoReserva.PENDIENTE));

        PagoReservaDto dto = service.registrarPago(10L, pago(MetodoPago.EFECTIVO, "30", null, false));

        assertThat(dto.tipo()).isEqualTo(TipoPagoReserva.ADELANTO);
        assertThat(dto.cajaId()).isEqualTo(CAJA);
        assertThat(dto.registradoPor()).isEqualTo(9L);
        assertThat(dto.monto()).isEqualByComparingTo("30.00");
        ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
        verify(persistence).guardar(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoReserva.PENDIENTE);
    }

    @Test
    void registrarPago_quealcanzaElAdelantoMinimo_confirmaLaReserva() {
        existe(reservaDeManiana(10L, EstadoReserva.PENDIENTE));
        when(persistence.pagadoNeto(10L)).thenReturn(new BigDecimal("20.00"));

        service.registrarPago(10L, pago(MetodoPago.EFECTIVO, "30", null, false));

        ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
        verify(persistence).guardar(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
    }

    @Test
    void registrarPago_porElSaldoExacto_esTipoSaldoYCompletaSiLaHoraYaEmpezo() {
        existe(reserva(10L, EstadoReserva.CONFIRMADA, at(13, 9), at(13, 10), "50.00"));
        when(persistence.pagadoNeto(10L)).thenReturn(new BigDecimal("20.00"));

        PagoReservaDto dto = service.registrarPago(10L, pago(MetodoPago.EFECTIVO, "30", null, false));

        assertThat(dto.tipo()).isEqualTo(TipoPagoReserva.SALDO);
        ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
        verify(persistence).guardar(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoReserva.COMPLETADA);
    }

    @Test
    void registrarPago_porElSaldoExactoAntesDeLaHora_seguiraConfirmada() {
        existe(reservaDeManiana(10L, EstadoReserva.CONFIRMADA));
        when(persistence.pagadoNeto(10L)).thenReturn(new BigDecimal("60.00"));

        service.registrarPago(10L, pago(MetodoPago.EFECTIVO, "40", null, false));

        ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
        verify(persistence).guardar(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
    }

    @Test
    void registrarPago_mayorAlSaldo_lanzaDomainRNPAG03() {
        existe(reservaDeManiana(10L, EstadoReserva.PENDIENTE));
        when(persistence.pagadoNeto(10L)).thenReturn(new BigDecimal("60.00"));

        assertThatThrownBy(() -> service.registrarPago(10L, pago(MetodoPago.EFECTIVO, "40.01", null, false)))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("RN-PAG-03");

        verify(persistence, never()).guardarPago(any());
    }

    @Test
    void registrarPago_reservaYaPagadaPorCompleto_lanzaDomain() {
        existe(reservaDeManiana(10L, EstadoReserva.CONFIRMADA));
        when(persistence.pagadoNeto(10L)).thenReturn(new BigDecimal("100.00"));

        assertThatThrownBy(() -> service.registrarPago(10L, pago(MetodoPago.EFECTIVO, "1", null, false)))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("pagada por completo");
    }

    @Test
    void registrarPago_enMantenimiento_lanzaDomainRNPAG02() {
        existe(mantenimiento(11L));

        assertThatThrownBy(() -> service.registrarPago(11L, pago(MetodoPago.EFECTIVO, "10", null, false)))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("RN-PAG-02");

        verify(cajaService, never()).exigirCajaAbiertaId();
    }

    @Test
    void registrarPago_enReservaCancelada_lanzaDomainRNPAG02() {
        existe(reservaDeManiana(10L, EstadoReserva.CANCELADA));

        assertThatThrownBy(() -> service.registrarPago(10L, pago(MetodoPago.EFECTIVO, "10", null, false)))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("RN-PAG-02");
    }

    @Test
    void registrarPago_sinCajaAbierta_propagaDomainRNG05() {
        existe(reservaDeManiana(10L, EstadoReserva.PENDIENTE));
        doThrow(new DomainException("RN-G05: no hay una caja de cancha abierta")).when(cajaService).exigirCajaAbiertaId();

        assertThatThrownBy(() -> service.registrarPago(10L, pago(MetodoPago.EFECTIVO, "10", null, false)))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("RN-G05");
    }

    @Test
    void registrarPago_digitalSinNumeroDeOperacion_lanzaInvalidReservaRNPAG05() {
        existe(reservaDeManiana(10L, EstadoReserva.PENDIENTE));

        assertThatThrownBy(() -> service.registrarPago(10L, pago(MetodoPago.YAPE, "10", " ", false)))
                .isInstanceOf(InvalidReservaException.class)
                .hasMessageContaining("RN-PAG-05");
    }

    @Test
    void registrarPago_numeroDeOperacionYaUsado_lanzaConflictRNPAG05() {
        existe(reservaDeManiana(10L, EstadoReserva.PENDIENTE));
        when(persistence.existePagoConOperacion(MetodoPago.YAPE, "OP-1")).thenReturn(true);

        assertThatThrownBy(() -> service.registrarPago(10L, pago(MetodoPago.YAPE, "10", "OP-1", false)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("RN-PAG-05");

        verify(persistence, never()).guardarPago(any());
    }

    @Test
    void registrarPago_digitalConNumeroDeOperacion_seGuardaConElNumero() {
        existe(reservaDeManiana(10L, EstadoReserva.PENDIENTE));

        PagoReservaDto dto = service.registrarPago(10L, pago(MetodoPago.YAPE, "10", " OP-1 ", false));

        assertThat(dto.nroOperacion()).isEqualTo("OP-1");
        assertThat(dto.metodo()).isEqualTo(MetodoPago.YAPE);
    }

    @Test
    void registrarPago_reservaInexistente_lanzaNotFound() {
        when(persistence.buscarPorIdParaActualizar(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrarPago(10L, pago(MetodoPago.EFECTIVO, "10", null, false)))
                .isInstanceOf(ReservaNotFoundException.class);
    }

    // ---------------------------------------------------------------- devoluciones

    @Test
    void registrarPago_devolucionPorEncargado_lanzaForbiddenRNPAG07() {
        existe(reservaDeManiana(10L, EstadoReserva.CANCELADA));

        assertThatThrownBy(() -> service.registrarPago(10L, pago(MetodoPago.EFECTIVO, "10", null, true)))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("RN-PAG-07");
    }

    @Test
    void registrarPago_devolucionEnReservaCanceladaPorAdmin_registraDevolucionSinCambiarEstado() {
        admin();
        existe(reservaDeManiana(10L, EstadoReserva.CANCELADA));
        when(persistence.pagadoNeto(10L)).thenReturn(new BigDecimal("50.00"));

        PagoReservaDto dto = service.registrarPago(10L, pago(MetodoPago.EFECTIVO, "50", null, true));

        assertThat(dto.tipo()).isEqualTo(TipoPagoReserva.DEVOLUCION);
        assertThat(dto.registradoPor()).isEqualTo(1L);
        verify(persistence, never()).guardar(any(Reserva.class));
    }

    @Test
    void registrarPago_devolucionEnReservaActivaSinExceso_lanzaDomainRNPAG07() {
        admin();
        existe(reservaDeManiana(10L, EstadoReserva.CONFIRMADA));
        when(persistence.pagadoNeto(10L)).thenReturn(new BigDecimal("100.00"));

        assertThatThrownBy(() -> service.registrarPago(10L, pago(MetodoPago.EFECTIVO, "10", null, true)))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("canceladas o con pago en exceso");
    }

    @Test
    void registrarPago_devolucionDelExcesoEnReservaActiva_esValida() {
        admin();
        existe(reservaDeManiana(10L, EstadoReserva.CONFIRMADA));
        when(persistence.pagadoNeto(10L)).thenReturn(new BigDecimal("150.00"));

        assertThat(service.registrarPago(10L, pago(MetodoPago.EFECTIVO, "50", null, true)).tipo())
                .isEqualTo(TipoPagoReserva.DEVOLUCION);
    }

    @Test
    void registrarPago_devolucionMayorALoPagado_lanzaDomainRNPAG07() {
        admin();
        existe(reservaDeManiana(10L, EstadoReserva.CANCELADA));
        when(persistence.pagadoNeto(10L)).thenReturn(new BigDecimal("50.00"));

        assertThatThrownBy(() -> service.registrarPago(10L, pago(MetodoPago.EFECTIVO, "50.01", null, true)))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("no puede superar lo pagado");
    }

    @Test
    void registrarPago_devolucionEnEfectivoSinEfectivoSuficiente_lanzaDomainRNMCA03() {
        admin();
        existe(reservaDeManiana(10L, EstadoReserva.CANCELADA));
        when(persistence.pagadoNeto(10L)).thenReturn(new BigDecimal("50.00"));
        when(cajaService.efectivoEsperado(CAJA)).thenReturn(new BigDecimal("10.00"));

        assertThatThrownBy(() -> service.registrarPago(10L, pago(MetodoPago.EFECTIVO, "50", null, true)))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("RN-MCA-03");

        verify(persistence, never()).guardarPago(any());
    }

    @Test
    void registrarPago_devolucionDigital_noValidaElEfectivoDeCaja() {
        admin();
        existe(reservaDeManiana(10L, EstadoReserva.CANCELADA));
        when(persistence.pagadoNeto(10L)).thenReturn(new BigDecimal("50.00"));
        when(cajaService.efectivoEsperado(CAJA)).thenReturn(new BigDecimal("0.00"));

        PagoReservaDto dto = service.registrarPago(10L, pago(MetodoPago.YAPE, "50", "DEV-1", true));

        assertThat(dto.tipo()).isEqualTo(TipoPagoReserva.DEVOLUCION);
        verify(cajaService, never()).efectivoEsperado(any());
    }

    // ---------------------------------------------------------------- listar pagos

    @Test
    void listarPagos_reservaExistente_mapeaLosPagos() {
        existe(reservaDeManiana(10L, EstadoReserva.CONFIRMADA));
        when(persistence.listarPagos(10L)).thenReturn(List.of(PagoReserva.restaurar(1L, 10L, CAJA,
                TipoPagoReserva.ADELANTO, MetodoPago.EFECTIVO, new BigDecimal("50.00"), null, null, 9L, AHORA)));

        assertThat(service.listarPagos(10L)).extracting(PagoReservaDto::id).containsExactly(1L);
    }

    @Test
    void listarPagos_reservaInexistente_lanzaNotFound() {
        when(persistence.buscarPorId(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listarPagos(10L)).isInstanceOf(ReservaNotFoundException.class);
    }

    // ================================================================ cancelarVencidas

    @Test
    void cancelarVencidas_cancelaSoloLasQueSiguenVencidasTrasBloquearLaFila() {
        Reserva vencida = reserva(10L, EstadoReserva.PENDIENTE, at(13, 18), at(13, 19), "50.00");
        Reserva yaConfirmada = reserva(11L, EstadoReserva.CONFIRMADA, at(13, 18), at(13, 19), "50.00");
        Reserva desaparecida = reserva(12L, EstadoReserva.PENDIENTE, at(13, 18), at(13, 19), "50.00");
        when(persistence.buscarPendientesConAdelantoVencido(any()))
                .thenReturn(List.of(vencida, yaConfirmada, desaparecida));
        // la fila bloqueada refleja el estado más reciente: la primera sigue PENDIENTE y vencida
        Reserva vencidaBloqueada = Reserva.restaurar(10L, CANCHA, CLIENTE, TipoReserva.RESERVA,
                EstadoReserva.PENDIENTE, at(13, 18), at(13, 19), new BigDecimal("50.00"), AHORA.minusHours(1),
                OrigenReserva.PRESENCIAL, null, null, 9L, AHORA);
        when(persistence.buscarPorIdParaActualizar(10L)).thenReturn(Optional.of(vencidaBloqueada));
        when(persistence.buscarPorIdParaActualizar(11L)).thenReturn(Optional.of(yaConfirmada));
        when(persistence.buscarPorIdParaActualizar(12L)).thenReturn(Optional.empty());

        int canceladas = service.cancelarVencidas();

        assertThat(canceladas).isEqualTo(1);
        ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
        verify(persistence).guardar(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(10L);
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoReserva.CANCELADA);
        assertThat(captor.getValue().getMotivoCancelacion()).isEqualTo("Adelanto no pagado");
    }

    @Test
    void cancelarVencidas_sinCandidatas_devuelveCero() {
        when(persistence.buscarPendientesConAdelantoVencido(any())).thenReturn(List.of());

        assertThat(service.cancelarVencidas()).isZero();

        verify(persistence, never()).guardar(any());
    }
}
