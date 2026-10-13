package nestoredidev.bodega.venta.application.ports.output;

import nestoredidev.bodega.venta.domain.model.EstadoVenta;
import nestoredidev.bodega.venta.domain.model.Venta;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;
import nestoredidev.common.domain.MetodoPago;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface VentaPersistencePort {

    /** Inserta la venta con detalles y pagos, o actualiza el estado de una existente. */
    Venta guardar(Venta venta);

    Optional<Venta> buscarPorId(Long id);

    Optional<Venta> buscarPorIdParaActualizar(Long id);

    PageResult<Venta> listar(Long cajaId, EstadoVenta estado, OffsetDateTime desde, OffsetDateTime hasta,
                             PageQuery query);

    boolean existePagoConOperacion(MetodoPago metodo, String nroOperacion);
}
