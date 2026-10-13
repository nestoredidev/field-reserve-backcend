package nestoredidev.cancha.application.ports.input;

import nestoredidev.cancha.application.ports.input.dto.CambiarPrecioCommand;
import nestoredidev.cancha.application.ports.input.dto.CreateTarifaCommand;
import nestoredidev.cancha.application.ports.input.dto.TarifaDto;

import java.util.List;

public interface TarifaServicePort {

    TarifaDto crear(Long canchaId, CreateTarifaCommand command);

    TarifaDto obtener(Long id);

    List<TarifaDto> listar(Long canchaId, boolean soloActivas);

    TarifaDto desactivar(Long id);

    TarifaDto cambiarPrecio(Long id, CambiarPrecioCommand command);
}
