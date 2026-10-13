package nestoredidev.cajacancha.application.ports.input;

import nestoredidev.cajacancha.application.ports.input.dto.*;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;

import java.math.BigDecimal;

public interface CajaCanchaServicePort {

    CajaCanchaDto abrir(AbrirCajaCommand command);

    CajaCanchaDto actual();

    CajaCanchaDto cerrar(CerrarCajaCommand command);

    CajaCanchaDto obtener(Long id);

    PageResult<CajaCanchaDto> listar(PageQuery query);

    MovimientoCajaDto registrarMovimiento(MovimientoCajaCommand command);

    PageResult<MovimientoCajaDto> listarMovimientos(Long cajaId, PageQuery query);

    // ---- API interna para otros módulos de cancha (reserva/pagos) ----

    /** RN-G05: id de la caja abierta; falla si no hay ninguna. */
    Long exigirCajaAbiertaId();

    /** Efectivo que debería haber ahora mismo en la caja indicada. */
    BigDecimal efectivoEsperado(Long cajaId);
}
