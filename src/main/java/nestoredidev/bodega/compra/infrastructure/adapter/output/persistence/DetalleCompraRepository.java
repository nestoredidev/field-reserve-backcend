package nestoredidev.bodega.compra.infrastructure.adapter.output.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DetalleCompraRepository extends JpaRepository<DetalleCompraEntity, Long> {
    List<DetalleCompraEntity> findByCompraIdOrderByIdAsc(Long compraId);
}
