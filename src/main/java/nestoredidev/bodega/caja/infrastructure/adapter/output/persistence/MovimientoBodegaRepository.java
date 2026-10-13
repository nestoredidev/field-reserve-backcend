package nestoredidev.bodega.caja.infrastructure.adapter.output.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimientoBodegaRepository extends JpaRepository<MovimientoBodegaEntity, Long> {
    Page<MovimientoBodegaEntity> findByCajaId(Long cajaId, Pageable pageable);
}
