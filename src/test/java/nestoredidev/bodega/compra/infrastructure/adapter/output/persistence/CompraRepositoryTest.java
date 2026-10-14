package nestoredidev.bodega.compra.infrastructure.adapter.output.persistence;

import nestoredidev.bodega.compra.domain.model.EstadoCompra;
import nestoredidev.support.AbstractRepositoryTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class CompraRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private CompraRepository compraRepository;
    @Autowired
    private DetalleCompraRepository detalleRepository;

    private Long usuario;
    private Long compra;
    private Long producto;

    @BeforeEach
    void datos() {
        usuario = data.usuario();
        Long proveedor = data.proveedor("Backus", "20123456789", true);
        producto = data.producto("1", "Cristal", data.categoria(), "PRODUCTO", 0, 0, null, true);
        compra = data.compra(proveedor, "REGISTRADA", "120.00", usuario);
    }

    @Test
    void findByIdForUpdate_devuelveLaCompraOVacio() {
        flushAndClear();

        assertThat(compraRepository.findByIdForUpdate(compra)).hasValueSatisfying(c -> {
            assertThat(c.getEstado()).isEqualTo(EstadoCompra.REGISTRADA);
            assertThat(c.getTotal()).isEqualByComparingTo("120.00");
        });
        assertThat(compraRepository.findByIdForUpdate(-1L)).isEmpty();
    }

    @Test
    void findByCompraIdOrderByIdAsc_ordenaPorIdYSoloDeLaCompra() {
        Long otroProducto = data.producto("2", "Pilsen", data.categoria(), "PRODUCTO", 0, 0, null, true);
        Long otraCompra = data.compra(null, "REGISTRADA", "10.00", usuario);
        Long d1 = data.detalleCompra(compra, producto, 24, "2.50");
        Long d2 = data.detalleCompra(compra, otroProducto, 12, "5.00");
        data.detalleCompra(otraCompra, producto, 5, "2.00");
        flushAndClear();

        assertThat(detalleRepository.findByCompraIdOrderByIdAsc(compra)).extracting(DetalleCompraEntity::getId)
                .containsExactly(d1, d2);
    }

    @Test
    void detalleCompra_elSubtotalEsUnaColumnaGeneradaPorLaBase() {
        Long detalle = data.detalleCompra(compra, producto, 24, "2.50");
        flushAndClear();

        Number subtotal = (Number) em.createNativeQuery("select subtotal from bodega.detalle_compra where id = " + detalle)
                .getSingleResult();
        assertThat(subtotal.doubleValue()).isEqualTo(60.0);
    }

    @Test
    void compraSinProveedor_esValida() {
        assertThat(data.compra(null, "REGISTRADA", "5.00", usuario)).isNotNull();
    }
}
