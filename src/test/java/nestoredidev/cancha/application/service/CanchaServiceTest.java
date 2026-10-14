package nestoredidev.cancha.application.service;

import nestoredidev.cancha.application.ports.input.dto.CanchaCommand;
import nestoredidev.cancha.application.ports.input.dto.CanchaDto;
import nestoredidev.cancha.application.ports.output.CanchaPersistencePort;
import nestoredidev.cancha.domain.exception.CanchaNotFoundException;
import nestoredidev.cancha.domain.exception.InvalidCanchaException;
import nestoredidev.cancha.domain.model.Cancha;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;
import nestoredidev.common.exception.ConflictException;
import nestoredidev.common.exception.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
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
class CanchaServiceTest {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final PageQuery PAGE = new PageQuery(0, 10, "nombre", "asc");

    @Mock
    private CanchaPersistencePort persistence;

    private Clock clock;
    private CanchaService service;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(OffsetDateTime.parse("2026-10-13T10:00:00-05:00").toInstant(), LIMA);
        service = new CanchaService(persistence, clock);
    }

    private static Cancha cancha(Long id, String nombre, boolean activa) {
        return Cancha.restaurar(id, nombre, "desc", activa, OffsetDateTime.parse("2026-01-01T00:00:00-05:00"));
    }

    // ---------- crear ----------

    @Test
    void crear_valida_guardaActivaYNombreRecortado() {
        when(persistence.existePorNombre("Cancha 1", null)).thenReturn(false);
        when(persistence.guardar(any(Cancha.class))).thenAnswer(inv -> {
            Cancha c = inv.getArgument(0);
            return Cancha.restaurar(1L, c.getNombre(), c.getDescripcion(), c.isActiva(), OffsetDateTime.now(clock));
        });

        CanchaDto dto = service.crear(new CanchaCommand("  Cancha 1 ", "  Césped "));

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.nombre()).isEqualTo("Cancha 1");
        assertThat(dto.descripcion()).isEqualTo("Césped");
        assertThat(dto.activa()).isTrue();
    }

    @Test
    void crear_nombreDuplicado_lanzaConflictRNCAN01() {
        when(persistence.existePorNombre("Cancha 1", null)).thenReturn(true);

        assertThatThrownBy(() -> service.crear(new CanchaCommand("Cancha 1", null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("RN-CAN-01");

        verify(persistence, never()).guardar(any());
    }

    @Test
    void crear_nombreVacio_lanzaInvalidCancha() {
        assertThatThrownBy(() -> service.crear(new CanchaCommand("  ", null)))
                .isInstanceOf(InvalidCanchaException.class);

        verify(persistence, never()).existePorNombre(any(), any());
    }

    // ---------- obtener / listar ----------

    @Test
    void obtener_existente_devuelveDto() {
        when(persistence.buscarPorId(1L)).thenReturn(Optional.of(cancha(1L, "Cancha 1", true)));

        assertThat(service.obtener(1L).nombre()).isEqualTo("Cancha 1");
    }

    @Test
    void obtener_inexistente_lanzaNotFound() {
        when(persistence.buscarPorId(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(9L)).isInstanceOf(CanchaNotFoundException.class);
    }

    @Test
    void listar_filtroActiva_delegaYMapea() {
        when(persistence.listar(true, PAGE))
                .thenReturn(new PageResult<>(List.of(cancha(1L, "Cancha 1", true)), 0, 10, 1, 1));

        PageResult<CanchaDto> resultado = service.listar(true, PAGE);

        assertThat(resultado.content()).extracting(CanchaDto::id).containsExactly(1L);
    }

    // ---------- actualizar ----------

    @Test
    void actualizar_valida_validaNombreExcluyendoElPropioId() {
        when(persistence.buscarPorId(1L)).thenReturn(Optional.of(cancha(1L, "Cancha 1", true)));
        when(persistence.existePorNombre("Cancha Nueva", 1L)).thenReturn(false);
        when(persistence.guardar(any(Cancha.class))).thenAnswer(inv -> inv.getArgument(0));

        CanchaDto dto = service.actualizar(1L, new CanchaCommand("Cancha Nueva", "otra"));

        assertThat(dto.nombre()).isEqualTo("Cancha Nueva");
        assertThat(dto.activa()).isTrue();
    }

    @Test
    void actualizar_nombreDeOtraCancha_lanzaConflict() {
        when(persistence.buscarPorId(1L)).thenReturn(Optional.of(cancha(1L, "Cancha 1", true)));
        when(persistence.existePorNombre("Cancha 2", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.actualizar(1L, new CanchaCommand("Cancha 2", null)))
                .isInstanceOf(ConflictException.class);

        verify(persistence, never()).guardar(any());
    }

    @Test
    void actualizar_inexistente_lanzaNotFound() {
        when(persistence.buscarPorId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizar(1L, new CanchaCommand("X", null)))
                .isInstanceOf(CanchaNotFoundException.class);
    }

    // ---------- activar / desactivar ----------

    @Test
    void activar_cambiaActivaATrue() {
        when(persistence.buscarPorId(1L)).thenReturn(Optional.of(cancha(1L, "Cancha 1", false)));
        when(persistence.guardar(any(Cancha.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.activar(1L).activa()).isTrue();
    }

    @Test
    void desactivar_sinOcupacionFutura_cambiaActivaAFalse() {
        when(persistence.buscarPorId(1L)).thenReturn(Optional.of(cancha(1L, "Cancha 1", true)));
        when(persistence.tieneOcupacionFutura(1L, OffsetDateTime.now(clock))).thenReturn(false);
        when(persistence.guardar(any(Cancha.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.desactivar(1L).activa()).isFalse();
    }

    @Test
    void desactivar_conOcupacionFutura_lanzaDomainRNCAN03() {
        when(persistence.buscarPorId(1L)).thenReturn(Optional.of(cancha(1L, "Cancha 1", true)));
        when(persistence.tieneOcupacionFutura(1L, OffsetDateTime.now(clock))).thenReturn(true);

        assertThatThrownBy(() -> service.desactivar(1L))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("RN-CAN-03");

        verify(persistence, never()).guardar(any());
    }

    @Test
    void desactivar_inexistente_lanzaNotFoundSinConsultarOcupacion() {
        when(persistence.buscarPorId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.desactivar(1L)).isInstanceOf(CanchaNotFoundException.class);

        verify(persistence, never()).tieneOcupacionFutura(any(), any());
    }
}
