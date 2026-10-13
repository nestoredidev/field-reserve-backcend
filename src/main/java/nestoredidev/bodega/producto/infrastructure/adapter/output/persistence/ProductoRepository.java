package nestoredidev.bodega.producto.infrastructure.adapter.output.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<ProductoEntity, Long>, JpaSpecificationExecutor<ProductoEntity> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProductoEntity p where p.id = :id")
    Optional<ProductoEntity> findByIdForUpdate(@Param("id") Long id);

    Optional<ProductoEntity> findByCodigoBarras(String codigoBarras);

    @Query("select count(p) > 0 from ProductoEntity p where p.codigoBarras = :codigo and p.id <> :excluirId")
    boolean existsByCodigoBarras(@Param("codigo") String codigo, @Param("excluirId") Long excluirId);

    boolean existsByEnvaseId(Long envaseId);

    @Query("select p from ProductoEntity p where p.activo = true and p.stock <= p.stockMinimo order by p.nombre")
    List<ProductoEntity> stockBajo();
}
