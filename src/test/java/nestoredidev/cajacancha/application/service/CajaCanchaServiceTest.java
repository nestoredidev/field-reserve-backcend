package nestoredidev.cajacancha.application.service;

import nestoredidev.cajacancha.application.ports.input.dto.AbrirCajaCommand;
import nestoredidev.cajacancha.application.ports.input.dto.CajaCanchaDto;
import nestoredidev.cajacancha.application.ports.input.dto.CerrarCajaCommand;
import nestoredidev.cajacancha.application.ports.input.dto.MovimientoCajaCommand;
import nestoredidev.cajacancha.application.ports.input.dto.MovimientoCajaDto;
import nestoredidev.cajacancha.application.ports.output.CajaCanchaPersistencePort;
import nestoredidev.cajacancha.domain.exception.CajaCanchaNotFoundException;
import nestoredidev.cajacancha.domain.exception.InvalidCajaCanchaException;
import nestoredidev.cajacancha.domain.model.CajaCancha;
import nestoredidev.cajacancha.domain.model.CajaResumen;
import nestoredidev.cajacancha.domain.model.MovimientoCaja;
import nestoredidev.common.application.CurrentUser;
import nestoredidev.common.application.CurrentUserPort;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;
import nestoredidev.common.domain.EstadoCaja;
import nestoredidev.common.domain.MetodoPago;
import nestoredidev.common.domain.Rol;
import nestoredidev.common.domain.TipoMovimientoCaja;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CajaCanchaServiceTest {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-13T10:00:00-05:00");
    private static final PageQuery PAGE = new PageQuery(0, 10, "id", "desc");

    @Mock
    private CajaCanchaPersistencePort persistence;
    @Mock
    private CurrentUserPort currentUser;

    private CajaCanchaService service;

    @BeforeEach
    void setUp() {
        service = new CajaCanchaService(persistence, currentUser, Clock.fixed(AHORA.toInstant(), LIMA));
        when(currentUser.get()).thenReturn(new CurrentUser(7L, "enc", Rol.ENCARGADO));
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    private static CajaResumen resumen(String esperado) {
        return new CajaResumen(bd("0.00"), bd("0.00"), bd("0.00"), bd("0.00"), bd(esperado), bd("0.00"));
    }

    private static CajaCancha cajaAbierta(Long id, LocalDate fecha) {
        return CajaCancha.restaurar(id, fecha, bd("50.00"), null, EstadoCaja.ABIERTA, null, 7L, null,
                AHORA.minusHours(2), null);
    }

    // ---------- abrir ----------

    @Test
    void abrir_sinCajaAbierta_guardaConUsuarioYFechaDeHoy() {
        when(persistence.buscarAbierta()).thenReturn(Optional.empty());
        when(persistence.guardar(any(CajaCancha.class))).thenAnswer(inv -> {
            CajaCancha c = inv.getArgument(0);
            return CajaCancha.restaurar(1L, c.getFecha(), c.getMontoApertura(), null, c.getEstado(), null,
                    c.getAbiertaPor(), null, c.getAbiertaAt(), null);
        });
        when(persistence.resumen(1L)).thenReturn(resumen("50.00"));

        CajaCanchaDto dto = service.abrir(new AbrirCajaCommand(bd("50")));

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.estado()).isEqualTo(EstadoCaja.ABIERTA);
        assertThat(dto.fecha()).isEqualTo(LocalDate.of(2026, 10, 13));
        assertThat(dto.montoApertura()).isEqualByComparingTo("50.00");
        assertThat(dto.abiertaPor()).isEqualTo(7L);
        assertThat(dto.advertencia()).isNull();
    }

    @Test
    void abrir_montoCero_esValidoRNCAJ02() {
        when(persistence.buscarAbierta()).thenReturn(Optional.empty());
        when(persistence.guardar(any(CajaCancha.class))).thenAnswer(inv -> inv.getArgument(0));
        when(persistence.resumen(any())).thenReturn(resumen("0.00"));

        assertThat(service.abrir(new AbrirCajaCommand(BigDecimal.ZERO)).montoApertura()).isEqualByComparingTo("0");
    }

    @Test
    void abrir_yaExisteCajaAbierta_lanzaConflictRNCAJ01() {
        when(persistence.buscarAbierta()).thenReturn(Optional.of(cajaAbierta(1L, LocalDate.of(2026, 10, 13))));

        assertThatThrownBy(() -> service.abrir(new AbrirCajaCommand(bd("10"))))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("RN-CAJ-01");

        verify(persistence, never()).guardar(any());
    }

    @Test
    void abrir_montoNegativo_lanzaInvalidCajaRNCAJ02() {
        when(persistence.buscarAbierta()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.abrir(new AbrirCajaCommand(bd("-1"))))
                .isInstanceOf(InvalidCajaCanchaException.class)
                .hasMessageContaining("RN-CAJ-02");
    }

    // ---------- actual ----------

    @Test
    void actual_sinCajaAbierta_lanzaNotFound() {
        when(persistence.buscarAbierta()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actual()).isInstanceOf(CajaCanchaNotFoundException.class);
    }

    @Test
    void actual_cajaDeDiaAnterior_incluyeAdvertenciaRNCAJ06() {
        when(persistence.buscarAbierta()).thenReturn(Optional.of(cajaAbierta(1L, LocalDate.of(2026, 10, 12))));
        when(persistence.resumen(1L)).thenReturn(resumen("50.00"));

        assertThat(service.actual().advertencia()).contains("RN-CAJ-06").contains("2026-10-12");
    }

    @Test
    void actual_cajaDeHoy_sinAdvertencia() {
        when(persistence.buscarAbierta()).thenReturn(Optional.of(cajaAbierta(1L, LocalDate.of(2026, 10, 13))));
        when(persistence.resumen(1L)).thenReturn(resumen("50.00"));

        assertThat(service.actual().advertencia()).isNull();
    }

    @Test
    void actual_encargadoNoVeEfectivoEsperadoNiDiferencia() {
        when(persistence.buscarAbierta()).thenReturn(Optional.of(cajaAbierta(1L, LocalDate.of(2026, 10, 13))));
        when(persistence.resumen(1L)).thenReturn(resumen("50.00"));

        CajaCanchaDto dto = service.actual();

        assertThat(dto.efectivoEsperado()).isNull();
        assertThat(dto.diferencia()).isNull();
    }

    @Test
    void actual_adminVeEfectivoEsperadoYDiferencia() {
        when(currentUser.get()).thenReturn(new CurrentUser(1L, "admin", Rol.ADMIN));
        when(persistence.buscarAbierta()).thenReturn(Optional.of(cajaAbierta(1L, LocalDate.of(2026, 10, 13))));
        when(persistence.resumen(1L)).thenReturn(resumen("80.00"));

        CajaCanchaDto dto = service.actual();

        assertThat(dto.efectivoEsperado()).isEqualByComparingTo("80.00");
        assertThat(dto.diferencia()).isNotNull();
    }

    // ---------- cerrar ----------

    @Test
    void cerrar_montoIgualAlEsperado_cierraSinObservacion() {
        CajaCancha caja = cajaAbierta(1L, LocalDate.of(2026, 10, 13));
        when(persistence.buscarAbierta()).thenReturn(Optional.of(caja));
        when(persistence.resumen(1L)).thenReturn(resumen("150.00"));
        when(persistence.guardar(caja)).thenReturn(caja);

        CajaCanchaDto dto = service.cerrar(new CerrarCajaCommand(bd("150"), null));

        assertThat(dto.estado()).isEqualTo(EstadoCaja.CERRADA);
        assertThat(dto.montoCierreContado()).isEqualByComparingTo("150.00");
        assertThat(dto.cerradaPor()).isEqualTo(7L);
        assertThat(dto.cerradaAt()).isEqualTo(AHORA);
    }

    @Test
    void cerrar_conDiferenciaSinObservacion_lanzaInvalidCajaRNCAJ04() {
        CajaCancha caja = cajaAbierta(1L, LocalDate.of(2026, 10, 13));
        when(persistence.buscarAbierta()).thenReturn(Optional.of(caja));
        when(persistence.resumen(1L)).thenReturn(resumen("150.00"));

        assertThatThrownBy(() -> service.cerrar(new CerrarCajaCommand(bd("140"), "  ")))
                .isInstanceOf(InvalidCajaCanchaException.class)
                .hasMessageContaining("RN-CAJ-04");

        verify(persistence, never()).guardar(any());
        assertThat(caja.estaAbierta()).isTrue();
    }

    @Test
    void cerrar_conDiferenciaYObservacion_cierraGuardandoObservacionRecortada() {
        CajaCancha caja = cajaAbierta(1L, LocalDate.of(2026, 10, 13));
        when(persistence.buscarAbierta()).thenReturn(Optional.of(caja));
        when(persistence.resumen(1L)).thenReturn(resumen("150.00"));
        when(persistence.guardar(caja)).thenReturn(caja);

        CajaCanchaDto dto = service.cerrar(new CerrarCajaCommand(bd("140"), "  faltó vuelto "));

        assertThat(dto.observacion()).isEqualTo("faltó vuelto");
        assertThat(dto.estado()).isEqualTo(EstadoCaja.CERRADA);
    }

    @Test
    void cerrar_sinCajaAbierta_lanzaNotFound() {
        when(persistence.buscarAbierta()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cerrar(new CerrarCajaCommand(bd("1"), null)))
                .isInstanceOf(CajaCanchaNotFoundException.class);
    }

    @Test
    void cerrar_contadoNegativo_lanzaInvalidCajaRNCAJ03() {
        CajaCancha caja = cajaAbierta(1L, LocalDate.of(2026, 10, 13));
        when(persistence.buscarAbierta()).thenReturn(Optional.of(caja));
        when(persistence.resumen(1L)).thenReturn(resumen("0.00"));

        assertThatThrownBy(() -> service.cerrar(new CerrarCajaCommand(bd("-5"), "x")))
                .isInstanceOf(InvalidCajaCanchaException.class)
                .hasMessageContaining("RN-CAJ-03");
    }

    // ---------- obtener / listar ----------

    @Test
    void obtener_inexistente_lanzaNotFound() {
        when(persistence.buscarPorId(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(3L)).isInstanceOf(CajaCanchaNotFoundException.class);
    }

    @Test
    void obtener_existente_incluyeResumen() {
        when(persistence.buscarPorId(3L)).thenReturn(Optional.of(cajaAbierta(3L, LocalDate.of(2026, 10, 13))));
        when(persistence.resumen(3L)).thenReturn(new CajaResumen(bd("10.00"), bd("20.00"), bd("0.00"), bd("5.00"),
                bd("55.00"), bd("0.00")));

        CajaCanchaDto dto = service.obtener(3L);

        assertThat(dto.cobrosEfectivo()).isEqualByComparingTo("10.00");
        assertThat(dto.cobrosDigitales()).isEqualByComparingTo("20.00");
        assertThat(dto.egresosEfectivo()).isEqualByComparingTo("5.00");
    }

    @Test
    void listar_mapeaCadaCajaConSuResumen() {
        when(persistence.listar(PAGE)).thenReturn(new PageResult<>(
                List.of(cajaAbierta(1L, LocalDate.of(2026, 10, 13)), cajaAbierta(2L, LocalDate.of(2026, 10, 12))),
                0, 10, 2, 1));
        when(persistence.resumen(any())).thenReturn(resumen("0.00"));

        assertThat(service.listar(PAGE).content()).extracting(CajaCanchaDto::id).containsExactly(1L, 2L);
    }

    // ---------- registrarMovimiento ----------

    @Test
    void registrarMovimiento_sinCajaAbierta_lanzaDomainRNG05() {
        when(persistence.buscarAbierta()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrarMovimiento(
                new MovimientoCajaCommand(TipoMovimientoCaja.INGRESO, MetodoPago.EFECTIVO, "Venta", bd("10"))))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("RN-G05");
    }

    @Test
    void registrarMovimiento_ingresoEfectivo_guardaSinValidarEfectivoDisponible() {
        when(persistence.buscarAbierta()).thenReturn(Optional.of(cajaAbierta(1L, LocalDate.of(2026, 10, 13))));
        when(persistence.guardarMovimiento(any(MovimientoCaja.class))).thenAnswer(inv -> inv.getArgument(0));

        MovimientoCajaDto dto = service.registrarMovimiento(
                new MovimientoCajaCommand(TipoMovimientoCaja.INGRESO, MetodoPago.EFECTIVO, " Propina ", bd("10")));

        assertThat(dto.cajaId()).isEqualTo(1L);
        assertThat(dto.concepto()).isEqualTo("Propina");
        assertThat(dto.registradoPor()).isEqualTo(7L);
        verify(persistence, never()).resumen(any());
    }

    @Test
    void registrarMovimiento_egresoDigital_noValidaEfectivoDisponible() {
        when(persistence.buscarAbierta()).thenReturn(Optional.of(cajaAbierta(1L, LocalDate.of(2026, 10, 13))));
        when(persistence.guardarMovimiento(any(MovimientoCaja.class))).thenAnswer(inv -> inv.getArgument(0));

        service.registrarMovimiento(
                new MovimientoCajaCommand(TipoMovimientoCaja.EGRESO, MetodoPago.YAPE, "Pago", bd("1000")));

        verify(persistence, never()).resumen(any());
    }

    @Test
    void registrarMovimiento_egresoEfectivoMayorAlDisponible_lanzaDomainRNMCA03() {
        when(persistence.buscarAbierta()).thenReturn(Optional.of(cajaAbierta(1L, LocalDate.of(2026, 10, 13))));
        when(persistence.resumen(1L)).thenReturn(resumen("30.00"));

        assertThatThrownBy(() -> service.registrarMovimiento(
                new MovimientoCajaCommand(TipoMovimientoCaja.EGRESO, MetodoPago.EFECTIVO, "Pago luz", bd("30.01"))))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("RN-MCA-03");

        verify(persistence, never()).guardarMovimiento(any());
    }

    @Test
    void registrarMovimiento_egresoEfectivoIgualAlDisponible_seRegistra() {
        when(persistence.buscarAbierta()).thenReturn(Optional.of(cajaAbierta(1L, LocalDate.of(2026, 10, 13))));
        when(persistence.resumen(1L)).thenReturn(resumen("30.00"));
        when(persistence.guardarMovimiento(any(MovimientoCaja.class))).thenAnswer(inv -> inv.getArgument(0));

        MovimientoCajaDto dto = service.registrarMovimiento(
                new MovimientoCajaCommand(TipoMovimientoCaja.EGRESO, MetodoPago.EFECTIVO, "Pago luz", bd("30")));

        assertThat(dto.monto()).isEqualByComparingTo("30.00");
    }

    @Test
    void registrarMovimiento_conceptoVacio_lanzaInvalidCajaRNMCA02() {
        when(persistence.buscarAbierta()).thenReturn(Optional.of(cajaAbierta(1L, LocalDate.of(2026, 10, 13))));

        assertThatThrownBy(() -> service.registrarMovimiento(
                new MovimientoCajaCommand(TipoMovimientoCaja.INGRESO, MetodoPago.EFECTIVO, " ", bd("10"))))
                .isInstanceOf(InvalidCajaCanchaException.class)
                .hasMessageContaining("RN-MCA-02");
    }

    // ---------- listarMovimientos ----------

    @Test
    void listarMovimientos_cajaInexistente_lanzaNotFound() {
        when(persistence.buscarPorId(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listarMovimientos(9L, PAGE)).isInstanceOf(CajaCanchaNotFoundException.class);
    }

    @Test
    void listarMovimientos_cajaExistente_mapeaMovimientos() {
        when(persistence.buscarPorId(1L)).thenReturn(Optional.of(cajaAbierta(1L, LocalDate.of(2026, 10, 13))));
        MovimientoCaja mov = MovimientoCaja.restaurar(5L, 1L, TipoMovimientoCaja.INGRESO, MetodoPago.EFECTIVO, "X",
                bd("10.00"), 7L, AHORA);
        when(persistence.listarMovimientos(1L, PAGE)).thenReturn(new PageResult<>(List.of(mov), 0, 10, 1, 1));

        assertThat(service.listarMovimientos(1L, PAGE).content()).extracting(MovimientoCajaDto::id).containsExactly(5L);
    }

    // ---------- exigirCajaAbiertaId / efectivoEsperado ----------

    @Test
    void exigirCajaAbiertaId_conCajaAbierta_devuelveId() {
        when(persistence.buscarAbierta()).thenReturn(Optional.of(cajaAbierta(4L, LocalDate.of(2026, 10, 13))));

        assertThat(service.exigirCajaAbiertaId()).isEqualTo(4L);
    }

    @Test
    void efectivoEsperado_delegaEnElResumenDeLaBase() {
        when(persistence.resumen(4L)).thenReturn(resumen("77.00"));

        assertThat(service.efectivoEsperado(4L)).isEqualByComparingTo("77.00");
    }
}
