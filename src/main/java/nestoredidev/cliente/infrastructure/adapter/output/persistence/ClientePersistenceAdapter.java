package nestoredidev.cliente.infrastructure.adapter.output.persistence;

import lombok.RequiredArgsConstructor;
import nestoredidev.cliente.application.ports.output.ClientePersistencePort;
import nestoredidev.cliente.domain.model.Cliente;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;
import nestoredidev.common.infrastructure.persistence.PageAdapter;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ClientePersistenceAdapter implements ClientePersistencePort {
    private final ClienteRepository repository;

    @Override
    public Cliente guardar(Cliente cliente) {
        ClienteEntity e = cliente.getId() == null
                ? new ClienteEntity()
                : repository.findById(cliente.getId()).orElseGet(ClienteEntity::new);
        e.setNombre(cliente.getNombre());
        e.setTelefono(cliente.getTelefono());
        e.setNotas(cliente.getNotas());
        return toDomain(repository.saveAndFlush(e));
    }

    @Override
    public Optional<Cliente> buscarPorId(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Cliente> buscarPorTelefono(String telefono) {
        return repository.findByTelefono(telefono).map(this::toDomain);
    }

    @Override
    public List<Cliente> buscarPorIds(Collection<Long> ids) {
        return repository.findAllById(ids).stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existePorTelefono(String telefono, Long excluirId) {
        return repository.existsByTelefono(telefono, excluirId);
    }

    @Override
    public PageResult<Cliente> listar(String q, PageQuery query) {
        return PageAdapter.toResult(repository.buscar(q, PageAdapter.toPageable(query)), this::toDomain);
    }

    private Cliente toDomain(ClienteEntity e) {
        return Cliente.restaurar(e.getId(), e.getNombre(), e.getTelefono(), e.getNotas(), e.getCreatedAt());
    }
}
