package nestoredidev.bodega.caja.application.ports.input;

import nestoredidev.bodega.caja.application.ports.input.dto.*;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;

import java.math.BigDecimal;

public interface CajaBodegaServicePort {

    CajaBodegaDto abrir(AbrirCajaCommand command);

    CajaBodegaDto actual();

    CajaBodegaDto cerrar(CerrarCajaCommand command);

    CajaBodegaDto obtener(Long id);

    PageResult<CajaBodegaDto> listar(PageQuery query);

    MovimientoBodegaDto registrarMovimiento(MovimientoBodegaCommand command);

    PageResult<MovimientoBodegaDto> listarMovimientos(Long cajaId, PageQuery query);

    // ---- API interna para otros módulos de la bodega (compras/ventas) ----

    /** RN-G05: id de la caja abierta; falla si no hay ninguna. */
    Long exigirCajaAbiertaId();

    /** Efectivo que debería haber ahora mismo en la caja indicada. */
    BigDecimal efectivoEsperado(Long cajaId);

    /** Si la caja indicada sigue abierta (RN-VEN-04). */
    boolean estaAbierta(Long cajaId);

    /** RN-COM-05: egreso en efectivo de la caja abierta (p. ej. "Compra #12"). */
    void registrarEgresoEfectivo(String concepto, BigDecimal monto);
}
