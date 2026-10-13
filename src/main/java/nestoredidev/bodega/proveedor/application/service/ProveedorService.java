package nestoredidev.bodega.proveedor.application.service;

import lombok.RequiredArgsConstructor;
import nestoredidev.bodega.proveedor.application.ports.input.ProveedorServicePort;
import nestoredidev.bodega.proveedor.application.ports.input.dto.ProveedorCommand;
import nestoredidev.bodega.proveedor.application.ports.input.dto.ProveedorDto;
import nestoredidev.bodega.proveedor.application.ports.output.ProveedorPersistencePort;
import nestoredidev.bodega.proveedor.domain.exception.ProveedorNotFoundException;
import nestoredidev.bodega.proveedor.domain.model.Proveedor;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;
import nestoredidev.common.exception.ConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProveedorService implements ProveedorServicePort {
    private final ProveedorPersistencePort persistence;

    @Override
    @Transactional
    public ProveedorDto crear(ProveedorCommand command) {
        Proveedor nuevo = Proveedor.crear(command.nombre(), command.ruc(), command.telefono());
        validarRucUnico(nuevo.getRuc(), null);
        return ProveedorDto.fromDomain(persistence.guardar(nuevo));
    }

    @Override
    @Transactional(readOnly = true)
    public ProveedorDto obtener(Long id) {
        return ProveedorDto.fromDomain(buscar(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ProveedorDto> listar(String q, Boolean activo, PageQuery query) {
        return persistence.listar(q == null || q.isBlank() ? null : q.trim(), activo, query)
                .map(ProveedorDto::fromDomain);
    }

    @Override
    @Transactional
    public ProveedorDto actualizar(Long id, ProveedorCommand command) {
        Proveedor actualizado = buscar(id).actualizar(command.nombre(), command.ruc(), command.telefono());
        validarRucUnico(actualizado.getRuc(), id);
        return ProveedorDto.fromDomain(persistence.guardar(actualizado));
    }

    @Override
    @Transactional
    public ProveedorDto activar(Long id) {
        return ProveedorDto.fromDomain(persistence.guardar(buscar(id).conActivo(true)));
    }

    @Override
    @Transactional
    public ProveedorDto desactivar(Long id) {
        return ProveedorDto.fromDomain(persistence.guardar(buscar(id).conActivo(false)));
    }

    private Proveedor buscar(Long id) {
        return persistence.buscarPorId(id).orElseThrow(() -> new ProveedorNotFoundException(id));
    }

    private void validarRucUnico(String ruc, Long excluirId) {
        if (ruc != null && persistence.existePorRuc(ruc, excluirId)) {
            throw new ConflictException("RN-PRV-01: ya existe un proveedor con el RUC " + ruc);
        }
    }
}
