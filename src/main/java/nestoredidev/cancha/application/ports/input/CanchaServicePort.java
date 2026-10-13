package nestoredidev.cancha.application.ports.input;

import nestoredidev.cancha.application.ports.input.dto.CanchaCommand;
import nestoredidev.cancha.application.ports.input.dto.CanchaDto;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;

public interface CanchaServicePort {

    CanchaDto crear(CanchaCommand command);

    CanchaDto obtener(Long id);

    PageResult<CanchaDto> listar(Boolean activa, PageQuery query);

    CanchaDto actualizar(Long id, CanchaCommand command);

    CanchaDto activar(Long id);

    CanchaDto desactivar(Long id);
}
