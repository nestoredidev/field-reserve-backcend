package nestoredidev.bodega.venta.infrastructure.adapter.output.persistence;

import nestoredidev.bodega.venta.domain.model.EstadoVenta;
import nestoredidev.common.domain.MetodoPago;
import nestoredidev.support.AbstractRepositoryTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class VentaRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private VentaRepository ventaRepository;
    @Autowired
    private DetalleVentaRepository detalleRepository;
    @Autowired
    private PagoVentaRepository pagoRepository;

    private Long usuario;
    private Long caja;
    private Long producto;
    private Long venta;

    @BeforeEach
    void datos() {
        usuario = data.usuario();
        caja = data.cajaBodega("ABIERTA", "10.00", usuario);
        producto = data.producto("1", "Cristal", data.categoria(), "PRODUCTO", 10, 1, null, true);
        venta = data.venta(caja, "REGISTRADA", "9.00", usuario);
    }

    @Test
    void findByIdForUpdate_devuelveLaVentaOVacio() {
        flushAndClear();

        assertThat(ventaRepository.findByIdForUpdate(venta)).hasValueSatisfying(v -> {
            assertThat(v.getEstado()).isEqualTo(EstadoVenta.REGISTRADA);
            assertThat(v.getCajaId()).isEqualTo(caja);
            assertThat(v.getTotal()).isEqualByComparingTo("9.00");
        });
        assertThat(ventaRepository.findByIdForUpdate(-1L)).isEmpty();
    }

    @Test
    void findByVentaIdOrderByIdAsc_detalles_ordenaPorIdYSoloDeLaVenta() {
        Long otroProducto = data.producto("2", "Pilsen", data.categoria(), "PRODUCTO", 10, 1, null, true);
        Long otraVenta = data.venta(caja, "REGISTRADA", "3.00", usuario);
        Long d1 = data.detalleVenta(venta, producto, 2, "3.00", "2.00");
        Long d2 = data.detalleVenta(venta, otroProducto, 1, "3.00", "2.00");
        data.detalleVenta(otraVenta, producto, 1, "3.00", "2.00");
        flushAndClear();

        assertThat(detalleRepository.findByVentaIdOrderByIdAsc(venta)).extracting(DetalleVentaEntity::getId)
                .containsExactly(d1, d2);
    }

    @Test
    void detalles_elSubtotalEsUnaColumnaGeneradaPorLaBase() {
        Long detalle = data.detalleVenta(venta, producto, 3, "3.50", "2.00");
        flushAndClear();

        Number subtotal = (Number) em.createNativeQuery("select subtotal from bodega.detalle_venta where id = " + detalle)
                .getSingleResult();
        assertThat(subtotal.doubleValue()).isEqualTo(10.5);
    }

    @Test
    void findByVentaIdOrderByIdAsc_pagos_ordenaPorId() {
        Long p1 = data.pagoVenta(venta, "EFECTIVO", "5.00", null);
        Long p2 = data.pagoVenta(venta, "YAPE", "4.00", "OP-1");
        Long otraVenta = data.venta(caja, "REGISTRADA", "3.00", usuario);
        data.pagoVenta(otraVenta, "EFECTIVO", "3.00", null);
        flushAndClear();

        assertThat(pagoRepository.findByVentaIdOrderByIdAsc(venta)).extracting(PagoVentaEntity::getId)
                .containsExactly(p1, p2);
    }

    @Test
    void existsByMetodoAndNroOperacion_distingueMetodoYNumero() {
        data.pagoVenta(venta, "YAPE", "9.00", "OP-1");

        assertThat(pagoRepository.existsByMetodoAndNroOperacion(MetodoPago.YAPE, "OP-1")).isTrue();
        assertThat(pagoRepository.existsByMetodoAndNroOperacion(MetodoPago.PLIN, "OP-1")).isFalse();
        assertThat(pagoRepository.existsByMetodoAndNroOperacion(MetodoPago.YAPE, "OP-2")).isFalse();
    }
}
