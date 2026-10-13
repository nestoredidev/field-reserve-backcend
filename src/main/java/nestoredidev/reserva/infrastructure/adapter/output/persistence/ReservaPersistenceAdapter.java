package nestoredidev.reserva.infrastructure.adapter.output.persistence;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;
import nestoredidev.common.domain.MetodoPago;
import nestoredidev.common.domain.Money;
import nestoredidev.common.infrastructure.persistence.PageAdapter;
import nestoredidev.reserva.application.ports.input.dto.ReservaFiltro;
import nestoredidev.reserva.application.ports.output.ReservaPersistencePort;
import nestoredidev.reserva.domain.model.EstadoReserva;
import nestoredidev.reserva.domain.model.PagoReserva;
import nestoredidev.reserva.domain.model.Reserva;
import nestoredidev.reserva.domain.model.TipoPagoReserva;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.*;

@Component
@RequiredArgsConstructor
public class ReservaPersistenceAdapter implements ReservaPersistencePort {
    private final ReservaRepository reservaRepository;
    private final PagoReservaRepository pagoRepository;
    private final Clock clock;

    @Override
    public Reserva guardar(Reserva r) {
        ReservaEntity e = r.getId() == null
                ? new ReservaEntity()
                : reservaRepository.findById(r.getId()).orElseGet(ReservaEntity::new);
        e.setCanchaId(r.getCanchaId());
        e.setClienteId(r.getClienteId());
        e.setTipo(r.getTipo());
        e.setEstado(r.getEstado());
        e.setInicio(r.getInicio());
        e.setFin(r.getFin());
        e.setMontoTotal(r.getMontoTotal());
        e.setFechaLimiteAdelanto(r.getFechaLimiteAdelanto());
        e.setOrigen(r.getOrigen());
        e.setObservacion(r.getObservacion());
        e.setMotivoCancelacion(r.getMotivoCancelacion());
        e.setCreadoPor(r.getCreadoPor());
        return toDomain(reservaRepository.saveAndFlush(e));
    }

    @Override
    public Optional<Reserva> buscarPorId(Long id) {
        return reservaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Reserva> buscarPorIdParaActualizar(Long id) {
        return reservaRepository.findByIdForUpdate(id).map(this::toDomain);
    }

    @Override
    public PageResult<Reserva> listar(ReservaFiltro f, PageQuery query) {
        Specification<ReservaEntity> spec = (root, cq, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (f.canchaId() != null) p.add(cb.equal(root.get("canchaId"), f.canchaId()));
            if (f.clienteId() != null) p.add(cb.equal(root.get("clienteId"), f.clienteId()));
            if (f.estado() != null) p.add(cb.equal(root.get("estado"), f.estado()));
            if (f.tipo() != null) p.add(cb.equal(root.get("tipo"), f.tipo()));
            if (f.desde() != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("inicio"), f.desde().atStartOfDay(clock.getZone()).toOffsetDateTime()));
            }
            if (f.hasta() != null) {
                p.add(cb.lessThan(root.get("inicio"), f.hasta().plusDays(1).atStartOfDay(clock.getZone()).toOffsetDateTime()));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
        return PageAdapter.toResult(reservaRepository.findAll(spec, PageAdapter.toPageable(query)), this::toDomain);
    }

    @Override
    public List<Reserva> buscarCruces(Long canchaId, OffsetDateTime inicio, OffsetDateTime fin, Long excluirId) {
        return reservaRepository.buscarCruces(canchaId, inicio, fin, excluirId == null ? -1L : excluirId,
                EstadoReserva.CANCELADA).stream().map(this::toDomain).toList();
    }

    @Override
    public long contarPorClienteYEstado(Long clienteId, EstadoReserva estado) {
        return reservaRepository.countByClienteIdAndEstado(clienteId, estado);
    }

    @Override
    public List<Reserva> buscarPendientesConAdelantoVencido(OffsetDateTime ahora) {
        return reservaRepository.findByEstadoAndFechaLimiteAdelantoBefore(EstadoReserva.PENDIENTE, ahora)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public BigDecimal pagadoNeto(Long reservaId) {
        pagoRepository.flush();
        return Money.of(pagoRepository.pagadoNeto(reservaId, TipoPagoReserva.DEVOLUCION));
    }

    @Override
    public Map<Long, BigDecimal> pagadoNetoPorReservas(Collection<Long> reservaIds) {
        if (reservaIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, BigDecimal> result = new HashMap<>();
        for (Object[] row : pagoRepository.pagadoNetoPorReservas(reservaIds, TipoPagoReserva.DEVOLUCION)) {
            result.put((Long) row[0], Money.of((BigDecimal) row[1]));
        }
        return result;
    }

    @Override
    public PagoReserva guardarPago(PagoReserva p) {
        PagoReservaEntity e = new PagoReservaEntity();
        e.setReservaId(p.getReservaId());
        e.setCajaId(p.getCajaId());
        e.setTipo(p.getTipo());
        e.setMetodo(p.getMetodo());
        e.setMonto(p.getMonto());
        e.setNroOperacion(p.getNroOperacion());
        e.setVoucherUrl(p.getVoucherUrl());
        e.setRegistradoPor(p.getRegistradoPor());
        return toDomain(pagoRepository.saveAndFlush(e));
    }

    @Override
    public List<PagoReserva> listarPagos(Long reservaId) {
        return pagoRepository.findByReservaIdOrderByFechaAscIdAsc(reservaId).stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existePagoConOperacion(MetodoPago metodo, String nroOperacion) {
        return pagoRepository.existsByMetodoAndNroOperacion(metodo, nroOperacion);
    }

    private Reserva toDomain(ReservaEntity e) {
        return Reserva.restaurar(e.getId(), e.getCanchaId(), e.getClienteId(), e.getTipo(), e.getEstado(),
                e.getInicio(), e.getFin(), e.getMontoTotal(), e.getFechaLimiteAdelanto(), e.getOrigen(),
                e.getObservacion(), e.getMotivoCancelacion(), e.getCreadoPor(), e.getCreatedAt());
    }

    private PagoReserva toDomain(PagoReservaEntity e) {
        return PagoReserva.restaurar(e.getId(), e.getReservaId(), e.getCajaId(), e.getTipo(), e.getMetodo(),
                e.getMonto(), e.getNroOperacion(), e.getVoucherUrl(), e.getRegistradoPor(), e.getFecha());
    }
}
