package nestoredidev.bodega.producto.infrastructure.adapter.output.persistence;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import nestoredidev.bodega.producto.application.ports.output.ProductoPersistencePort;
import nestoredidev.bodega.producto.domain.model.MovimientoInventario;
import nestoredidev.bodega.producto.domain.model.Producto;
import nestoredidev.bodega.producto.domain.model.TipoMovimientoInventario;
import nestoredidev.bodega.producto.domain.model.TipoProducto;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;
import nestoredidev.common.infrastructure.persistence.PageAdapter;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProductoPersistenceAdapter implements ProductoPersistencePort {
    private final ProductoRepository repository;
    private final MovimientoInventarioRepository movimientoRepository;

    @Override
    public Producto guardar(Producto p) {
        ProductoEntity e;
        if (p.getId() == null) {
            e = new ProductoEntity();
            e.setStock(p.getStock());
        } else {
            e = repository.findById(p.getId()).orElseThrow(); // el stock existente no se toca
        }
        e.setCodigoBarras(p.getCodigoBarras());
        e.setNombre(p.getNombre());
        e.setCategoriaId(p.getCategoriaId());
        e.setTipo(p.getTipo());
        e.setPrecioCompra(p.getPrecioCompra());
        e.setPrecioVenta(p.getPrecioVenta());
        e.setStockMinimo(p.getStockMinimo());
        e.setUnidadesPorCaja(p.getUnidadesPorCaja());
        e.setEnvaseId(p.getEnvaseId());
        e.setActivo(p.isActivo());
        return toDomain(repository.saveAndFlush(e));
    }

    @Override
    public Producto guardarStock(Producto p) {
        ProductoEntity e = repository.findById(p.getId()).orElseThrow();
        e.setStock(p.getStock());
        return toDomain(repository.saveAndFlush(e));
    }

    @Override
    public Optional<Producto> buscarPorId(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Producto> buscarPorIdParaActualizar(Long id) {
        return repository.findByIdForUpdate(id).map(this::toDomain);
    }

    @Override
    public Optional<Producto> buscarPorCodigoBarras(String codigoBarras) {
        return repository.findByCodigoBarras(codigoBarras).map(this::toDomain);
    }

    @Override
    public boolean existePorCodigoBarras(String codigoBarras, Long excluirId) {
        return repository.existsByCodigoBarras(codigoBarras, excluirId == null ? -1L : excluirId);
    }

    @Override
    public boolean esEnvaseDeOtroProducto(Long id) {
        return repository.existsByEnvaseId(id);
    }

    @Override
    public PageResult<Producto> listar(String q, Long categoriaId, TipoProducto tipo, Boolean activo, PageQuery query) {
        Specification<ProductoEntity> spec = (root, cq, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            if (q != null) {
                String like = "%" + q.toLowerCase() + "%";
                preds.add(cb.or(cb.like(cb.lower(root.get("nombre")), like),
                        cb.like(cb.lower(root.get("codigoBarras")), like)));
            }
            if (categoriaId != null) preds.add(cb.equal(root.get("categoriaId"), categoriaId));
            if (tipo != null) preds.add(cb.equal(root.get("tipo"), tipo));
            if (activo != null) preds.add(cb.equal(root.get("activo"), activo));
            return cb.and(preds.toArray(new Predicate[0]));
        };
        return PageAdapter.toResult(repository.findAll(spec, PageAdapter.toPageable(query)), this::toDomain);
    }

    @Override
    public List<Producto> stockBajo() {
        return repository.stockBajo().stream().map(this::toDomain).toList();
    }

    @Override
    public MovimientoInventario guardarMovimiento(MovimientoInventario m) {
        MovimientoInventarioEntity e = new MovimientoInventarioEntity();
        e.setProductoId(m.getProductoId());
        e.setTipo(m.getTipo());
        e.setCantidad(m.getCantidad());
        e.setStockAnterior(m.getStockAnterior());
        e.setStockResultante(m.getStockResultante());
        e.setCompraId(m.getCompraId());
        e.setVentaId(m.getVentaId());
        e.setMotivo(m.getMotivo());
        e.setRegistradoPor(m.getRegistradoPor());
        return toDomain(movimientoRepository.saveAndFlush(e));
    }

    @Override
    public PageResult<MovimientoInventario> listarMovimientos(Long productoId, TipoMovimientoInventario tipo,
                                                              PageQuery query) {
        Specification<MovimientoInventarioEntity> spec = (root, cq, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            if (productoId != null) preds.add(cb.equal(root.get("productoId"), productoId));
            if (tipo != null) preds.add(cb.equal(root.get("tipo"), tipo));
            return cb.and(preds.toArray(new Predicate[0]));
        };
        return PageAdapter.toResult(movimientoRepository.findAll(spec, PageAdapter.toPageable(query)), this::toDomain);
    }

    @Override
    public List<MovimientoInventario> movimientosDeVenta(Long ventaId) {
        return movimientoRepository.findByVentaIdOrderByIdAsc(ventaId).stream().map(this::toDomain).toList();
    }

    private Producto toDomain(ProductoEntity e) {
        return Producto.restaurar(e.getId(), e.getCodigoBarras(), e.getNombre(), e.getCategoriaId(), e.getTipo(),
                e.getPrecioCompra(), e.getPrecioVenta(), e.getStock(), e.getStockMinimo(), e.getUnidadesPorCaja(),
                e.getEnvaseId(), Boolean.TRUE.equals(e.getActivo()), e.getCreatedAt());
    }

    private MovimientoInventario toDomain(MovimientoInventarioEntity e) {
        return new MovimientoInventario(e.getId(), e.getProductoId(), e.getTipo(), e.getCantidad(),
                e.getStockAnterior(), e.getStockResultante(), e.getCompraId(), e.getVentaId(), e.getMotivo(),
                e.getRegistradoPor(), e.getFecha());
    }
}
