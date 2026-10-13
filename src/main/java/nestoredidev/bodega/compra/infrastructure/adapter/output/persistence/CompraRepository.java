package nestoredidev.bodega.compra.infrastructure.adapter.output.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CompraRepository extends JpaRepository<CompraEntity, Long>, JpaSpecificationExecutor<CompraEntity> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CompraEntity c where c.id = :id")
    Optional<CompraEntity> findByIdForUpdate(@Param("id") Long id);
}
