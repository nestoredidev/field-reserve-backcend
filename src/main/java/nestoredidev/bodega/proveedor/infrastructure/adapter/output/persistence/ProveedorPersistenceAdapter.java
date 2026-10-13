package nestoredidev.bodega.proveedor.infrastructure.adapter.output.persistence;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import nestoredidev.bodega.proveedor.application.ports.output.ProveedorPersistencePort;
import nestoredidev.bodega.proveedor.domain.model.Proveedor;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;
import nestoredidev.common.infrastructure.persistence.PageAdapter;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProveedorPersistenceAdapter implements ProveedorPersistencePort {
    private final ProveedorRepository repository;

    @Override
    public Proveedor guardar(Proveedor p) {
        ProveedorEntity e = p.getId() == null
                ? new ProveedorEntity()
                : repository.findById(p.getId()).orElseGet(ProveedorEntity::new);
        e.setNombre(p.getNombre());
        e.setRuc(p.getRuc());
        e.setTelefono(p.getTelefono());
        e.setActivo(p.isActivo());
        return toDomain(repository.saveAndFlush(e));
    }

    @Override
    public Optional<Proveedor> buscarPorId(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public boolean existePorRuc(String ruc, Long excluirId) {
        return repository.existsByRuc(ruc, excluirId == null ? -1L : excluirId);
    }

    @Override
    public PageResult<Proveedor> listar(String q, Boolean activo, PageQuery query) {
        Specification<ProveedorEntity> spec = (root, cq, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            if (q != null) {
                String like = "%" + q.toLowerCase() + "%";
                preds.add(cb.or(cb.like(cb.lower(root.get("nombre")), like), cb.like(root.get("ruc"), "%" + q + "%")));
            }
            if (activo != null) preds.add(cb.equal(root.get("activo"), activo));
            return cb.and(preds.toArray(new Predicate[0]));
        };
        return PageAdapter.toResult(repository.findAll(spec, PageAdapter.toPageable(query)), this::toDomain);
    }

    private Proveedor toDomain(ProveedorEntity e) {
        return Proveedor.restaurar(e.getId(), e.getNombre(), e.getRuc(), e.getTelefono(),
                Boolean.TRUE.equals(e.getActivo()), e.getCreatedAt());
    }
}
