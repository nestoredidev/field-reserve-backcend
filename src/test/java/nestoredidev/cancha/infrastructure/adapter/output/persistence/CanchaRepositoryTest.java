package nestoredidev.cancha.infrastructure.adapter.output.persistence;

import nestoredidev.support.AbstractRepositoryTest;
import nestoredidev.support.TestData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CanchaRepositoryTest extends AbstractRepositoryTest {

    private static final OffsetDateTime AHORA = TestData.BASE;

    @Autowired
    private CanchaRepository repository;

    // ---------- existsByNombre ----------

    @Test
    void existsByNombre_ignoraMayusculasYMinusculas() {
        data.cancha("Cancha Norte", true);

        assertThat(repository.existsByNombre("cancha norte", null)).isTrue();
        assertThat(repository.existsByNombre("CANCHA NORTE", null)).isTrue();
        assertThat(repository.existsByNombre("Cancha Sur", null)).isFalse();
    }

    @Test
    void existsByNombre_conExcluirId_noCuentaLaPropiaCancha() {
        Long id = data.cancha("Cancha Norte", true);
        Long otra = data.cancha("Cancha Sur", true);

        assertThat(repository.existsByNombre("Cancha Norte", id)).isFalse();
        assertThat(repository.existsByNombre("Cancha Norte", otra)).isTrue();
    }

    // ---------- buscar ----------

    @Test
    void buscar_conActivaNula_devuelveTodas() {
        data.cancha("A", true);
        data.cancha("B", false);

        Page<CanchaEntity> page = repository.buscar(null, PageRequest.of(0, 10, Sort.by("nombre")));

        assertThat(page.getContent()).extracting(CanchaEntity::getNombre).containsExactly("A", "B");
    }

    @Test
    void buscar_filtraPorEstadoActiva() {
        data.cancha("A", true);
        data.cancha("B", false);

        assertThat(repository.buscar(true, PageRequest.of(0, 10)).getContent())
                .extracting(CanchaEntity::getNombre).containsExactly("A");
        assertThat(repository.buscar(false, PageRequest.of(0, 10)).getContent())
                .extracting(CanchaEntity::getNombre).containsExactly("B");
    }

    @Test
    void buscar_paginaYOrdena() {
        data.cancha("A", true);
        data.cancha("B", true);
        data.cancha("C", true);

        Page<CanchaEntity> pagina2 = repository.buscar(null, PageRequest.of(1, 2, Sort.by("nombre").descending()));

        assertThat(pagina2.getTotalElements()).isEqualTo(3);
        assertThat(pagina2.getTotalPages()).isEqualTo(2);
        assertThat(pagina2.getContent()).extracting(CanchaEntity::getNombre).containsExactly("A");
    }

    // ---------- tieneOcupacionFutura ----------

    @Test
    void tieneOcupacionFutura_conReservaPendienteQueTerminaDespues_esTrue() {
        Long cancha = data.cancha();
        data.reserva(cancha, data.cliente(), "PENDIENTE", AHORA.plusDays(1), AHORA.plusDays(1).plusHours(1), "50.00", null);

        assertThat(repository.tieneOcupacionFutura(cancha, AHORA)).isTrue();
    }

    @Test
    void tieneOcupacionFutura_conMantenimientoConfirmado_esTrue() {
        Long cancha = data.cancha();
        data.mantenimiento(cancha, "CONFIRMADA", AHORA.plusDays(2), AHORA.plusDays(2).plusHours(2));

        assertThat(repository.tieneOcupacionFutura(cancha, AHORA)).isTrue();
    }

    @Test
    void tieneOcupacionFutura_soloReservasCanceladasCompletadasONoAsistio_esFalse() {
        Long cancha = data.cancha();
        Long cliente = data.cliente();
        data.reserva(cancha, cliente, "CANCELADA", AHORA.plusDays(1), AHORA.plusDays(1).plusHours(1), "50.00", null);
        data.reserva(cancha, cliente, "COMPLETADA", AHORA.plusDays(2), AHORA.plusDays(2).plusHours(1), "50.00", null);
        data.reserva(cancha, cliente, "NO_ASISTIO", AHORA.plusDays(3), AHORA.plusDays(3).plusHours(1), "50.00", null);

        assertThat(repository.tieneOcupacionFutura(cancha, AHORA)).isFalse();
    }

    @Test
    void tieneOcupacionFutura_reservaQueYaTermino_esFalse() {
        Long cancha = data.cancha();
        data.reserva(cancha, data.cliente(), "CONFIRMADA", AHORA.minusDays(1), AHORA.minusDays(1).plusHours(1), "50.00", null);

        assertThat(repository.tieneOcupacionFutura(cancha, AHORA)).isFalse();
    }

    @Test
    void tieneOcupacionFutura_reservaEnCursoQueTerminaDespuesDeDesde_esTrue() {
        Long cancha = data.cancha();
        data.reserva(cancha, data.cliente(), "CONFIRMADA", AHORA.minusMinutes(30), AHORA.plusMinutes(30), "50.00", null);

        assertThat(repository.tieneOcupacionFutura(cancha, AHORA)).isTrue();
    }

    @Test
    void tieneOcupacionFutura_reservaQueTerminaExactamenteEnDesde_esFalse() {
        Long cancha = data.cancha();
        data.reserva(cancha, data.cliente(), "CONFIRMADA", AHORA.minusHours(1), AHORA, "50.00", null);

        assertThat(repository.tieneOcupacionFutura(cancha, AHORA)).isFalse();
    }

    @Test
    void tieneOcupacionFutura_noMezclaCanchas() {
        Long canchaConReserva = data.cancha();
        Long canchaLibre = data.cancha();
        data.reserva(canchaConReserva, data.cliente(), "PENDIENTE", AHORA.plusDays(1), AHORA.plusDays(1).plusHours(1), "50.00", null);

        assertThat(repository.tieneOcupacionFutura(canchaLibre, AHORA)).isFalse();
    }
}
