package nestoredidev.bodega.producto.domain.exception;

import nestoredidev.common.exception.DomainException;

public class StockInsuficienteException extends DomainException {
    public StockInsuficienteException(String producto, int stock, int solicitado) {
        super("RN-PRO-02: stock insuficiente de '" + producto + "' (stock " + stock + ", se piden " + solicitado + ")");
    }
}
