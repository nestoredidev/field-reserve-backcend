package nestoredidev.bodega.categoria.infrastructure.adapter.output.persistence;

import lombok.RequiredArgsConstructor;
import nestoredidev.bodega.categoria.application.ports.output.CategoriaPersistencePort;
import nestoredidev.bodega.categoria.domain.model.Categoria;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;
import nestoredidev.common.infrastructure.persistence.PageAdapter;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CategoriaPersistenceAdapter implements CategoriaPersistencePort {
    private final CategoriaRepository repository;

    @Override
    public Categoria guardar(Categoria categoria) {
        CategoriaEntity e = categoria.getId() == null
                ? new CategoriaEntity()
                : repository.findById(categoria.getId()).orElseGet(CategoriaEntity::new);
        e.setNombre(categoria.getNombre());
        return toDomain(repository.saveAndFlush(e));
    }

    @Override
    public Optional<Categoria> buscarPorId(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public boolean existePorNombre(String nombre, Long excluirId) {
        return repository.existsByNombre(nombre, excluirId == null ? -1L : excluirId);
    }

    @Override
    public PageResult<Categoria> listar(PageQuery query) {
        return PageAdapter.toResult(repository.findAll(PageAdapter.toPageable(query)), this::toDomain);
    }

    @Override
    public boolean tieneProductos(Long id) {
        return repository.tieneProductos(id);
    }

    @Override
    public void eliminar(Long id) {
        repository.deleteById(id);
        repository.flush();
    }

    private Categoria toDomain(CategoriaEntity e) {
        return Categoria.restaurar(e.getId(), e.getNombre());
    }
}
