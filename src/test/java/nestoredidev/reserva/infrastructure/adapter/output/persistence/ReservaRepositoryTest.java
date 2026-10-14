package nestoredidev.reserva.infrastructure.adapter.output.persistence;

import jakarta.persistence.PersistenceException;
import nestoredidev.reserva.domain.model.EstadoReserva;
import nestoredidev.support.AbstractRepositoryTest;
import nestoredidev.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservaRepositoryTest extends AbstractRepositoryTest {

    private static final OffsetDateTime AHORA = TestData.BASE;
    private static final OffsetDateTime T18 = AHORA.plusDays(1).withHour(18);
    private static final OffsetDateTime T19 = T18.plusHours(1);
    private static final OffsetDateTime T20 = T18.plusHours(2);
    private static final OffsetDateTime T21 = T18.plusHours(3);

    @Autowired
    private ReservaRepository repository;

    private Long cancha;
    private Long cliente;

    @BeforeEach
    void datos() {
        cancha = data.cancha();
        cliente = data.cliente();
    }

    // ---------- buscarCruces ----------

    @Test
    void buscarCruces_devuelveLasOcupacionesQueSeSuperponenOrdenadasPorInicio() {
        Long tarde = data.reserva(cancha, cliente, "CONFIRMADA", T19, T20, "50.00", null);
        Long temprano = data.reserva(cancha, cliente, "PENDIENTE", T18, T19, "50.00", null);
        flushAndClear();

        List<ReservaEntity> res = repository.buscarCruces(cancha, T18, T20, -1L, EstadoReserva.CANCELADA);

        assertThat(res).extracting(ReservaEntity::getId).containsExactly(temprano, tarde);
    }

    @Test
    void buscarCruces_incluyeMantenimientos() {
        Long mant = data.mantenimiento(cancha, "CONFIRMADA", T18, T20);
        flushAndClear();

        assertThat(repository.buscarCruces(cancha, T19, T21, -1L, EstadoReserva.CANCELADA))
                .extracting(ReservaEntity::getId).containsExactly(mant);
    }

    @Test
    void buscarCruces_excluyeLasCanceladas() {
        data.reserva(cancha, cliente, "CANCELADA", T18, T19, "50.00", null);
        flushAndClear();

        assertThat(repository.buscarCruces(cancha, T18, T19, -1L, EstadoReserva.CANCELADA)).isEmpty();
    }

    @Test
    void buscarCruces_excluyeLaReservaIndicada() {
        Long propia = data.reserva(cancha, cliente, "CONFIRMADA", T18, T19, "50.00", null);
        flushAndClear();

        assertThat(repository.buscarCruces(cancha, T18, T19, propia, EstadoReserva.CANCELADA)).isEmpty();
        assertThat(repository.buscarCruces(cancha, T18, T19, -1L, EstadoReserva.CANCELADA)).hasSize(1);
    }

    @Test
    void buscarCruces_horariosContiguosNoSeCruzan() {
        data.reserva(cancha, cliente, "CONFIRMADA", T18, T19, "50.00", null);
        flushAndClear();

        assertThat(repository.buscarCruces(cancha, T19, T20, -1L, EstadoReserva.CANCELADA)).isEmpty();
        assertThat(repository.buscarCruces(cancha, T18.minusHours(1), T18, -1L, EstadoReserva.CANCELADA)).isEmpty();
    }

    @Test
    void buscarCruces_noMezclaCanchas() {
        Long otraCancha = data.cancha();
        data.reserva(otraCancha, cliente, "CONFIRMADA", T18, T19, "50.00", null);
        flushAndClear();

        assertThat(repository.buscarCruces(cancha, T18, T19, -1L, EstadoReserva.CANCELADA)).isEmpty();
    }

    @Test
    void buscarCruces_unaReservaQueCubreTodoElRango_tambienSeDetecta() {
        Long larga = data.reserva(cancha, cliente, "CONFIRMADA", T18, T21, "150.00", null);
        flushAndClear();

        assertThat(repository.buscarCruces(cancha, T19, T20, -1L, EstadoReserva.CANCELADA))
                .extracting(ReservaEntity::getId).containsExactly(larga);
    }

    // ---------- countByClienteIdAndEstado ----------

    @Test
    void countByClienteIdAndEstado_cuentaSoloEseClienteYEstado() {
        Long otro = data.cliente();
        data.reserva(cancha, cliente, "NO_ASISTIO", T18, T19, "50.00", null);
        data.reserva(cancha, cliente, "NO_ASISTIO", T19, T20, "50.00", null);
        data.reserva(cancha, cliente, "CONFIRMADA", T20, T21, "50.00", null);
        data.reserva(cancha, otro, "NO_ASISTIO", T21, T21.plusHours(1), "50.00", null);

        assertThat(repository.countByClienteIdAndEstado(cliente, EstadoReserva.NO_ASISTIO)).isEqualTo(2);
        assertThat(repository.countByClienteIdAndEstado(cliente, EstadoReserva.CONFIRMADA)).isEqualTo(1);
        assertThat(repository.countByClienteIdAndEstado(cliente, EstadoReserva.CANCELADA)).isZero();
    }

    // ---------- findByEstadoAndFechaLimiteAdelantoBefore ----------

    @Test
    void findByEstadoAndFechaLimiteAdelantoBefore_devuelveLasPendientesVencidas() {
        Long vencida = data.reserva(cancha, cliente, "PENDIENTE", T18, T19, "50.00", AHORA.minusMinutes(1));
        data.reserva(cancha, cliente, "PENDIENTE", T19, T20, "50.00", AHORA.plusHours(1)); // aún a tiempo
        data.reserva(cancha, cliente, "CONFIRMADA", T20, T21, "50.00", AHORA.minusHours(3)); // ya no es pendiente
        flushAndClear();

        assertThat(repository.findByEstadoAndFechaLimiteAdelantoBefore(EstadoReserva.PENDIENTE, AHORA))
                .extracting(ReservaEntity::getId).containsExactly(vencida);
    }

    @Test
    void findByEstadoAndFechaLimiteAdelantoBefore_limiteExactamenteAhora_noEstaVencida() {
        data.reserva(cancha, cliente, "PENDIENTE", T18, T19, "50.00", AHORA);

        assertThat(repository.findByEstadoAndFechaLimiteAdelantoBefore(EstadoReserva.PENDIENTE, AHORA)).isEmpty();
    }

    // ---------- findByIdForUpdate ----------

    @Test
    void findByIdForUpdate_devuelveLaReservaConSusDatos() {
        Long id = data.reserva(cancha, cliente, "CONFIRMADA", T18, T19, "50.00", null);
        flushAndClear();

        assertThat(repository.findByIdForUpdate(id)).hasValueSatisfying(r -> {
            assertThat(r.getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
            assertThat(r.getCanchaId()).isEqualTo(cancha);
            assertThat(r.getMontoTotal()).isEqualByComparingTo("50.00");
        });
        assertThat(repository.findByIdForUpdate(-1L)).isEmpty();
    }

    // ---------- constraints de la base (regla clave: no hay cruces) ----------

    @Test
    void dosReservasActivasQueSeCruzan_laBaseLasRechazaPorExclusion() {
        data.reserva(cancha, cliente, "CONFIRMADA", T18, T20, "100.00", null);

        assertThatThrownBy(() -> data.reserva(cancha, cliente, "PENDIENTE", T19, T21, "100.00", null))
                .isInstanceOf(PersistenceException.class)
                .hasStackTraceContaining("ex_reserva_sin_cruce");
    }

    @Test
    void reservaCanceladaNoBloqueaElHorario() {
        data.reserva(cancha, cliente, "CANCELADA", T18, T20, "100.00", null);

        assertThat(data.reserva(cancha, cliente, "PENDIENTE", T18, T20, "100.00", null)).isNotNull();
    }

    @Test
    void reservasContiguasEnLaMismaCancha_sonValidas() {
        data.reserva(cancha, cliente, "CONFIRMADA", T18, T19, "50.00", null);

        assertThat(data.reserva(cancha, cliente, "CONFIRMADA", T19, T20, "50.00", null)).isNotNull();
    }

    @Test
    void mantenimientoSobreUnaReserva_laBaseLoRechaza() {
        data.reserva(cancha, cliente, "CONFIRMADA", T18, T19, "50.00", null);

        assertThatThrownBy(() -> data.mantenimiento(cancha, "CONFIRMADA", T18, T20))
                .isInstanceOf(PersistenceException.class)
                .hasStackTraceContaining("ex_reserva_sin_cruce");
    }

    @Test
    void reservaSinCliente_laBaseLaRechazaPorCheckDeTipo() {
        assertThatThrownBy(() -> em.createNativeQuery("insert into cancha.reserva (cancha_id, tipo, estado, inicio, fin, "
                + "monto_total) values (" + cancha + ", 'RESERVA', 'PENDIENTE', timestamptz '2026-10-14T18:00:00-05:00', "
                + "timestamptz '2026-10-14T19:00:00-05:00', 50)").executeUpdate())
                .isInstanceOf(PersistenceException.class)
                .hasStackTraceContaining("chk_reserva_tipo");
    }
}
