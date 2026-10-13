package nestoredidev.cancha.infrastructure.adapter.output.persistence;

import lombok.RequiredArgsConstructor;
import nestoredidev.cancha.application.ports.output.CanchaPersistencePort;
import nestoredidev.cancha.domain.model.Cancha;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;
import nestoredidev.common.infrastructure.persistence.PageAdapter;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CanchaPersistenceAdapter implements CanchaPersistencePort {
    private final CanchaRepository repository;

    @Override
    public Cancha guardar(Cancha cancha) {
        CanchaEntity e = cancha.getId() == null
                ? new CanchaEntity()
                : repository.findById(cancha.getId()).orElseGet(CanchaEntity::new);
        e.setNombre(cancha.getNombre());
        e.setDescripcion(cancha.getDescripcion());
        e.setActiva(cancha.isActiva());
        return toDomain(repository.saveAndFlush(e));
    }

    @Override
    public Optional<Cancha> buscarPorId(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public boolean existePorNombre(String nombre, Long excluirId) {
        return repository.existsByNombre(nombre, excluirId);
    }

    @Override
    public PageResult<Cancha> listar(Boolean activa, PageQuery query) {
        return PageAdapter.toResult(repository.buscar(activa, PageAdapter.toPageable(query)), this::toDomain);
    }

    @Override
    public boolean tieneOcupacionFutura(Long canchaId, OffsetDateTime desde) {
        return repository.tieneOcupacionFutura(canchaId, desde);
    }

    private Cancha toDomain(CanchaEntity e) {
        return Cancha.restaurar(e.getId(), e.getNombre(), e.getDescripcion(), Boolean.TRUE.equals(e.getActiva()),
                e.getCreatedAt());
    }
}
