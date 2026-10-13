package nestoredidev.cancha.infrastructure.adapter.output.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;

public interface CanchaRepository extends JpaRepository<CanchaEntity, Long> {

    @Query("select count(c) > 0 from CanchaEntity c where lower(c.nombre) = lower(:nombre) "
            + "and (:excluirId is null or c.id <> :excluirId)")
    boolean existsByNombre(@Param("nombre") String nombre, @Param("excluirId") Long excluirId);

    @Query("select c from CanchaEntity c where (:activa is null or c.activa = :activa)")
    Page<CanchaEntity> buscar(@Param("activa") Boolean activa, Pageable pageable);

    @Query(value = "select count(*) > 0 from cancha.reserva where cancha_id = :canchaId "
            + "and estado in ('PENDIENTE','CONFIRMADA') and fin > :desde", nativeQuery = true)
    boolean tieneOcupacionFutura(@Param("canchaId") Long canchaId, @Param("desde") OffsetDateTime desde);
}
