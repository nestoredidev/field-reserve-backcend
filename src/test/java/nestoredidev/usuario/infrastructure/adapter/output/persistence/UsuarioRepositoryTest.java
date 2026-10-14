package nestoredidev.usuario.infrastructure.adapter.output.persistence;

import jakarta.persistence.PersistenceException;
import nestoredidev.common.domain.Rol;
import nestoredidev.support.AbstractRepositoryTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UsuarioRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private UsuarioRepository repository;

    @Test
    void findByUsername_existente_devuelveElUsuario() {
        Long id = data.usuario("maria", "ENCARGADO", true);
        flushAndClear();

        assertThat(repository.findByUsername("maria")).hasValueSatisfying(u -> {
            assertThat(u.getId()).isEqualTo(id);
            assertThat(u.getRol()).isEqualTo(Rol.ENCARGADO);
            assertThat(u.getActivo()).isTrue();
        });
    }

    @Test
    void findByUsername_inexistenteOConOtraCapitalizacion_devuelveVacio() {
        data.usuario("maria", "ENCARGADO", true);

        assertThat(repository.findByUsername("nadie")).isEmpty();
        assertThat(repository.findByUsername("MARIA")).isEmpty(); // el username se guarda en minúsculas
    }

    @Test
    void existsByUsername_segunExista() {
        data.usuario("maria", "ENCARGADO", true);

        assertThat(repository.existsByUsername("maria")).isTrue();
        assertThat(repository.existsByUsername("otro")).isFalse();
    }

    @Test
    void countByRolAndActivoTrue_cuentaSoloLosActivosDelRol() {
        data.usuario("admin1", "ADMIN", true);
        data.usuario("admin2", "ADMIN", true);
        data.usuario("admin3", "ADMIN", false);
        data.usuario("enc1", "ENCARGADO", true);

        assertThat(repository.countByRolAndActivoTrue(Rol.ADMIN)).isEqualTo(2);
        assertThat(repository.countByRolAndActivoTrue(Rol.ENCARGADO)).isEqualTo(1);
    }

    @Test
    void countByRolAndActivoTrue_sinUsuariosDelRol_esCero() {
        data.usuario("admin1", "ADMIN", true);

        assertThat(repository.countByRolAndActivoTrue(Rol.ENCARGADO)).isZero();
    }

    @Test
    void usernameDuplicado_laBaseLoRechaza() {
        data.usuario("maria", "ENCARGADO", true);

        assertThatThrownBy(() -> data.usuario("maria", "ADMIN", true))
                .isInstanceOf(PersistenceException.class)
                .hasStackTraceContaining("username");
    }
}
