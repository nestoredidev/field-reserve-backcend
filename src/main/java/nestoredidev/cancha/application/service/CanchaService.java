package nestoredidev.cancha.application.service;

import lombok.RequiredArgsConstructor;
import nestoredidev.cancha.application.ports.input.CanchaServicePort;
import nestoredidev.cancha.application.ports.input.dto.CanchaCommand;
import nestoredidev.cancha.application.ports.input.dto.CanchaDto;
import nestoredidev.cancha.application.ports.output.CanchaPersistencePort;
import nestoredidev.cancha.domain.exception.CanchaNotFoundException;
import nestoredidev.cancha.domain.model.Cancha;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;
import nestoredidev.common.exception.ConflictException;
import nestoredidev.common.exception.DomainException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class CanchaService implements CanchaServicePort {
    private final CanchaPersistencePort persistence;
    private final Clock clock;

    @Override
    @Transactional
    public CanchaDto crear(CanchaCommand command) {
        Cancha nueva = Cancha.crear(command.nombre(), command.descripcion());
        validarNombreUnico(nueva.getNombre(), null);
        return CanchaDto.fromDomain(persistence.guardar(nueva));
    }

    @Override
    @Transactional(readOnly = true)
    public CanchaDto obtener(Long id) {
        return CanchaDto.fromDomain(buscar(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CanchaDto> listar(Boolean activa, PageQuery query) {
        return persistence.listar(activa, query).map(CanchaDto::fromDomain);
    }

    @Override
    @Transactional
    public CanchaDto actualizar(Long id, CanchaCommand command) {
        Cancha actualizada = buscar(id).actualizar(command.nombre(), command.descripcion());
        validarNombreUnico(actualizada.getNombre(), id);
        return CanchaDto.fromDomain(persistence.guardar(actualizada));
    }

    @Override
    @Transactional
    public CanchaDto activar(Long id) {
        return CanchaDto.fromDomain(persistence.guardar(buscar(id).conActiva(true)));
    }

    @Override
    @Transactional
    public CanchaDto desactivar(Long id) {
        Cancha cancha = buscar(id);
        if (persistence.tieneOcupacionFutura(id, OffsetDateTime.now(clock))) {
            throw new DomainException("RN-CAN-03: la cancha tiene reservas futuras PENDIENTE o CONFIRMADA; "
                    + "cancélalas o reprográmalas primero");
        }
        return CanchaDto.fromDomain(persistence.guardar(cancha.conActiva(false)));
    }

    private Cancha buscar(Long id) {
        return persistence.buscarPorId(id).orElseThrow(() -> new CanchaNotFoundException(id));
    }

    private void validarNombreUnico(String nombre, Long excluirId) {
        if (persistence.existePorNombre(nombre, excluirId)) {
            throw new ConflictException("RN-CAN-01: ya existe una cancha llamada '" + nombre + "'");
        }
    }
}
