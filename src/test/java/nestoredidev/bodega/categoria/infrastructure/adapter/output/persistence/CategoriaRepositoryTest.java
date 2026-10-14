package nestoredidev.bodega.categoria.infrastructure.adapter.output.persistence;

import nestoredidev.support.AbstractRepositoryTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class CategoriaRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private CategoriaRepository repository;

    @Test
    void existsByNombre_ignoraMayusculas() {
        data.categoria("Cerveza");

        assertThat(repository.existsByNombre("cerveza", -1L)).isTrue();
        assertThat(repository.existsByNombre("CERVEZA", -1L)).isTrue();
        assertThat(repository.existsByNombre("Gaseosa", -1L)).isFalse();
    }

    @Test
    void existsByNombre_conExcluirId_noCuentaLaPropiaCategoria() {
        Long cerveza = data.categoria("Cerveza");
        Long agua = data.categoria("Agua");

        assertThat(repository.existsByNombre("Cerveza", cerveza)).isFalse();
        assertThat(repository.existsByNombre("Cerveza", agua)).isTrue();
    }

    @Test
    void tieneProductos_segunTengaProductosAsociados() {
        Long conProductos = data.categoria("Cerveza");
        Long sinProductos = data.categoria("Agua");
        data.producto("111", "Cristal", conProductos, "PRODUCTO", 5, 1, null, true);

        assertThat(repository.tieneProductos(conProductos)).isTrue();
        assertThat(repository.tieneProductos(sinProductos)).isFalse();
    }

    @Test
    void tieneProductos_tambienCuentaProductosInactivos() {
        Long categoria = data.categoria("Cerveza");
        data.producto("111", "Cristal", categoria, "PRODUCTO", 0, 0, null, false);

        assertThat(repository.tieneProductos(categoria)).isTrue();
    }
}
