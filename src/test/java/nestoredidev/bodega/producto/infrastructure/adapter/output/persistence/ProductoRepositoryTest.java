package nestoredidev.bodega.producto.infrastructure.adapter.output.persistence;

import jakarta.persistence.PersistenceException;
import nestoredidev.bodega.producto.domain.model.TipoMovimientoInventario;
import nestoredidev.bodega.producto.domain.model.TipoProducto;
import nestoredidev.support.AbstractRepositoryTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductoRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private ProductoRepository repository;
    @Autowired
    private MovimientoInventarioRepository movimientoRepository;

    private Long categoria;
    private Long usuario;

    @BeforeEach
    void datos() {
        categoria = data.categoria();
        usuario = data.usuario();
    }

    // ---------- búsquedas por código ----------

    @Test
    void findByCodigoBarras_existenteEInexistente() {
        Long id = data.producto("775001", "Cristal", categoria, "PRODUCTO", 10, 2, null, true);
        flushAndClear();

        assertThat(repository.findByCodigoBarras("775001")).hasValueSatisfying(p -> {
            assertThat(p.getId()).isEqualTo(id);
            assertThat(p.getTipo()).isEqualTo(TipoProducto.PRODUCTO);
            assertThat(p.getStock()).isEqualTo(10);
        });
        assertThat(repository.findByCodigoBarras("000")).isEmpty();
    }

    @Test
    void existsByCodigoBarras_conExcluirId_noCuentaAlPropioProducto() {
        Long cristal = data.producto("775001", "Cristal", categoria, "PRODUCTO", 10, 2, null, true);
        Long pilsen = data.producto("775002", "Pilsen", categoria, "PRODUCTO", 10, 2, null, true);

        assertThat(repository.existsByCodigoBarras("775001", -1L)).isTrue();
        assertThat(repository.existsByCodigoBarras("775001", cristal)).isFalse();
        assertThat(repository.existsByCodigoBarras("775001", pilsen)).isTrue();
        assertThat(repository.existsByCodigoBarras("999", -1L)).isFalse();
    }

    @Test
    void codigoDeBarrasDuplicado_laBaseLoRechaza() {
        data.producto("775001", "Cristal", categoria, "PRODUCTO", 10, 2, null, true);

        assertThatThrownBy(() -> data.producto("775001", "Otro", categoria, "PRODUCTO", 1, 0, null, true))
                .isInstanceOf(PersistenceException.class)
                .hasStackTraceContaining("codigo_barras");
    }

    @Test
    void productosSinCodigoDeBarras_puedenRepetirseNulo() {
        data.producto(null, "A", categoria, "PRODUCTO", 1, 0, null, true);

        assertThat(data.producto(null, "B", categoria, "PRODUCTO", 1, 0, null, true)).isNotNull();
    }

    // ---------- envases ----------

    @Test
    void existsByEnvaseId_soloSiAlgunProductoLoUsaComoEnvase() {
        Long envase = data.producto("E1", "Envase 620", categoria, "ENVASE", 20, 0, null, true);
        Long envaseLibre = data.producto("E2", "Envase 330", categoria, "ENVASE", 5, 0, null, true);
        data.producto("775001", "Cristal 620", categoria, "PRODUCTO", 10, 2, envase, true);

        assertThat(repository.existsByEnvaseId(envase)).isTrue();
        assertThat(repository.existsByEnvaseId(envaseLibre)).isFalse();
    }

    // ---------- stockBajo ----------

    @Test
    void stockBajo_devuelveActivosConStockMenorOIgualAlMinimoOrdenadosPorNombre() {
        Long zeta = data.producto("1", "Zeta", categoria, "PRODUCTO", 2, 5, null, true);   // bajo
        Long alfa = data.producto("2", "Alfa", categoria, "PRODUCTO", 5, 5, null, true);   // igual al mínimo
        data.producto("3", "Medio", categoria, "PRODUCTO", 6, 5, null, true);              // suficiente
        data.producto("4", "Inactivo", categoria, "PRODUCTO", 0, 5, null, false);          // inactivo: no se avisa
        flushAndClear();

        assertThat(repository.stockBajo()).extracting(ProductoEntity::getId).containsExactly(alfa, zeta);
    }

    @Test
    void stockBajo_productoSinStockMinimoYSinStock_cuentaComoBajo() {
        Long agotado = data.producto("1", "Agotado", categoria, "PRODUCTO", 0, 0, null, true);

        assertThat(repository.stockBajo()).extracting(ProductoEntity::getId).containsExactly(agotado);
    }

    @Test
    void stockBajo_sinProductosBajos_devuelveVacio() {
        data.producto("1", "Lleno", categoria, "PRODUCTO", 50, 5, null, true);

        assertThat(repository.stockBajo()).isEmpty();
    }

    // ---------- findByIdForUpdate ----------

    @Test
    void findByIdForUpdate_devuelveElProductoOVacio() {
        Long id = data.producto("1", "Cristal", categoria, "PRODUCTO", 10, 2, null, true);
        flushAndClear();

        assertThat(repository.findByIdForUpdate(id)).hasValueSatisfying(p -> assertThat(p.getNombre()).isEqualTo("Cristal"));
        assertThat(repository.findByIdForUpdate(-1L)).isEmpty();
    }

    // ---------- constraints ----------

    @Test
    void stockNegativo_laBaseLoRechaza() {
        assertThatThrownBy(() -> data.producto("1", "Malo", categoria, "PRODUCTO", -1, 0, null, true))
                .isInstanceOf(PersistenceException.class);
    }

    // ---------- MovimientoInventarioRepository ----------

    @Test
    void findByVentaIdOrderByIdAsc_devuelveLosMovimientosDeEsaVentaEnOrden() {
        Long caja = data.cajaBodega("ABIERTA", "10.00", usuario);
        Long producto = data.producto("1", "Cristal", categoria, "PRODUCTO", 10, 2, null, true);
        Long venta = data.venta(caja, "REGISTRADA", "6.00", usuario);
        Long otraVenta = data.venta(caja, "REGISTRADA", "3.00", usuario);
        Long m1 = data.movimientoInventario(producto, "SALIDA_VENTA", 2, 10, 8, null, venta, usuario);
        Long m2 = data.movimientoInventario(producto, "DEVOLUCION_ENVASE", 2, 8, 10, null, venta, usuario);
        data.movimientoInventario(producto, "SALIDA_VENTA", 1, 10, 9, null, otraVenta, usuario);
        data.movimientoInventario(producto, "AJUSTE_POSITIVO", 5, 9, 14, null, null, usuario);
        flushAndClear();

        List<MovimientoInventarioEntity> res = movimientoRepository.findByVentaIdOrderByIdAsc(venta);

        assertThat(res).extracting(MovimientoInventarioEntity::getId).containsExactly(m1, m2);
        assertThat(res.get(0).getTipo()).isEqualTo(TipoMovimientoInventario.SALIDA_VENTA);
    }

    @Test
    void findByVentaIdOrderByIdAsc_ventaSinMovimientos_devuelveVacio() {
        assertThat(movimientoRepository.findByVentaIdOrderByIdAsc(-1L)).isEmpty();
    }
}
