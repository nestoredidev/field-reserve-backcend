package nestoredidev.reserva.infrastructure.adapter.output.persistence;

import nestoredidev.common.domain.MetodoPago;
import nestoredidev.reserva.domain.model.TipoPagoReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

public interface PagoReservaRepository extends JpaRepository<PagoReservaEntity, Long> {

    List<PagoReservaEntity> findByReservaIdOrderByFechaAscIdAsc(Long reservaId);

    boolean existsByMetodoAndNroOperacion(MetodoPago metodo, String nroOperacion);

    @Query("select coalesce(sum(case when p.tipo = :devolucion then -p.monto else p.monto end), 0) "
            + "from PagoReservaEntity p where p.reservaId = :reservaId")
    BigDecimal pagadoNeto(@Param("reservaId") Long reservaId, @Param("devolucion") TipoPagoReserva devolucion);

    @Query("select p.reservaId, sum(case when p.tipo = :devolucion then -p.monto else p.monto end) "
            + "from PagoReservaEntity p where p.reservaId in :ids group by p.reservaId")
    List<Object[]> pagadoNetoPorReservas(@Param("ids") Collection<Long> ids,
                                         @Param("devolucion") TipoPagoReserva devolucion);
}
