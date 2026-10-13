package nestoredidev.bodega.producto.domain.model;

/** Kardex: todo lo que mueve el stock. signo = +1 suma, -1 resta. */
public enum TipoMovimientoInventario {
    ENTRADA_COMPRA(1),
    SALIDA_VENTA(-1),
    ANULACION_VENTA(1),
    ANULACION_COMPRA(-1),
    AJUSTE_POSITIVO(1),
    AJUSTE_NEGATIVO(-1),
    MERMA(-1),
    DEVOLUCION_ENVASE(1),
    SALIDA_ENVASE(-1);

    private final int signo;

    TipoMovimientoInventario(int signo) {
        this.signo = signo;
    }

    public int signo() {
        return signo;
    }

    /** Tipos que el ADMIN puede registrar a mano (los demás nacen de compras y ventas). */
    public boolean esManual() {
        return this == AJUSTE_POSITIVO || this == AJUSTE_NEGATIVO || this == MERMA
                || this == DEVOLUCION_ENVASE || this == SALIDA_ENVASE;
    }
}
