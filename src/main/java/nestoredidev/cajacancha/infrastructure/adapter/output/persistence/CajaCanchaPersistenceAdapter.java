package nestoredidev.cajacancha.infrastructure.adapter.output.persistence;

import lombok.RequiredArgsConstructor;
import nestoredidev.cajacancha.application.ports.output.CajaCanchaPersistencePort;
import nestoredidev.cajacancha.domain.model.CajaCancha;
import nestoredidev.cajacancha.domain.model.CajaResumen;
import nestoredidev.cajacancha.domain.model.MovimientoCaja;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;
import nestoredidev.common.domain.EstadoCaja;
import nestoredidev.common.domain.Money;
import nestoredidev.common.infrastructure.persistence.PageAdapter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CajaCanchaPersistenceAdapter implements CajaCanchaPersistencePort {
    private final CajaCanchaRepository cajaRepository;
    private final MovimientoCajaRepository movimientoRepository;

    @Override
    public CajaCancha guardar(CajaCancha c) {
        CajaCanchaEntity e = c.getId() == null
                ? new CajaCanchaEntity()
                : cajaRepository.findById(c.getId()).orElseGet(CajaCanchaEntity::new);
        e.setFecha(c.getFecha());
        e.setMontoApertura(c.getMontoApertura());
        e.setMontoCierreContado(c.getMontoCierreContado());
        e.setEstado(c.getEstado());
        e.setObservacion(c.getObservacion());
        e.setAbiertaPor(c.getAbiertaPor());
        e.setCerradaPor(c.getCerradaPor());
        e.setAbiertaAt(c.getAbiertaAt());
        e.setCerradaAt(c.getCerradaAt());
        return toDomain(cajaRepository.saveAndFlush(e));
    }

    @Override
    public Optional<CajaCancha> buscarPorId(Long id) {
        return cajaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<CajaCancha> buscarAbierta() {
        return cajaRepository.findFirstByEstado(EstadoCaja.ABIERTA).map(this::toDomain);
    }

    @Override
    public PageResult<CajaCancha> listar(PageQuery query) {
        return PageAdapter.toResult(cajaRepository.findAll(PageAdapter.toPageable(query)), this::toDomain);
    }

    @Override
    public CajaResumen resumen(Long cajaId) {
        cajaRepository.flush();
        return cajaRepository.resumen(cajaId).map(r -> new CajaResumen(nz(r.getCobrosEfectivo()),
                nz(r.getCobrosDigitales()), nz(r.getOtrosIngresosEfectivo()), nz(r.getEgresosEfectivo()),
                nz(r.getEfectivoEsperado()), r.getDiferencia() == null ? null : Money.of(r.getDiferencia())))
                .orElseThrow(() -> new IllegalStateException("Sin resumen para la caja " + cajaId));
    }

    @Override
    public MovimientoCaja guardarMovimiento(MovimientoCaja m) {
        MovimientoCajaEntity e = new MovimientoCajaEntity();
        e.setCajaId(m.getCajaId());
        e.setTipo(m.getTipo());
        e.setMetodo(m.getMetodo());
        e.setConcepto(m.getConcepto());
        e.setMonto(m.getMonto());
        e.setRegistradoPor(m.getRegistradoPor());
        return toDomain(movimientoRepository.saveAndFlush(e));
    }

    @Override
    public PageResult<MovimientoCaja> listarMovimientos(Long cajaId, PageQuery query) {
        return PageAdapter.toResult(movimientoRepository.findByCajaId(cajaId, PageAdapter.toPageable(query)),
                this::toDomain);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? Money.ZERO : Money.of(v);
    }

    private CajaCancha toDomain(CajaCanchaEntity e) {
        return CajaCancha.restaurar(e.getId(), e.getFecha(), e.getMontoApertura(), e.getMontoCierreContado(),
                e.getEstado(), e.getObservacion(), e.getAbiertaPor(), e.getCerradaPor(), e.getAbiertaAt(),
                e.getCerradaAt());
    }

    private MovimientoCaja toDomain(MovimientoCajaEntity e) {
        return MovimientoCaja.restaurar(e.getId(), e.getCajaId(), e.getTipo(), e.getMetodo(), e.getConcepto(),
                e.getMonto(), e.getRegistradoPor(), e.getFecha());
    }
}
