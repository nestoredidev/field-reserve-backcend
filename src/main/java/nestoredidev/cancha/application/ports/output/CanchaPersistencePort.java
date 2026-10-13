package nestoredidev.cancha.application.ports.output;

import nestoredidev.cancha.domain.model.Cancha;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface CanchaPersistencePort {

    Cancha guardar(Cancha cancha);

    Optional<Cancha> buscarPorId(Long id);

    boolean existePorNombre(String nombre, Long excluirId);

    PageResult<Cancha> listar(Boolean activa, PageQuery query);

    /** RN-CAN-03: reservas o mantenimientos PENDIENTE/CONFIRMADA que empiezan después de {@code desde}. */
    boolean tieneOcupacionFutura(Long canchaId, OffsetDateTime desde);
}
