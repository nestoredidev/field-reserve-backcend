package nestoredidev.usuario.infrastructure.adapter.output.persistence;

import lombok.RequiredArgsConstructor;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;
import nestoredidev.common.domain.Rol;
import nestoredidev.common.infrastructure.persistence.PageAdapter;
import nestoredidev.usuario.application.ports.output.UsuarioPersistencePort;
import nestoredidev.usuario.domain.model.Usuario;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UsuarioPersistenceAdapter implements UsuarioPersistencePort {
    private final UsuarioRepository repository;

    @Override
    public Usuario guardar(Usuario usuario) {
        UsuarioEntity e = usuario.getId() == null
                ? new UsuarioEntity()
                : repository.findById(usuario.getId()).orElseGet(UsuarioEntity::new);
        e.setNombre(usuario.getNombre());
        e.setUsername(usuario.getUsername());
        e.setPasswordHash(usuario.getPasswordHash());
        e.setRol(usuario.getRol());
        e.setActivo(usuario.isActivo());
        return toDomain(repository.saveAndFlush(e));
    }

    @Override
    public Optional<Usuario> buscarPorId(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Usuario> buscarPorUsername(String username) {
        return repository.findByUsername(username).map(this::toDomain);
    }

    @Override
    public boolean existePorUsername(String username) {
        return repository.existsByUsername(username);
    }

    @Override
    public PageResult<Usuario> listar(PageQuery query) {
        return PageAdapter.toResult(repository.findAll(PageAdapter.toPageable(query)), this::toDomain);
    }

    @Override
    public long contarAdminsActivos() {
        return repository.countByRolAndActivoTrue(Rol.ADMIN);
    }

    private Usuario toDomain(UsuarioEntity e) {
        return Usuario.restaurar(e.getId(), e.getNombre(), e.getUsername(), e.getPasswordHash(), e.getRol(),
                Boolean.TRUE.equals(e.getActivo()), e.getCreatedAt());
    }
}
