package nestoredidev.bodega.caja.infrastructure.adapter.output.persistence;

import lombok.RequiredArgsConstructor;
import nestoredidev.bodega.caja.application.ports.output.CajaBodegaPersistencePort;
import nestoredidev.bodega.caja.domain.model.CajaBodega;
import nestoredidev.bodega.caja.domain.model.CajaBodegaResumen;
import nestoredidev.bodega.caja.domain.model.MovimientoBodega;
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
public class CajaBodegaPersistenceAdapter implements CajaBodegaPersistencePort {
    private final CajaBodegaRepository cajaRepository;
    private final MovimientoBodegaRepository movimientoRepository;

    @Override
    public CajaBodega guardar(CajaBodega c) {
        CajaBodegaEntity e = c.getId() == null
                ? new CajaBodegaEntity()
                : cajaRepository.findById(c.getId()).orElseGet(CajaBodegaEntity::new);
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
    public Optional<CajaBodega> buscarPorId(Long id) {
        return cajaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<CajaBodega> buscarAbierta() {
        return cajaRepository.findFirstByEstado(EstadoCaja.ABIERTA).map(this::toDomain);
    }

    @Override
    public PageResult<CajaBodega> listar(PageQuery query) {
        return PageAdapter.toResult(cajaRepository.findAll(PageAdapter.toPageable(query)), this::toDomain);
    }

    @Override
    public CajaBodegaResumen resumen(Long cajaId) {
        cajaRepository.flush();
        return cajaRepository.resumen(cajaId).map(r -> new CajaBodegaResumen(nz(r.getCobrosEfectivo()),
                nz(r.getCobrosDigitales()), nz(r.getOtrosIngresosEfectivo()), nz(r.getEgresosEfectivo()),
                nz(r.getEfectivoEsperado()), r.getDiferencia() == null ? null : Money.of(r.getDiferencia())))
                .orElseThrow(() -> new IllegalStateException("Sin resumen para la caja " + cajaId));
    }

    @Override
    public MovimientoBodega guardarMovimiento(MovimientoBodega m) {
        MovimientoBodegaEntity e = new MovimientoBodegaEntity();
        e.setCajaId(m.getCajaId());
        e.setTipo(m.getTipo());
        e.setMetodo(m.getMetodo());
        e.setConcepto(m.getConcepto());
        e.setMonto(m.getMonto());
        e.setRegistradoPor(m.getRegistradoPor());
        return toDomain(movimientoRepository.saveAndFlush(e));
    }

    @Override
    public PageResult<MovimientoBodega> listarMovimientos(Long cajaId, PageQuery query) {
        return PageAdapter.toResult(movimientoRepository.findByCajaId(cajaId, PageAdapter.toPageable(query)),
                this::toDomain);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? Money.ZERO : Money.of(v);
    }

    private CajaBodega toDomain(CajaBodegaEntity e) {
        return CajaBodega.restaurar(e.getId(), e.getFecha(), e.getMontoApertura(), e.getMontoCierreContado(),
                e.getEstado(), e.getObservacion(), e.getAbiertaPor(), e.getCerradaPor(), e.getAbiertaAt(),
                e.getCerradaAt());
    }

    private MovimientoBodega toDomain(MovimientoBodegaEntity e) {
        return MovimientoBodega.restaurar(e.getId(), e.getCajaId(), e.getTipo(), e.getMetodo(), e.getConcepto(),
                e.getMonto(), e.getRegistradoPor(), e.getFecha());
    }
}
