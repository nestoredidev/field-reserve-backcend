package nestoredidev.common.domain;

public enum MetodoPago {
    EFECTIVO, YAPE, PLIN, TRANSFERENCIA;

    public boolean requiereNroOperacion() {
        return this != EFECTIVO;
    }
}
