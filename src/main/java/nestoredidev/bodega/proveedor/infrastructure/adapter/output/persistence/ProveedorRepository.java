package nestoredidev.bodega.proveedor.infrastructure.adapter.output.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProveedorRepository extends JpaRepository<ProveedorEntity, Long>, JpaSpecificationExecutor<ProveedorEntity> {

    @Query("select count(p) > 0 from ProveedorEntity p where p.ruc = :ruc and p.id <> :excluirId")
    boolean existsByRuc(@Param("ruc") String ruc, @Param("excluirId") Long excluirId);
}
