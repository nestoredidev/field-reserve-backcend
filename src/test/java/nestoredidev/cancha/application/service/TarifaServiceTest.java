package nestoredidev.cancha.application.service;

import nestoredidev.cancha.application.ports.input.dto.CambiarPrecioCommand;
import nestoredidev.cancha.application.ports.input.dto.CreateTarifaCommand;
import nestoredidev.cancha.application.ports.input.dto.TarifaDto;
import nestoredidev.cancha.application.ports.output.CanchaPersistencePort;
import nestoredidev.cancha.application.ports.output.TarifaPersistencePort;
import nestoredidev.cancha.domain.exception.CanchaNotFoundException;
import nestoredidev.cancha.domain.exception.InvalidCanchaException;
import nestoredidev.cancha.domain.exception.TarifaNotFoundException;
import nestoredidev.cancha.domain.model.Cancha;
import nestoredidev.cancha.domain.model.Tarifa;
import nestoredidev.common.exception.ConflictException;
import nestoredidev.common.exception.DomainException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalTime;
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
class TarifaServiceTest {

    private static final LocalTime INICIO = LocalTime.of(18, 0);
    private static final LocalTime FIN = LocalTime.of(23, 0);

    @Mock
    private TarifaPersistencePort persistence;
    @Mock
    private CanchaPersistencePort canchaPersistence;

    @InjectMocks
    private TarifaService service;

    private static Cancha cancha() {
        return Cancha.restaurar(1L, "Cancha 1", null, true, OffsetDateTime.now());
    }

    private static Tarifa tarifa(Long id, boolean activa) {
        return Tarifa.restaurar(id, 1L, (short) 1, INICIO, FIN, new BigDecimal("60.00"), activa, OffsetDateTime.now());
    }

    // ---------- crear ----------

    @Test
    void crear_valida_guardaTarifaActivaConPrecioDosDecimales() {
        when(canchaPersistence.buscarPorId(1L)).thenReturn(Optional.of(cancha()));
        when(persistence.existeCruce(1L, (short) 1, INICIO, FIN, null)).thenReturn(false);
        when(persistence.guardar(any(Tarifa.class))).thenAnswer(inv -> inv.getArgument(0));

        TarifaDto dto = service.crear(1L, new CreateTarifaCommand((short) 1, INICIO, FIN, new BigDecimal("60")));

        assertThat(dto.canchaId()).isEqualTo(1L);
        assertThat(dto.activa()).isTrue();
        assertThat(dto.precioHora()).isEqualByComparingTo("60.00");
        assertThat(dto.precioHora().scale()).isEqualTo(2);
    }

    @Test
    void crear_canchaInexistente_lanzaNotFound() {
        when(canchaPersistence.buscarPorId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crear(1L, new CreateTarifaCommand((short) 1, INICIO, FIN, BigDecimal.TEN)))
                .isInstanceOf(CanchaNotFoundException.class);

        verify(persistence, never()).guardar(any());
    }

