package nestoredidev.bodega.producto.infrastructure.adapter.output.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface MovimientoInventarioRepository
        extends JpaRepository<MovimientoInventarioEntity, Long>, JpaSpecificationExecutor<MovimientoInventarioEntity> {

    List<MovimientoInventarioEntity> findByVentaIdOrderByIdAsc(Long ventaId);
}
