package nestoredidev.cajacancha.infrastructure.adapter.output.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimientoCajaRepository extends JpaRepository<MovimientoCajaEntity, Long> {
    Page<MovimientoCajaEntity> findByCajaId(Long cajaId, Pageable pageable);
}
