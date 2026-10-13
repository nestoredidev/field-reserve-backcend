package nestoredidev.usuario.application.ports.output;

import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;
import nestoredidev.usuario.domain.model.Usuario;

import java.util.Optional;

public interface UsuarioPersistencePort {

    Usuario guardar(Usuario usuario);

    Optional<Usuario> buscarPorId(Long id);

    Optional<Usuario> buscarPorUsername(String username);

    boolean existePorUsername(String username);

    PageResult<Usuario> listar(PageQuery query);

    long contarAdminsActivos();
}
