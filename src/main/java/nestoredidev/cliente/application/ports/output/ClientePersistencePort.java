package nestoredidev.cliente.application.ports.output;

import nestoredidev.cliente.domain.model.Cliente;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ClientePersistencePort {

    Cliente guardar(Cliente cliente);

    Optional<Cliente> buscarPorId(Long id);

    Optional<Cliente> buscarPorTelefono(String telefono);

    List<Cliente> buscarPorIds(Collection<Long> ids);

    boolean existePorTelefono(String telefono, Long excluirId);

    PageResult<Cliente> listar(String q, PageQuery query);
}
