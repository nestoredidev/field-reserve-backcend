package nestoredidev.cliente.infrastructure.adapter.output.persistence;

import jakarta.persistence.PersistenceException;
import nestoredidev.support.AbstractRepositoryTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClienteRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private ClienteRepository repository;

    private Long ana;
    private Long luis;

    @BeforeEach
    void datos() {
        ana = data.cliente("Ana Torres", "911111111");
        luis = data.cliente("Luis Ramos", "922222233");
        flushAndClear();
    }

    @Test
    void findByTelefono_existenteEInexistente() {
        assertThat(repository.findByTelefono("911111111")).hasValueSatisfying(c -> assertThat(c.getId()).isEqualTo(ana));
        assertThat(repository.findByTelefono("999999999")).isEmpty();
    }

    @Test
    void existsByTelefono_sinExcluir() {
        assertThat(repository.existsByTelefono("911111111", null)).isTrue();
        assertThat(repository.existsByTelefono("999999999", null)).isFalse();
    }

    @Test
    void existsByTelefono_conExcluirId_noCuentaAlPropioCliente() {
        assertThat(repository.existsByTelefono("911111111", ana)).isFalse();
        assertThat(repository.existsByTelefono("911111111", luis)).isTrue();
    }

    @Test
    void buscar_sinTexto_devuelveTodos() {
        Page<ClienteEntity> page = repository.buscar(null, PageRequest.of(0, 10, Sort.by("nombre")));

        assertThat(page.getContent()).extracting(ClienteEntity::getId).containsExactly(ana, luis);
    }

    @Test
    void buscar_porNombreSinImportarMayusculas() {
        assertThat(repository.buscar("TORRES", PageRequest.of(0, 10)).getContent())
                .extracting(ClienteEntity::getId).containsExactly(ana);
        assertThat(repository.buscar("lu", PageRequest.of(0, 10)).getContent())
                .extracting(ClienteEntity::getId).containsExactly(luis);
    }

    @Test
    void buscar_porFragmentoDeTelefono() {
        assertThat(repository.buscar("2233", PageRequest.of(0, 10)).getContent())
                .extracting(ClienteEntity::getId).containsExactly(luis);
    }

    @Test
    void buscar_sinCoincidencias_devuelveVacio() {
        assertThat(repository.buscar("zzz", PageRequest.of(0, 10)).getContent()).isEmpty();
    }

    @Test
    void buscar_paginaLosResultados() {
        Page<ClienteEntity> page = repository.buscar(null, PageRequest.of(1, 1, Sort.by("nombre")));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(ClienteEntity::getId).containsExactly(luis);
    }

    @Test
    void telefonoDuplicado_laBaseLoRechaza() {
        assertThatThrownBy(() -> data.cliente("Otra", "911111111"))
                .isInstanceOf(PersistenceException.class)
                .hasStackTraceContaining("telefono");
    }
}
