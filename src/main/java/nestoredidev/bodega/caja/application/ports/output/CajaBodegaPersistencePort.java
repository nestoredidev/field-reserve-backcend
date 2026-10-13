package nestoredidev.bodega.caja.application.ports.output;

import nestoredidev.bodega.caja.domain.model.CajaBodega;
import nestoredidev.bodega.caja.domain.model.CajaBodegaResumen;
import nestoredidev.bodega.caja.domain.model.MovimientoBodega;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;

import java.util.Optional;

public interface CajaBodegaPersistencePort {

    CajaBodega guardar(CajaBodega caja);

    Optional<CajaBodega> buscarPorId(Long id);

    Optional<CajaBodega> buscarAbierta();

    PageResult<CajaBodega> listar(PageQuery query);

    CajaBodegaResumen resumen(Long cajaId);

    MovimientoBodega guardarMovimiento(MovimientoBodega movimiento);

    PageResult<MovimientoBodega> listarMovimientos(Long cajaId, PageQuery query);
}
