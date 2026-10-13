package nestoredidev.bodega.producto.application.ports.input.dto;

import nestoredidev.bodega.producto.domain.model.Producto;
import nestoredidev.bodega.producto.domain.model.TipoProducto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record ProductoDto(Long id, String codigoBarras, String nombre, Long categoriaId, TipoProducto tipo,
                          BigDecimal precioCompra, BigDecimal precioVenta, int stock, int stockMinimo,
                          int unidadesPorCaja, Long envaseId, boolean activo, boolean stockBajo,
                          List<String> advertencias, OffsetDateTime createdAt) {

    public static ProductoDto fromDomain(Producto p) {
        List<String> advertencias = p.vendeBajoCosto()
                ? List.of("RN-PRO-04: el precio de venta es menor que el precio de compra")
                : null;
        return new ProductoDto(p.getId(), p.getCodigoBarras(), p.getNombre(), p.getCategoriaId(), p.getTipo(),
                p.getPrecioCompra(), p.getPrecioVenta(), p.getStock(), p.getStockMinimo(), p.getUnidadesPorCaja(),
                p.getEnvaseId(), p.isActivo(), p.stockBajo(), advertencias, p.getCreatedAt());
    }
}
