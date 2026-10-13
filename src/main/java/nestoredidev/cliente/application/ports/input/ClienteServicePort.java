package nestoredidev.cliente.application.ports.input;

import nestoredidev.cliente.application.ports.input.dto.ClienteCommand;
import nestoredidev.cliente.application.ports.input.dto.ClienteDto;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;

import java.util.Collection;
import java.util.List;

public interface ClienteServicePort {

    ClienteDto crear(ClienteCommand command);

    ClienteDto obtener(Long id);

    ClienteDto obtenerPorTelefono(String telefono);

    List<ClienteDto> obtenerPorIds(Collection<Long> ids);

    PageResult<ClienteDto> listar(String q, PageQuery query);

    ClienteDto actualizar(Long id, ClienteCommand command);

    /** RN-CLI-02: busca por teléfono y reutiliza; si no existe lo registra. */
    ClienteDto buscarOCrear(String nombre, String telefono);
}
