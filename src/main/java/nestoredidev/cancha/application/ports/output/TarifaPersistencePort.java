package nestoredidev.cancha.application.ports.output;

import nestoredidev.cancha.domain.model.Tarifa;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface TarifaPersistencePort {

    Tarifa guardar(Tarifa tarifa);

    Optional<Tarifa> buscarPorId(Long id);

    List<Tarifa> listarPorCancha(Long canchaId, boolean soloActivas);

    /** RN-TAR-01: franja activa del mismo día y cancha que se cruza con [inicio, fin). */
    boolean existeCruce(Long canchaId, Short diaSemana, LocalTime inicio, LocalTime fin, Long excluirId);
}
