package nestoredidev.bodega.compra.infrastructure.adapter.output.persistence;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import nestoredidev.bodega.compra.application.ports.output.CompraPersistencePort;
import nestoredidev.bodega.compra.domain.model.Compra;
import nestoredidev.bodega.compra.domain.model.DetalleCompra;
import nestoredidev.bodega.compra.domain.model.EstadoCompra;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;
import nestoredidev.common.infrastructure.persistence.PageAdapter;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CompraPersistenceAdapter implements CompraPersistencePort {
    private final CompraRepository repository;
    private final DetalleCompraRepository detalleRepository;

    @Override
    public Compra guardar(Compra c) {
        if (c.getId() != null) { // solo cambia el estado (RN-G04: las compras no se editan)
            CompraEntity e = repository.findById(c.getId()).orElseThrow();
            e.setEstado(c.getEstado());
            return toDomain(repository.saveAndFlush(e));
        }
        CompraEntity e = new CompraEntity();
        e.setProveedorId(c.getProveedorId());
        e.setNroComprobante(c.getNroComprobante());
        e.setTotal(c.getTotal());
        e.setEstado(c.getEstado());
        e.setRegistradoPor(c.getRegistradoPor());
        CompraEntity saved = repository.saveAndFlush(e);
        for (DetalleCompra d : c.getDetalles()) {
            DetalleCompraEntity de = new DetalleCompraEntity();
            de.setCompraId(saved.getId());
            de.setProductoId(d.getProductoId());
            de.setCantidadCajas(d.getCantidadCajas());
            de.setCantidadUnidades(d.getCantidadUnidades());
            de.setCostoUnitario(d.getCostoUnitario());
            detalleRepository.save(de);
        }
        detalleRepository.flush();
        return toDomain(saved);
    }

    @Override
    public Optional<Compra> buscarPorId(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Compra> buscarPorIdParaActualizar(Long id) {
        return repository.findByIdForUpdate(id).map(this::toDomain);
    }

    @Override
    public PageResult<Compra> listar(Long proveedorId, EstadoCompra estado, OffsetDateTime desde,
                                     OffsetDateTime hasta, PageQuery query) {
        Specification<CompraEntity> spec = (root, cq, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (proveedorId != null) p.add(cb.equal(root.get("proveedorId"), proveedorId));
            if (estado != null) p.add(cb.equal(root.get("estado"), estado));
            if (desde != null) p.add(cb.greaterThanOrEqualTo(root.get("fecha"), desde));
            if (hasta != null) p.add(cb.lessThan(root.get("fecha"), hasta));
            return cb.and(p.toArray(new Predicate[0]));
        };
        return PageAdapter.toResult(repository.findAll(spec, PageAdapter.toPageable(query)), this::toDomain);
    }

    private Compra toDomain(CompraEntity e) {
        List<DetalleCompra> detalles = detalleRepository.findByCompraIdOrderByIdAsc(e.getId()).stream()
                .map(d -> DetalleCompra.restaurar(d.getId(), d.getProductoId(), d.getCantidadCajas(),
                        d.getCantidadUnidades(), d.getCostoUnitario()))
                .toList();
        return Compra.restaurar(e.getId(), e.getProveedorId(), e.getNroComprobante(), e.getFecha(), e.getTotal(),
                e.getEstado(), e.getRegistradoPor(), detalles);
    }
}
