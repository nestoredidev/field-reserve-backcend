package nestoredidev.reserva.application.ports.input;

import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;
import nestoredidev.reserva.application.ports.input.dto.*;

import java.time.LocalDate;
import java.util.List;

public interface ReservaServicePort {

    ReservaDto crear(CreateReservaCommand command);

    ReservaDto crearMantenimiento(CreateMantenimientoCommand command);

    ReservaDto obtener(Long id);

    PageResult<ReservaDto> listar(ReservaFiltro filtro, PageQuery query);

    List<SlotDto> disponibilidad(Long canchaId, LocalDate fecha);

    ReservaDto reprogramar(Long id, ReprogramarCommand command);

    ReservaDto cancelar(Long id, CancelarReservaCommand command);

    ReservaDto marcarNoAsistio(Long id);

    ReservaDto completar(Long id);

    ReservaDto cambiarMonto(Long id, CambiarMontoCommand command);

    PagoReservaDto registrarPago(Long reservaId, PagoReservaCommand command);

    List<PagoReservaDto> listarPagos(Long reservaId);

    /** RN-RES-14: cancela las PENDIENTE cuyo plazo de adelanto venció. Devuelve cuántas canceló. */
    int cancelarVencidas();
}
