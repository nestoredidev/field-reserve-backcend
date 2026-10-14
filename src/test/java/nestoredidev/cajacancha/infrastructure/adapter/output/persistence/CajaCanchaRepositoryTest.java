package nestoredidev.cajacancha.infrastructure.adapter.output.persistence;

import jakarta.persistence.PersistenceException;
import nestoredidev.common.domain.EstadoCaja;
import nestoredidev.support.AbstractRepositoryTest;
import nestoredidev.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CajaCanchaRepositoryTest extends AbstractRepositoryTest {

    private static final OffsetDateTime AHORA = TestData.BASE;

    @Autowired
    private CajaCanchaRepository cajaRepository;
    @Autowired
    private MovimientoCajaRepository movimientoRepository;

    private Long usuario;

    @BeforeEach
    void datos() {
        usuario = data.usuario();
    }

    // ---------- findFirstByEstado ----------

    @Test
    void findFirstByEstado_abierta_devuelveLaCajaAbierta() {
        data.cajaCancha("CERRADA", "50.00", usuario);
        Long abierta = data.cajaCancha("ABIERTA", "100.00", usuario);
        flushAndClear();

        assertThat(cajaRepository.findFirstByEstado(EstadoCaja.ABIERTA))
                .hasValueSatisfying(c -> assertThat(c.getId()).isEqualTo(abierta));
    }

    @Test
    void findFirstByEstado_sinCajaAbierta_devuelveVacio() {
        data.cajaCancha("CERRADA", "50.00", usuario);

        assertThat(cajaRepository.findFirstByEstado(EstadoCaja.ABIERTA)).isEmpty();
    }

    @Test
    void soloPuedeHaberUnaCajaAbierta_laBaseRechazaLaSegunda() {
        data.cajaCancha("ABIERTA", "100.00", usuario);

        assertThatThrownBy(() -> data.cajaCancha("ABIERTA", "10.00", usuario))
                .isInstanceOf(PersistenceException.class)
                .hasStackTraceContaining("uq_cancha_caja_abierta");
    }

    // ---------- resumen (vista cancha.v_caja_resumen) ----------

    @Test
    void resumen_calculaCobrosMovimientosYEfectivoEsperado() {
        Long caja = data.cajaCancha("ABIERTA", "100.00", usuario);
        Long cancha = data.cancha();
        Long cliente = data.cliente();
        Long reserva = data.reserva(cancha, cliente, "CONFIRMADA", AHORA.plusDays(1), AHORA.plusDays(1).plusHours(2), "200.00", null);
        data.pagoReserva(reserva, caja, "ADELANTO", "EFECTIVO", "50.00", null, usuario, AHORA);
        data.pagoReserva(reserva, caja, "SALDO", "EFECTIVO", "10.00", null, usuario, AHORA);
        data.pagoReserva(reserva, caja, "DEVOLUCION", "EFECTIVO", "10.00", null, usuario, AHORA);
        data.pagoReserva(reserva, caja, "ADELANTO", "YAPE", "30.00", "OP-1", usuario, AHORA);
        data.movimientoCancha(caja, "INGRESO", "EFECTIVO", "5.00", usuario);
        data.movimientoCancha(caja, "EGRESO", "EFECTIVO", "20.00", usuario);
        data.movimientoCancha(caja, "EGRESO", "YAPE", "7.00", usuario); // digital: no afecta al efectivo
        flushAndClear();

        CajaCanchaRepository.ResumenRow r = cajaRepository.resumen(caja).orElseThrow();

        assertThat(r.getCobrosEfectivo()).isEqualByComparingTo("50.00"); // 50 + 10 - 10
        assertThat(r.getCobrosDigitales()).isEqualByComparingTo("30.00");
        assertThat(r.getOtrosIngresosEfectivo()).isEqualByComparingTo("5.00");
        assertThat(r.getEgresosEfectivo()).isEqualByComparingTo("20.00");
        assertThat(r.getEfectivoEsperado()).isEqualByComparingTo("135.00"); // 100 + 50 + 5 - 20
        assertThat(r.getDiferencia()).isNull(); // la caja sigue abierta: no hay efectivo contado
    }

    @Test
    void resumen_cajaSinMovimientos_todoEnCeroYEfectivoEsperadoIgualALaApertura() {
        Long caja = data.cajaCancha("ABIERTA", "80.00", usuario);
        flushAndClear();

        CajaCanchaRepository.ResumenRow r = cajaRepository.resumen(caja).orElseThrow();

        assertThat(r.getCobrosEfectivo()).isEqualByComparingTo("0");
        assertThat(r.getCobrosDigitales()).isEqualByComparingTo("0");
        assertThat(r.getOtrosIngresosEfectivo()).isEqualByComparingTo("0");
        assertThat(r.getEgresosEfectivo()).isEqualByComparingTo("0");
        assertThat(r.getEfectivoEsperado()).isEqualByComparingTo("80.00");
    }

    @Test
    void resumen_cajaCerrada_calculaLaDiferenciaContraElEfectivoContado() {
        // helper: monto_cierre_contado = apertura; sin movimientos el esperado es la apertura => diferencia 0
        Long caja = data.cajaCancha("CERRADA", "60.00", usuario);
        flushAndClear();

        assertThat(cajaRepository.resumen(caja).orElseThrow().getDiferencia()).isEqualByComparingTo("0");
    }

    @Test
    void resumen_aislaCadaCaja() {
        Long cerrada = data.cajaCancha("CERRADA", "10.00", usuario);
        Long abierta = data.cajaCancha("ABIERTA", "100.00", usuario);
        data.movimientoCancha(cerrada, "INGRESO", "EFECTIVO", "99.00", usuario);
        flushAndClear();

        assertThat(cajaRepository.resumen(abierta).orElseThrow().getOtrosIngresosEfectivo()).isEqualByComparingTo("0");
    }

    @Test
    void resumen_cajaInexistente_devuelveVacio() {
        assertThat(cajaRepository.resumen(-1L)).isEmpty();
    }

    // ---------- MovimientoCajaRepository ----------

    @Test
    void findByCajaId_devuelveSoloLosMovimientosDeLaCajaPaginados() {
        Long caja = data.cajaCancha("ABIERTA", "100.00", usuario);
        Long otraCaja = data.cajaCancha("CERRADA", "10.00", usuario);
        Long m1 = data.movimientoCancha(caja, "INGRESO", "EFECTIVO", "1.00", usuario);
        Long m2 = data.movimientoCancha(caja, "EGRESO", "EFECTIVO", "2.00", usuario);
        Long m3 = data.movimientoCancha(caja, "INGRESO", "YAPE", "3.00", usuario);
        data.movimientoCancha(otraCaja, "INGRESO", "EFECTIVO", "9.00", usuario);
        flushAndClear();

        Page<MovimientoCajaEntity> p1 = movimientoRepository.findByCajaId(caja, PageRequest.of(0, 2, Sort.by("id")));
        Page<MovimientoCajaEntity> p2 = movimientoRepository.findByCajaId(caja, PageRequest.of(1, 2, Sort.by("id")));

        assertThat(p1.getTotalElements()).isEqualTo(3);
        assertThat(p1.getContent()).extracting(MovimientoCajaEntity::getId).containsExactly(m1, m2);
        assertThat(p2.getContent()).extracting(MovimientoCajaEntity::getId).containsExactly(m3);
    }

    @Test
    void findByCajaId_cajaSinMovimientos_devuelvePaginaVacia() {
        Long caja = data.cajaCancha("ABIERTA", "100.00", usuario);

        assertThat(movimientoRepository.findByCajaId(caja, PageRequest.of(0, 10)).getContent()).isEmpty();
    }
}
