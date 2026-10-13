package nestoredidev.bodega.producto.domain.model;

import java.time.OffsetDateTime;

public class MovimientoInventario {
    private final Long id;
    private final Long productoId;
    private final TipoMovimientoInventario tipo;
    private final int cantidad;
    private final int stockAnterior;
    private final int stockResultante;
    private final Long compraId;
    private final Long ventaId;
    private final String motivo;
    private final Long registradoPor;
    private final OffsetDateTime fecha;

    public MovimientoInventario(Long id, Long productoId, TipoMovimientoInventario tipo, int cantidad,
                                int stockAnterior, int stockResultante, Long compraId, Long ventaId, String motivo,
                                Long registradoPor, OffsetDateTime fecha) {
        this.id = id;
        this.productoId = productoId;
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.stockAnterior = stockAnterior;
        this.stockResultante = stockResultante;
        this.compraId = compraId;
        this.ventaId = ventaId;
        this.motivo = motivo;
        this.registradoPor = registradoPor;
        this.fecha = fecha;
    }

    public Long getId() { return id; }
    public Long getProductoId() { return productoId; }
    public TipoMovimientoInventario getTipo() { return tipo; }
    public int getCantidad() { return cantidad; }
    public int getStockAnterior() { return stockAnterior; }
    public int getStockResultante() { return stockResultante; }
    public Long getCompraId() { return compraId; }
    public Long getVentaId() { return ventaId; }
    public String getMotivo() { return motivo; }
    public Long getRegistradoPor() { return registradoPor; }
    public OffsetDateTime getFecha() { return fecha; }
}
