package nestoredidev.cliente.application.service;

import lombok.RequiredArgsConstructor;
import nestoredidev.cliente.application.ports.input.ClienteServicePort;
import nestoredidev.cliente.application.ports.input.dto.ClienteCommand;
import nestoredidev.cliente.application.ports.input.dto.ClienteDto;
import nestoredidev.cliente.application.ports.output.ClientePersistencePort;
import nestoredidev.cliente.domain.exception.ClienteNotFoundException;
import nestoredidev.cliente.domain.model.Cliente;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;
import nestoredidev.common.exception.ConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClienteService implements ClienteServicePort {
    private final ClientePersistencePort persistence;

    @Override
    @Transactional
    public ClienteDto crear(ClienteCommand command) {
        Cliente nuevo = Cliente.crear(command.nombre(), command.telefono(), command.notas());
        validarTelefonoUnico(nuevo.getTelefono(), null);
        return ClienteDto.fromDomain(persistence.guardar(nuevo));
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteDto obtener(Long id) {
        return ClienteDto.fromDomain(buscar(id));
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteDto obtenerPorTelefono(String telefono) {
        return persistence.buscarPorTelefono(telefono == null ? "" : telefono.trim())
                .map(ClienteDto::fromDomain)
                .orElseThrow(() -> new ClienteNotFoundException("No existe un cliente con teléfono " + telefono));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteDto> obtenerPorIds(Collection<Long> ids) {
        return ids.isEmpty() ? List.of() : persistence.buscarPorIds(ids).stream().map(ClienteDto::fromDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ClienteDto> listar(String q, PageQuery query) {
        return persistence.listar(q == null || q.isBlank() ? null : q.trim(), query).map(ClienteDto::fromDomain);
    }

    @Override
    @Transactional
    public ClienteDto actualizar(Long id, ClienteCommand command) {
        Cliente actualizado = buscar(id).actualizar(command.nombre(), command.telefono(), command.notas());
        validarTelefonoUnico(actualizado.getTelefono(), id);
        return ClienteDto.fromDomain(persistence.guardar(actualizado));
    }

    @Override
    @Transactional
    public ClienteDto buscarOCrear(String nombre, String telefono) {
        String tel = Cliente.validarTelefono(telefono);
        return persistence.buscarPorTelefono(tel)
                .map(ClienteDto::fromDomain)
                .orElseGet(() -> ClienteDto.fromDomain(persistence.guardar(Cliente.crear(nombre, tel, null))));
    }

    private Cliente buscar(Long id) {
        return persistence.buscarPorId(id).orElseThrow(() -> ClienteNotFoundException.porId(id));
    }

    private void validarTelefonoUnico(String telefono, Long excluirId) {
        if (persistence.existePorTelefono(telefono, excluirId)) {
            throw new ConflictException("RN-CLI-02: ya existe un cliente con el teléfono " + telefono);
        }
    }
}
