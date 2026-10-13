package nestoredidev.bodega.venta.infrastructure.adapter.output.persistence;

import nestoredidev.common.domain.MetodoPago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PagoVentaRepository extends JpaRepository<PagoVentaEntity, Long> {
    List<PagoVentaEntity> findByVentaIdOrderByIdAsc(Long ventaId);

    boolean existsByMetodoAndNroOperacion(MetodoPago metodo, String nroOperacion);
}
