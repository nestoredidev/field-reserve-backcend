package nestoredidev.bodega.venta.infrastructure.adapter.output.persistence;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import nestoredidev.bodega.venta.application.ports.output.VentaPersistencePort;
import nestoredidev.bodega.venta.domain.model.*;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;
import nestoredidev.common.domain.MetodoPago;
import nestoredidev.common.infrastructure.persistence.PageAdapter;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class VentaPersistenceAdapter implements VentaPersistencePort {
    private final VentaRepository repository;
    private final DetalleVentaRepository detalleRepository;
    private final PagoVentaRepository pagoRepository;

    @Override
    public Venta guardar(Venta v) {
        if (v.getId() != null) { // las ventas no se editan: solo cambia el estado (RN-G04)
            VentaEntity e = repository.findById(v.getId()).orElseThrow();
            e.setEstado(v.getEstado());
            e.setMotivoAnulacion(v.getMotivoAnulacion());
            return toDomain(repository.saveAndFlush(e));
        }
        VentaEntity e = new VentaEntity();
        e.setCajaId(v.getCajaId());
        e.setTotal(v.getTotal());
        e.setEstado(v.getEstado());
        e.setRegistradoPor(v.getRegistradoPor());
        VentaEntity saved = repository.saveAndFlush(e);
        for (DetalleVenta d : v.getDetalles()) {
            DetalleVentaEntity de = new DetalleVentaEntity();
            de.setVentaId(saved.getId());
            de.setProductoId(d.getProductoId());
            de.setCantidad(d.getCantidad());
            de.setPrecioUnitario(d.getPrecioUnitario());
            de.setCostoUnitario(d.getCostoUnitario());
            detalleRepository.save(de);
        }
        for (PagoVenta p : v.getPagos()) {
            PagoVentaEntity pe = new PagoVentaEntity();
            pe.setVentaId(saved.getId());
            pe.setMetodo(p.getMetodo());
            pe.setMonto(p.getMonto());
            pe.setNroOperacion(p.getNroOperacion());
            pagoRepository.save(pe);
        }
        detalleRepository.flush();
        pagoRepository.flush();
        return toDomain(saved);
    }

    @Override
    public Optional<Venta> buscarPorId(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Venta> buscarPorIdParaActualizar(Long id) {
        return repository.findByIdForUpdate(id).map(this::toDomain);
    }

    @Override
    public PageResult<Venta> listar(Long cajaId, EstadoVenta estado, OffsetDateTime desde, OffsetDateTime hasta,
                                    PageQuery query) {
        Specification<VentaEntity> spec = (root, cq, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (cajaId != null) p.add(cb.equal(root.get("cajaId"), cajaId));
            if (estado != null) p.add(cb.equal(root.get("estado"), estado));
            if (desde != null) p.add(cb.greaterThanOrEqualTo(root.get("fecha"), desde));
            if (hasta != null) p.add(cb.lessThan(root.get("fecha"), hasta));
            return cb.and(p.toArray(new Predicate[0]));
        };
        return PageAdapter.toResult(repository.findAll(spec, PageAdapter.toPageable(query)), this::toDomain);
    }

    @Override
    public boolean existePagoConOperacion(MetodoPago metodo, String nroOperacion) {
        return pagoRepository.existsByMetodoAndNroOperacion(metodo, nroOperacion);
    }

    private Venta toDomain(VentaEntity e) {
        List<DetalleVenta> detalles = detalleRepository.findByVentaIdOrderByIdAsc(e.getId()).stream()
                .map(d -> DetalleVenta.restaurar(d.getId(), d.getProductoId(), d.getCantidad(), d.getPrecioUnitario(),
                        d.getCostoUnitario())).toList();
        List<PagoVenta> pagos = pagoRepository.findByVentaIdOrderByIdAsc(e.getId()).stream()
                .map(p -> PagoVenta.restaurar(p.getId(), p.getMetodo(), p.getMonto(), p.getNroOperacion(),
                        p.getFecha())).toList();
        return Venta.restaurar(e.getId(), e.getCajaId(), e.getFecha(), e.getTotal(), e.getEstado(),
                e.getMotivoAnulacion(), e.getRegistradoPor(), detalles, pagos);
    }
}
