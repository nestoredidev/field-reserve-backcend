package nestoredidev.cajacancha.application.ports.output;

import nestoredidev.cajacancha.domain.model.CajaCancha;
import nestoredidev.cajacancha.domain.model.CajaResumen;
import nestoredidev.cajacancha.domain.model.MovimientoCaja;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;

import java.util.Optional;

public interface CajaCanchaPersistencePort {

    CajaCancha guardar(CajaCancha caja);

    Optional<CajaCancha> buscarPorId(Long id);

    Optional<CajaCancha> buscarAbierta();

    PageResult<CajaCancha> listar(PageQuery query);

    CajaResumen resumen(Long cajaId);

    MovimientoCaja guardarMovimiento(MovimientoCaja movimiento);

    PageResult<MovimientoCaja> listarMovimientos(Long cajaId, PageQuery query);
}
