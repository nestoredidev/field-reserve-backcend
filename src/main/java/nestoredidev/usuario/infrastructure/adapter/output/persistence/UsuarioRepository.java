package nestoredidev.usuario.infrastructure.adapter.output.persistence;

import nestoredidev.common.domain.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<UsuarioEntity, Long> {
    Optional<UsuarioEntity> findByUsername(String username);

    boolean existsByUsername(String username);

    long countByRolAndActivoTrue(Rol rol);
}
