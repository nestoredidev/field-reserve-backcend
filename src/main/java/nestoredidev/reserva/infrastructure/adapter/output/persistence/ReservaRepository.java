package nestoredidev.reserva.infrastructure.adapter.output.persistence;

import jakarta.persistence.LockModeType;
import nestoredidev.reserva.domain.model.EstadoReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservaRepository extends JpaRepository<ReservaEntity, Long>, JpaSpecificationExecutor<ReservaEntity> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ReservaEntity r where r.id = :id")
    Optional<ReservaEntity> findByIdForUpdate(@Param("id") Long id);

    @Query("select r from ReservaEntity r where r.canchaId = :canchaId and r.estado <> :cancelada "
            + "and r.inicio < :fin and r.fin > :inicio and r.id <> :excluirId order by r.inicio")
    List<ReservaEntity> buscarCruces(@Param("canchaId") Long canchaId, @Param("inicio") OffsetDateTime inicio,
                                     @Param("fin") OffsetDateTime fin, @Param("excluirId") Long excluirId,
                                     @Param("cancelada") EstadoReserva cancelada);

    long countByClienteIdAndEstado(Long clienteId, EstadoReserva estado);

    List<ReservaEntity> findByEstadoAndFechaLimiteAdelantoBefore(EstadoReserva estado, OffsetDateTime limite);
}
