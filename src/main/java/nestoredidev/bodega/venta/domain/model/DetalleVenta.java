package nestoredidev.bodega.venta.domain.model;

import nestoredidev.bodega.venta.domain.exception.InvalidVentaException;
import nestoredidev.common.domain.Money;

import java.math.BigDecimal;

public class DetalleVenta {
    private final Long id;
    private final Long productoId;
    private final int cantidad;
    private final BigDecimal precioUnitario;
    private final BigDecimal costoUnitario;

    private DetalleVenta(Long id, Long productoId, int cantidad, BigDecimal precioUnitario, BigDecimal costoUnitario) {
        this.id = id;
        this.productoId = productoId;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.costoUnitario = costoUnitario;
    }

    /** RN-DVE-01 y RN-DVE-03: precio y costo salen del producto, nunca del cliente. */
    public static DetalleVenta crear(Long productoId, int cantidad, BigDecimal precioVenta, BigDecimal precioCompra) {
        if (productoId == null) {
            throw new InvalidVentaException("productoId es obligatorio");
        }
        if (cantidad <= 0) {
            throw new InvalidVentaException("RN-DVE-01: la cantidad debe ser un entero mayor que cero");
        }
        return new DetalleVenta(null, productoId, cantidad, Money.of(precioVenta), Money.of(precioCompra));
    }

    public static DetalleVenta restaurar(Long id, Long productoId, int cantidad, BigDecimal precioUnitario,
                                         BigDecimal costoUnitario) {
        return new DetalleVenta(id, productoId, cantidad, precioUnitario, costoUnitario);
    }

    public BigDecimal subtotal() {
        return Money.of(precioUnitario.multiply(BigDecimal.valueOf(cantidad)));
    }

    public Long getId() { return id; }
    public Long getProductoId() { return productoId; }
    public int getCantidad() { return cantidad; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public BigDecimal getCostoUnitario() { return costoUnitario; }
}
