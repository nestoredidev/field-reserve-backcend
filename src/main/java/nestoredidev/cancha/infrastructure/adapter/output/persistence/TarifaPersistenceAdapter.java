package nestoredidev.cancha.infrastructure.adapter.output.persistence;

import lombok.RequiredArgsConstructor;
import nestoredidev.cancha.application.ports.output.TarifaPersistencePort;
import nestoredidev.cancha.domain.model.Tarifa;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TarifaPersistenceAdapter implements TarifaPersistencePort {
    private final TarifaRepository repository;

    @Override
    public Tarifa guardar(Tarifa tarifa) {
        TarifaEntity e = tarifa.getId() == null
                ? new TarifaEntity()
                : repository.findById(tarifa.getId()).orElseGet(TarifaEntity::new);
        e.setCanchaId(tarifa.getCanchaId());
        e.setDiaSemana(tarifa.getDiaSemana());
        e.setHoraInicio(tarifa.getHoraInicio());
        e.setHoraFin(tarifa.getHoraFin());
        e.setPrecioHora(tarifa.getPrecioHora());
        e.setActiva(tarifa.isActiva());
        return toDomain(repository.saveAndFlush(e));
    }

    @Override
    public Optional<Tarifa> buscarPorId(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Tarifa> listarPorCancha(Long canchaId, boolean soloActivas) {
        List<TarifaEntity> list = soloActivas
                ? repository.findByCanchaIdAndActivaTrueOrderByDiaSemanaAscHoraInicioAsc(canchaId)
                : repository.findByCanchaIdOrderByDiaSemanaAscHoraInicioAsc(canchaId);
        return list.stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existeCruce(Long canchaId, Short diaSemana, LocalTime inicio, LocalTime fin, Long excluirId) {
        return repository.existeCruce(canchaId, diaSemana == null ? 0 : diaSemana, inicio, fin, excluirId);
    }

    private Tarifa toDomain(TarifaEntity e) {
        return Tarifa.restaurar(e.getId(), e.getCanchaId(), e.getDiaSemana(), e.getHoraInicio(), e.getHoraFin(),
                e.getPrecioHora(), Boolean.TRUE.equals(e.getActiva()), e.getCreatedAt());
    }
}
