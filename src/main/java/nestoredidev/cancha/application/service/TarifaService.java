package nestoredidev.cancha.application.service;

import lombok.RequiredArgsConstructor;
import nestoredidev.cancha.application.ports.input.TarifaServicePort;
import nestoredidev.cancha.application.ports.input.dto.CambiarPrecioCommand;
import nestoredidev.cancha.application.ports.input.dto.CreateTarifaCommand;
import nestoredidev.cancha.application.ports.input.dto.TarifaDto;
import nestoredidev.cancha.application.ports.output.CanchaPersistencePort;
import nestoredidev.cancha.application.ports.output.TarifaPersistencePort;
import nestoredidev.cancha.domain.exception.CanchaNotFoundException;
import nestoredidev.cancha.domain.exception.TarifaNotFoundException;
import nestoredidev.cancha.domain.model.Tarifa;
import nestoredidev.common.exception.ConflictException;
import nestoredidev.common.exception.DomainException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TarifaService implements TarifaServicePort {
    private final TarifaPersistencePort persistence;
    private final CanchaPersistencePort canchaPersistence;

    @Override
    @Transactional
    public TarifaDto crear(Long canchaId, CreateTarifaCommand command) {
        if (canchaPersistence.buscarPorId(canchaId).isEmpty()) {
            throw new CanchaNotFoundException(canchaId);
        }
        Tarifa nueva = Tarifa.crear(canchaId, command.diaSemana(), command.horaInicio(), command.horaFin(),
                command.precioHora());
        validarSinCruce(nueva, null);
        return TarifaDto.fromDomain(persistence.guardar(nueva));
    }

    @Override
    @Transactional(readOnly = true)
    public TarifaDto obtener(Long id) {
        return TarifaDto.fromDomain(buscar(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TarifaDto> listar(Long canchaId, boolean soloActivas) {
        if (canchaPersistence.buscarPorId(canchaId).isEmpty()) {
            throw new CanchaNotFoundException(canchaId);
        }
        return persistence.listarPorCancha(canchaId, soloActivas).stream().map(TarifaDto::fromDomain).toList();
    }

    @Override
    @Transactional
    public TarifaDto desactivar(Long id) {
        Tarifa tarifa = buscar(id);
        if (!tarifa.isActiva()) {
            throw new DomainException("La tarifa ya está desactivada");
        }
        return TarifaDto.fromDomain(persistence.guardar(tarifa.desactivar()));
    }

    /** RN-TAR-06: se desactiva la tarifa y se crea una nueva; las reservas existentes no cambian (RN-TAR-05). */
    @Override
    @Transactional
    public TarifaDto cambiarPrecio(Long id, CambiarPrecioCommand command) {
        Tarifa actual = buscar(id);
        if (!actual.isActiva()) {
            throw new DomainException("Solo se puede cambiar el precio de una tarifa activa");
        }
        Tarifa nueva = actual.conNuevoPrecio(command.precioHora());
        persistence.guardar(actual.desactivar());
        return TarifaDto.fromDomain(persistence.guardar(nueva));
    }

    private Tarifa buscar(Long id) {
        return persistence.buscarPorId(id).orElseThrow(() -> new TarifaNotFoundException(id));
    }

    private void validarSinCruce(Tarifa t, Long excluirId) {
        if (persistence.existeCruce(t.getCanchaId(), t.getDiaSemana(), t.getHoraInicio(), t.getHoraFin(), excluirId)) {
            throw new ConflictException("RN-TAR-01: la franja se cruza con otra tarifa activa de la misma cancha y día");
        }
    }
}
