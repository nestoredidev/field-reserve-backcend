package nestoredidev.bodega.producto.domain.model;

import nestoredidev.bodega.producto.domain.exception.InvalidProductoException;
import nestoredidev.bodega.producto.domain.exception.StockInsuficienteException;
import nestoredidev.common.domain.Money;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class Producto {
    private final Long id;
    private final String codigoBarras;
    private final String nombre;
    private final Long categoriaId;
    private final TipoProducto tipo;
    private BigDecimal precioCompra;
    private final BigDecimal precioVenta;
    private int stock;
    private final int stockMinimo;
    private final int unidadesPorCaja;
    private final Long envaseId;
    private final boolean activo;
    private final OffsetDateTime createdAt;

    private Producto(Long id, String codigoBarras, String nombre, Long categoriaId, TipoProducto tipo,
                     BigDecimal precioCompra, BigDecimal precioVenta, int stock, int stockMinimo,
                     int unidadesPorCaja, Long envaseId, boolean activo, OffsetDateTime createdAt) {
        this.id = id;
        this.codigoBarras = codigoBarras;
        this.nombre = nombre;
        this.categoriaId = categoriaId;
        this.tipo = tipo;
        this.precioCompra = precioCompra;
        this.precioVenta = precioVenta;
        this.stock = stock;
        this.stockMinimo = stockMinimo;
        this.unidadesPorCaja = unidadesPorCaja;
        this.envaseId = envaseId;
        this.activo = activo;
        this.createdAt = createdAt;
    }

    /** RN-PRO-02: el stock nace en 0; el inventario inicial entra como AJUSTE_POSITIVO (RN-PRO-03). */
    public static Producto crear(String codigoBarras, String nombre, Long categoriaId, TipoProducto tipo,
                                 BigDecimal precioCompra, BigDecimal precioVenta, int stockMinimo,
                                 int unidadesPorCaja, Long envaseId) {
        validar(nombre, categoriaId, tipo, precioCompra, precioVenta, stockMinimo, unidadesPorCaja, envaseId);
        return new Producto(null, texto(codigoBarras), nombre.trim(), categoriaId, tipo, Money.of(precioCompra),
                Money.of(precioVenta), 0, stockMinimo, unidadesPorCaja, envaseId, true, null);
    }

    public static Producto restaurar(Long id, String codigoBarras, String nombre, Long categoriaId,
                                     TipoProducto tipo, BigDecimal precioCompra, BigDecimal precioVenta, int stock,
                                     int stockMinimo, int unidadesPorCaja, Long envaseId, boolean activo,
                                     OffsetDateTime createdAt) {
        return new Producto(id, codigoBarras, nombre, categoriaId, tipo, precioCompra, precioVenta, stock,
                stockMinimo, unidadesPorCaja, envaseId, activo, createdAt);
    }

    /** El stock nunca se edita aquí (RN-PRO-02). */
    public Producto actualizar(String codigoBarras, String nombre, Long categoriaId, TipoProducto tipo,
                               BigDecimal precioCompra, BigDecimal precioVenta, int stockMinimo,
                               int unidadesPorCaja, Long envaseId) {
        validar(nombre, categoriaId, tipo, precioCompra, precioVenta, stockMinimo, unidadesPorCaja, envaseId);
        if (envaseId != null && envaseId.equals(id)) {
            throw new InvalidProductoException("Un producto no puede ser su propio envase");
        }
        return new Producto(id, texto(codigoBarras), nombre.trim(), categoriaId, tipo, Money.of(precioCompra),
                Money.of(precioVenta), stock, stockMinimo, unidadesPorCaja, envaseId, activo, createdAt);
    }

    public Producto conActivo(boolean nuevoActivo) {
        return new Producto(id, codigoBarras, nombre, categoriaId, tipo, precioCompra, precioVenta, stock,
                stockMinimo, unidadesPorCaja, envaseId, nuevoActivo, createdAt);
    }

    /** RN-PRO-06: el precio de compra pasa a ser el costo unitario de la última compra. */
    public void actualizarPrecioCompra(BigDecimal costoUnitario) {
        if (costoUnitario == null || costoUnitario.signum() < 0) {
            throw new InvalidProductoException("El costo unitario no puede ser negativo");
        }
        this.precioCompra = Money.of(costoUnitario);
    }

    /** Aplica un movimiento del kardex y devuelve el stock anterior. */
    public int aplicarMovimiento(TipoMovimientoInventario tipoMovimiento, int cantidad) {
        if (cantidad <= 0) {
            throw new InvalidProductoException("La cantidad debe ser un entero mayor que cero");
        }
        int anterior = stock;
        int nuevo = anterior + tipoMovimiento.signo() * cantidad;
        if (nuevo < 0) {
            throw new StockInsuficienteException(nombre, anterior, cantidad);
        }
        this.stock = nuevo;
        return anterior;
    }

    public boolean stockBajo() {
        return activo && stock <= stockMinimo;
    }

    /** RN-PRO-04: advierte, no bloquea. */
    public boolean vendeBajoCosto() {
        return precioVenta.compareTo(precioCompra) < 0;
    }

    public boolean esRetornable() {
        return envaseId != null;
    }

    private static void validar(String nombre, Long categoriaId, TipoProducto tipo, BigDecimal precioCompra,
                                BigDecimal precioVenta, int stockMinimo, int unidadesPorCaja, Long envaseId) {
        if (nombre == null || nombre.isBlank()) {
            throw new InvalidProductoException("RN-PRO-01: el nombre es obligatorio");
        }
        if (nombre.trim().length() > 120) {
            throw new InvalidProductoException("nombre no debe exceder 120 caracteres");
        }
        if (categoriaId == null) {
            throw new InvalidProductoException("RN-PRO-01: la categoría es obligatoria");
        }
        if (tipo == null) {
            throw new InvalidProductoException("tipo es obligatorio");
        }
        if (precioCompra == null || precioCompra.signum() < 0 || precioVenta == null || precioVenta.signum() < 0) {
            throw new InvalidProductoException("Los precios no pueden ser negativos");
        }
        if (stockMinimo < 0) {
            throw new InvalidProductoException("stockMinimo no puede ser negativo");
        }
        if (unidadesPorCaja < 1) {
            throw new InvalidProductoException("RN-PRO-07: unidadesPorCaja debe ser al menos 1");
        }
        if (tipo == TipoProducto.ENVASE && envaseId != null) {
            throw new InvalidProductoException("RN-PRO-08: un ENVASE no puede tener envase asociado");
        }
    }

    private static String texto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    public Long getId() { return id; }
    public String getCodigoBarras() { return codigoBarras; }
    public String getNombre() { return nombre; }
    public Long getCategoriaId() { return categoriaId; }
    public TipoProducto getTipo() { return tipo; }
    public BigDecimal getPrecioCompra() { return precioCompra; }
    public BigDecimal getPrecioVenta() { return precioVenta; }
    public int getStock() { return stock; }
    public int getStockMinimo() { return stockMinimo; }
    public int getUnidadesPorCaja() { return unidadesPorCaja; }
    public Long getEnvaseId() { return envaseId; }
    public boolean isActivo() { return activo; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