    @Test
    void crear_franjaQueSeCruza_lanzaConflictRNTAR01() {
        when(canchaPersistence.buscarPorId(1L)).thenReturn(Optional.of(cancha()));
        when(persistence.existeCruce(1L, (short) 1, INICIO, FIN, null)).thenReturn(true);

        assertThatThrownBy(() -> service.crear(1L, new CreateTarifaCommand((short) 1, INICIO, FIN, BigDecimal.TEN)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("RN-TAR-01");

        verify(persistence, never()).guardar(any());
    }

    @Test
    void crear_horaFinNoPosteriorAInicio_lanzaInvalidCancha() {
        when(canchaPersistence.buscarPorId(1L)).thenReturn(Optional.of(cancha()));

        assertThatThrownBy(() -> service.crear(1L, new CreateTarifaCommand((short) 1, FIN, INICIO, BigDecimal.TEN)))
                .isInstanceOf(InvalidCanchaException.class);
    }

    @Test
    void crear_precioCero_lanzaInvalidCanchaRNTAR03() {
        when(canchaPersistence.buscarPorId(1L)).thenReturn(Optional.of(cancha()));

        assertThatThrownBy(() -> service.crear(1L, new CreateTarifaCommand((short) 1, INICIO, FIN, BigDecimal.ZERO)))
                .isInstanceOf(InvalidCanchaException.class)
                .hasMessageContaining("RN-TAR-03");
    }

    @Test
    void crear_diaSemanaNulo_creaTarifaParaTodosLosDias() {
        when(canchaPersistence.buscarPorId(1L)).thenReturn(Optional.of(cancha()));
        when(persistence.existeCruce(1L, null, INICIO, FIN, null)).thenReturn(false);
        when(persistence.guardar(any(Tarifa.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.crear(1L, new CreateTarifaCommand(null, INICIO, FIN, BigDecimal.TEN)).diaSemana()).isNull();
    }

    // ---------- obtener / listar ----------

    @Test
    void obtener_inexistente_lanzaNotFound() {
        when(persistence.buscarPorId(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(5L)).isInstanceOf(TarifaNotFoundException.class);
    }

    @Test
    void listar_canchaExistente_mapeaTarifas() {
        when(canchaPersistence.buscarPorId(1L)).thenReturn(Optional.of(cancha()));
        when(persistence.listarPorCancha(1L, true)).thenReturn(List.of(tarifa(1L, true), tarifa(2L, true)));

        assertThat(service.listar(1L, true)).extracting(TarifaDto::id).containsExactly(1L, 2L);
    }

    @Test
    void listar_canchaInexistente_lanzaNotFound() {
        when(canchaPersistence.buscarPorId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listar(1L, false)).isInstanceOf(CanchaNotFoundException.class);

        verify(persistence, never()).listarPorCancha(any(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    // ---------- desactivar ----------

    @Test
    void desactivar_activa_guardaTarifaInactiva() {
        when(persistence.buscarPorId(1L)).thenReturn(Optional.of(tarifa(1L, true)));
        when(persistence.guardar(any(Tarifa.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.desactivar(1L).activa()).isFalse();
    }

    @Test
    void desactivar_yaDesactivada_lanzaDomainException() {
        when(persistence.buscarPorId(1L)).thenReturn(Optional.of(tarifa(1L, false)));

        assertThatThrownBy(() -> service.desactivar(1L))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("ya está desactivada");

        verify(persistence, never()).guardar(any());
    }

    // ---------- cambiarPrecio ----------

    @Test
    void cambiarPrecio_activa_desactivaLaAnteriorYCreaNuevaConMismasFranjas() {
        when(persistence.buscarPorId(1L)).thenReturn(Optional.of(tarifa(1L, true)));
        when(persistence.guardar(any(Tarifa.class))).thenAnswer(inv -> inv.getArgument(0));

        TarifaDto dto = service.cambiarPrecio(1L, new CambiarPrecioCommand(new BigDecimal("80")));

        InOrder orden = inOrder(persistence);
        ArgumentCaptor<Tarifa> captor = ArgumentCaptor.forClass(Tarifa.class);
        orden.verify(persistence, org.mockito.Mockito.times(2)).guardar(captor.capture());
        Tarifa vieja = captor.getAllValues().get(0);
        Tarifa nueva = captor.getAllValues().get(1);

        assertThat(vieja.getId()).isEqualTo(1L);
        assertThat(vieja.isActiva()).isFalse();
        assertThat(nueva.getId()).isNull();
        assertThat(nueva.isActiva()).isTrue();
        assertThat(nueva.getPrecioHora()).isEqualByComparingTo("80.00");
        assertThat(nueva.getHoraInicio()).isEqualTo(INICIO);
        assertThat(nueva.getHoraFin()).isEqualTo(FIN);
        assertThat(nueva.getDiaSemana()).isEqualTo((short) 1);
        assertThat(dto.precioHora()).isEqualByComparingTo("80.00");
    }

    @Test
    void cambiarPrecio_tarifaInactiva_lanzaDomainException() {
        when(persistence.buscarPorId(1L)).thenReturn(Optional.of(tarifa(1L, false)));

        assertThatThrownBy(() -> service.cambiarPrecio(1L, new CambiarPrecioCommand(BigDecimal.TEN)))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("tarifa activa");

        verify(persistence, never()).guardar(any());
    }

    @Test
    void cambiarPrecio_precioInvalido_lanzaInvalidCanchaSinGuardarNada() {
        when(persistence.buscarPorId(1L)).thenReturn(Optional.of(tarifa(1L, true)));

        assertThatThrownBy(() -> service.cambiarPrecio(1L, new CambiarPrecioCommand(BigDecimal.ZERO)))
                .isInstanceOf(InvalidCanchaException.class);

        verify(persistence, never()).guardar(any());
    }
}
