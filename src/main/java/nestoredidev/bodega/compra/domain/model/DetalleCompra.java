package nestoredidev.bodega.compra.domain.model;

import nestoredidev.bodega.compra.domain.exception.InvalidCompraException;
import nestoredidev.common.domain.Money;

import java.math.BigDecimal;

public class DetalleCompra {
    private final Long id;
    private final Long productoId;
    private final int cantidadCajas;
    private final int cantidadUnidades;
    private final BigDecimal costoUnitario;

    private DetalleCompra(Long id, Long productoId, int cantidadCajas, int cantidadUnidades, BigDecimal costoUnitario) {
        this.id = id;
        this.productoId = productoId;
        this.cantidadCajas = cantidadCajas;
        this.cantidadUnidades = cantidadUnidades;
        this.costoUnitario = costoUnitario;
    }

    /** RN-COM-02: cantidad_unidades = cajas × unidades_por_caja + sueltas. */
    public static DetalleCompra crear(Long productoId, int cajas, int sueltas, int unidadesPorCaja,
                                      BigDecimal costoUnitario) {
        if (productoId == null) {
            throw new InvalidCompraException("productoId es obligatorio");
        }
        if (cajas < 0 || sueltas < 0) {
            throw new InvalidCompraException("Las cantidades no pueden ser negativas");
        }
        if (costoUnitario == null || costoUnitario.signum() < 0) {
            throw new InvalidCompraException("El costo unitario no puede ser negativo");
        }
        long unidades = (long) cajas * unidadesPorCaja + sueltas;
        if (unidades <= 0) {
            throw new InvalidCompraException("La cantidad de unidades del producto " + productoId
                    + " debe ser mayor que cero");
        }
        if (unidades > 1_000_000) {
            throw new InvalidCompraException("La cantidad de unidades es demasiado grande");
        }
        return new DetalleCompra(null, productoId, cajas, (int) unidades, Money.of(costoUnitario));
    }

    public static DetalleCompra restaurar(Long id, Long productoId, int cantidadCajas, int cantidadUnidades,
                                          BigDecimal costoUnitario) {
        return new DetalleCompra(id, productoId, cantidadCajas, cantidadUnidades, costoUnitario);
    }

    public BigDecimal subtotal() {
        return Money.of(costoUnitario.multiply(BigDecimal.valueOf(cantidadUnidades)));
    }

    public Long getId() { return id; }
    public Long getProductoId() { return productoId; }
    public int getCantidadCajas() { return cantidadCajas; }
    public int getCantidadUnidades() { return cantidadUnidades; }
    public BigDecimal getCostoUnitario() { return costoUnitario; }
}
