package nestoredidev.reserva.application.ports.output;

import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;
import nestoredidev.common.domain.MetodoPago;
import nestoredidev.reserva.application.ports.input.dto.ReservaFiltro;
import nestoredidev.reserva.domain.model.EstadoReserva;
import nestoredidev.reserva.domain.model.PagoReserva;
import nestoredidev.reserva.domain.model.Reserva;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ReservaPersistencePort {

    Reserva guardar(Reserva reserva);

    Optional<Reserva> buscarPorId(Long id);

    /** Bloquea la fila para serializar pagos y cambios de estado concurrentes. */
    Optional<Reserva> buscarPorIdParaActualizar(Long id);

    PageResult<Reserva> listar(ReservaFiltro filtro, PageQuery query);

    /** Ocupaciones no canceladas que se cruzan con [inicio, fin). */
    List<Reserva> buscarCruces(Long canchaId, OffsetDateTime inicio, OffsetDateTime fin, Long excluirId);

    long contarPorClienteYEstado(Long clienteId, EstadoReserva estado);

    List<Reserva> buscarPendientesConAdelantoVencido(OffsetDateTime ahora);

    BigDecimal pagadoNeto(Long reservaId);

    Map<Long, BigDecimal> pagadoNetoPorReservas(Collection<Long> reservaIds);

    PagoReserva guardarPago(PagoReserva pago);

    List<PagoReserva> listarPagos(Long reservaId);

    boolean existePagoConOperacion(MetodoPago metodo, String nroOperacion);
}
