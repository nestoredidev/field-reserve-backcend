package nestoredidev.reserva.infrastructure.adapter.output.persistence;

import nestoredidev.common.domain.MetodoPago;
import nestoredidev.reserva.domain.model.TipoPagoReserva;
import nestoredidev.support.AbstractRepositoryTest;
import nestoredidev.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class PagoReservaRepositoryTest extends AbstractRepositoryTest {

    private static final OffsetDateTime AHORA = TestData.BASE;

    @Autowired
    private PagoReservaRepository repository;

    private Long usuario;
    private Long caja;
    private Long reserva1;
    private Long reserva2;
    private Long reserva3SinPagos;

    @BeforeEach
    void datos() {
        usuario = data.usuario();
        caja = data.cajaCancha("ABIERTA", "100.00", usuario);
        Long cancha = data.cancha();
        Long cliente = data.cliente();
        OffsetDateTime t = AHORA.plusDays(1);
        reserva1 = data.reserva(cancha, cliente, "CONFIRMADA", t, t.plusHours(1), "100.00", null);
        reserva2 = data.reserva(cancha, cliente, "CONFIRMADA", t.plusHours(2), t.plusHours(3), "80.00", null);
        reserva3SinPagos = data.reserva(cancha, cliente, "PENDIENTE", t.plusHours(4), t.plusHours(5), "60.00", null);
    }

    @Test
    void findByReservaIdOrderByFechaAscIdAsc_ordenaPorFechaYLuegoPorId() {
        Long tercero = data.pagoReserva(reserva1, caja, "SALDO", "EFECTIVO", "20.00", null, usuario, AHORA.plusHours(2));
        Long primero = data.pagoReserva(reserva1, caja, "ADELANTO", "EFECTIVO", "50.00", null, usuario, AHORA);
        Long segundoA = data.pagoReserva(reserva1, caja, "ADELANTO", "EFECTIVO", "10.00", null, usuario, AHORA.plusHours(1));
        Long segundoB = data.pagoReserva(reserva1, caja, "ADELANTO", "EFECTIVO", "10.00", null, usuario, AHORA.plusHours(1));
        data.pagoReserva(reserva2, caja, "ADELANTO", "EFECTIVO", "40.00", null, usuario, AHORA);
        flushAndClear();

        assertThat(repository.findByReservaIdOrderByFechaAscIdAsc(reserva1)).extracting(PagoReservaEntity::getId)
                .containsExactly(primero, segundoA, segundoB, tercero);
    }

    @Test
    void findByReservaIdOrderByFechaAscIdAsc_reservaSinPagos_devuelveVacio() {
        assertThat(repository.findByReservaIdOrderByFechaAscIdAsc(reserva3SinPagos)).isEmpty();
    }

    @Test
    void existsByMetodoAndNroOperacion_distingueMetodoYNumero() {
        data.pagoReserva(reserva1, caja, "ADELANTO", "YAPE", "50.00", "OP-1", usuario, AHORA);

        assertThat(repository.existsByMetodoAndNroOperacion(MetodoPago.YAPE, "OP-1")).isTrue();
        assertThat(repository.existsByMetodoAndNroOperacion(MetodoPago.PLIN, "OP-1")).isFalse();
        assertThat(repository.existsByMetodoAndNroOperacion(MetodoPago.YAPE, "OP-2")).isFalse();
    }

    @Test
    void pagadoNeto_sumaPagosYRestaDevoluciones() {
        data.pagoReserva(reserva1, caja, "ADELANTO", "EFECTIVO", "50.00", null, usuario, AHORA);
        data.pagoReserva(reserva1, caja, "SALDO", "YAPE", "60.00", "OP-9", usuario, AHORA.plusHours(1));
        data.pagoReserva(reserva1, caja, "DEVOLUCION", "EFECTIVO", "10.00", null, usuario, AHORA.plusHours(2));
        data.pagoReserva(reserva2, caja, "ADELANTO", "EFECTIVO", "999.00", null, usuario, AHORA);

        assertThat(repository.pagadoNeto(reserva1, TipoPagoReserva.DEVOLUCION)).isEqualByComparingTo("100.00");
    }

    @Test
    void pagadoNeto_sinPagos_esCero() {
        assertThat(repository.pagadoNeto(reserva3SinPagos, TipoPagoReserva.DEVOLUCION)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void pagadoNetoPorReservas_agrupaPorReservaYOmiteLasQueNoTienenPagos() {
        data.pagoReserva(reserva1, caja, "ADELANTO", "EFECTIVO", "50.00", null, usuario, AHORA);
        data.pagoReserva(reserva1, caja, "DEVOLUCION", "EFECTIVO", "20.00", null, usuario, AHORA.plusHours(1));
        data.pagoReserva(reserva2, caja, "ADELANTO", "EFECTIVO", "40.00", null, usuario, AHORA);
        data.pagoReserva(reserva2, caja, "SALDO", "EFECTIVO", "40.00", null, usuario, AHORA.plusHours(1));
        flushAndClear();

        List<Object[]> filas = repository.pagadoNetoPorReservas(List.of(reserva1, reserva2, reserva3SinPagos),
                TipoPagoReserva.DEVOLUCION);

        Map<Long, BigDecimal> porReserva = filas.stream()
                .collect(Collectors.toMap(f -> ((Number) f[0]).longValue(), f -> (BigDecimal) f[1]));
        assertThat(porReserva).containsOnlyKeys(reserva1, reserva2);
        assertThat(porReserva.get(reserva1)).isEqualByComparingTo("30.00");
        assertThat(porReserva.get(reserva2)).isEqualByComparingTo("80.00");
    }

    @Test
    void pagadoNetoPorReservas_soloIncluyeLosIdsPedidos() {
        data.pagoReserva(reserva1, caja, "ADELANTO", "EFECTIVO", "50.00", null, usuario, AHORA);
        data.pagoReserva(reserva2, caja, "ADELANTO", "EFECTIVO", "40.00", null, usuario, AHORA);

        List<Object[]> filas = repository.pagadoNetoPorReservas(List.of(reserva2), TipoPagoReserva.DEVOLUCION);

        assertThat(filas).hasSize(1);
        assertThat(((Number) filas.get(0)[0]).longValue()).isEqualTo(reserva2);
    }
}
