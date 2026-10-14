package nestoredidev.bodega.proveedor.infrastructure.adapter.output.persistence;

import jakarta.persistence.PersistenceException;
import nestoredidev.support.AbstractRepositoryTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProveedorRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private ProveedorRepository repository;

    @Test
    void existsByRuc_segunExista() {
        data.proveedor("Backus", "20123456789", true);

        assertThat(repository.existsByRuc("20123456789", -1L)).isTrue();
        assertThat(repository.existsByRuc("20999999999", -1L)).isFalse();
    }

    @Test
    void existsByRuc_conExcluirId_noCuentaAlPropioProveedor() {
        Long backus = data.proveedor("Backus", "20123456789", true);
        Long otro = data.proveedor("Otro", "20555555555", true);

        assertThat(repository.existsByRuc("20123456789", backus)).isFalse();
        assertThat(repository.existsByRuc("20123456789", otro)).isTrue();
    }

    @Test
    void existsByRuc_proveedoresSinRucNoCuentan() {
        data.proveedor("Informal", null, true);

        assertThat(repository.existsByRuc("20123456789", -1L)).isFalse();
    }

    @Test
    void rucDuplicado_laBaseLoRechaza() {
        data.proveedor("Backus", "20123456789", true);

        assertThatThrownBy(() -> data.proveedor("Otro", "20123456789", true))
                .isInstanceOf(PersistenceException.class)
                .hasStackTraceContaining("ruc");
    }

    @Test
    void rucConFormatoInvalido_laBaseLoRechaza() {
        assertThatThrownBy(() -> data.proveedor("Malo", "123", true))
                .isInstanceOf(PersistenceException.class);
    }

    @Test
    void variosProveedoresSinRuc_sonValidos() {
        data.proveedor("A", null, true);

        assertThat(data.proveedor("B", null, true)).isNotNull();
    }
}
