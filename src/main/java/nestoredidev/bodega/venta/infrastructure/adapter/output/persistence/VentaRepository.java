package nestoredidev.bodega.venta.infrastructure.adapter.output.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface VentaRepository extends JpaRepository<VentaEntity, Long>, JpaSpecificationExecutor<VentaEntity> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from VentaEntity v where v.id = :id")
    Optional<VentaEntity> findByIdForUpdate(@Param("id") Long id);
}
