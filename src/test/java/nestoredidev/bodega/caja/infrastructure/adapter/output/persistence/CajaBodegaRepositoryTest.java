package nestoredidev.bodega.caja.infrastructure.adapter.output.persistence;

import jakarta.persistence.PersistenceException;
import nestoredidev.common.domain.EstadoCaja;
import nestoredidev.support.AbstractRepositoryTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CajaBodegaRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private CajaBodegaRepository cajaRepository;
    @Autowired
    private MovimientoBodegaRepository movimientoRepository;

    private Long usuario;

    @BeforeEach
    void datos() {
        usuario = data.usuario();
    }

    @Test
    void findFirstByEstado_abierta_devuelveLaCajaAbierta() {
        data.cajaBodega("CERRADA", "50.00", usuario);
        Long abierta = data.cajaBodega("ABIERTA", "100.00", usuario);
        flushAndClear();

        assertThat(cajaRepository.findFirstByEstado(EstadoCaja.ABIERTA))
                .hasValueSatisfying(c -> assertThat(c.getId()).isEqualTo(abierta));
    }

    @Test
    void findFirstByEstado_sinCajaAbierta_devuelveVacio() {
        data.cajaBodega("CERRADA", "50.00", usuario);

        assertThat(cajaRepository.findFirstByEstado(EstadoCaja.ABIERTA)).isEmpty();
    }

    @Test
    void soloPuedeHaberUnaCajaAbierta_laBaseRechazaLaSegunda() {
        data.cajaBodega("ABIERTA", "100.00", usuario);

        assertThatThrownBy(() -> data.cajaBodega("ABIERTA", "10.00", usuario))
                .isInstanceOf(PersistenceException.class)
                .hasStackTraceContaining("uq_bodega_caja_abierta");
    }

    // ---------- resumen (vista bodega.v_caja_resumen) ----------

    @Test
    void resumen_cuentaSoloVentasRegistradasYSeparaEfectivoDeDigital() {
        Long caja = data.cajaBodega("ABIERTA", "100.00", usuario);
        Long ventaOk = data.venta(caja, "REGISTRADA", "50.00", usuario);
        data.pagoVenta(ventaOk, "EFECTIVO", "30.00", null);
        data.pagoVenta(ventaOk, "YAPE", "20.00", "OP-1");
        Long ventaAnulada = data.venta(caja, "ANULADA", "99.00", usuario);
        data.pagoVenta(ventaAnulada, "EFECTIVO", "99.00", null);
        data.movimientoBodega(caja, "INGRESO", "EFECTIVO", "5.00", usuario);
        data.movimientoBodega(caja, "EGRESO", "EFECTIVO", "15.00", usuario);
        data.movimientoBodega(caja, "EGRESO", "PLIN", "8.00", usuario); // digital: no afecta al efectivo
        flushAndClear();

        CajaBodegaRepository.ResumenRow r = cajaRepository.resumen(caja).orElseThrow();

        assertThat(r.getCobrosEfectivo()).isEqualByComparingTo("30.00");
        assertThat(r.getCobrosDigitales()).isEqualByComparingTo("20.00");
        assertThat(r.getOtrosIngresosEfectivo()).isEqualByComparingTo("5.00");
        assertThat(r.getEgresosEfectivo()).isEqualByComparingTo("15.00");
        assertThat(r.getEfectivoEsperado()).isEqualByComparingTo("120.00"); // 100 + 30 + 5 - 15
        assertThat(r.getDiferencia()).isNull();
    }

    @Test
    void resumen_cajaSinMovimientos_efectivoEsperadoIgualALaApertura() {
        Long caja = data.cajaBodega("ABIERTA", "70.00", usuario);
        flushAndClear();

        CajaBodegaRepository.ResumenRow r = cajaRepository.resumen(caja).orElseThrow();

        assertThat(r.getCobrosEfectivo()).isEqualByComparingTo("0");
        assertThat(r.getEfectivoEsperado()).isEqualByComparingTo("70.00");
    }

    @Test
    void resumen_cajaCerrada_calculaLaDiferencia() {
        Long caja = data.cajaBodega("CERRADA", "60.00", usuario);
        flushAndClear();

        assertThat(cajaRepository.resumen(caja).orElseThrow().getDiferencia()).isEqualByComparingTo("0");
    }

    @Test
    void resumen_cajaInexistente_devuelveVacio() {
        assertThat(cajaRepository.resumen(-1L)).isEmpty();
    }

    // ---------- MovimientoBodegaRepository ----------

    @Test
    void findByCajaId_devuelveSoloLosMovimientosDeLaCajaPaginados() {
        Long caja = data.cajaBodega("ABIERTA", "100.00", usuario);
        Long otra = data.cajaBodega("CERRADA", "10.00", usuario);
        Long m1 = data.movimientoBodega(caja, "INGRESO", "EFECTIVO", "1.00", usuario);
        Long m2 = data.movimientoBodega(caja, "EGRESO", "EFECTIVO", "2.00", usuario);
        data.movimientoBodega(otra, "INGRESO", "EFECTIVO", "9.00", usuario);
        flushAndClear();

        Page<MovimientoBodegaEntity> page = movimientoRepository.findByCajaId(caja, PageRequest.of(0, 10, Sort.by("id")));

        assertThat(page.getContent()).extracting(MovimientoBodegaEntity::getId).containsExactly(m1, m2);
        assertThat(page.getTotalElements()).isEqualTo(2);
    }
}
