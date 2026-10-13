package nestoredidev.cancha.infrastructure.adapter.output.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalTime;
import java.util.List;

public interface TarifaRepository extends JpaRepository<TarifaEntity, Long> {

    List<TarifaEntity> findByCanchaIdOrderByDiaSemanaAscHoraInicioAsc(Long canchaId);

    List<TarifaEntity> findByCanchaIdAndActivaTrueOrderByDiaSemanaAscHoraInicioAsc(Long canchaId);

    @Query("select count(t) > 0 from TarifaEntity t where t.canchaId = :canchaId and t.activa = true "
            + "and coalesce(t.diaSemana, 0) = :dia and t.horaInicio < :fin and t.horaFin > :inicio "
            + "and (:excluirId is null or t.id <> :excluirId)")
    boolean existeCruce(@Param("canchaId") Long canchaId, @Param("dia") int dia,
                        @Param("inicio") LocalTime inicio, @Param("fin") LocalTime fin,
                        @Param("excluirId") Long excluirId);
}
