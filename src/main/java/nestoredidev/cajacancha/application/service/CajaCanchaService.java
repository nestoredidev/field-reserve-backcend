package nestoredidev.cajacancha.application.service;

import lombok.RequiredArgsConstructor;
import nestoredidev.cajacancha.application.ports.input.CajaCanchaServicePort;
import nestoredidev.cajacancha.application.ports.input.dto.*;
import nestoredidev.cajacancha.application.ports.output.CajaCanchaPersistencePort;
import nestoredidev.cajacancha.domain.exception.CajaCanchaNotFoundException;
import nestoredidev.cajacancha.domain.model.CajaCancha;
import nestoredidev.cajacancha.domain.model.MovimientoCaja;
import nestoredidev.common.application.CurrentUserPort;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;
import nestoredidev.common.exception.ConflictException;
import nestoredidev.common.exception.DomainException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class CajaCanchaService implements CajaCanchaServicePort {
    private final CajaCanchaPersistencePort persistence;
    private final CurrentUserPort currentUser;
    private final Clock clock;

    @Override
    @Transactional
    public CajaCanchaDto abrir(AbrirCajaCommand command) {
        if (persistence.buscarAbierta().isPresent()) {
            throw new ConflictException("RN-CAJ-01: ya existe una caja de cancha abierta");
        }
        CajaCancha caja = persistence.guardar(
                CajaCancha.abrir(command.montoApertura(), currentUser.get().id(), OffsetDateTime.now(clock)));
        return toDto(caja);
    }

    @Override
    @Transactional(readOnly = true)
    public CajaCanchaDto actual() {
        return toDto(persistence.buscarAbierta()
                .orElseThrow(() -> new CajaCanchaNotFoundException("No hay una caja de cancha abierta")));
    }

    @Override
    @Transactional
    public CajaCanchaDto cerrar(CerrarCajaCommand command) {
        CajaCancha caja = persistence.buscarAbierta()
                .orElseThrow(() -> new CajaCanchaNotFoundException("No hay una caja de cancha abierta"));
        BigDecimal esperado = persistence.resumen(caja.getId()).efectivoEsperado();
        caja.cerrar(command.montoCierreContado(), esperado, command.observacion(), currentUser.get().id(),
                OffsetDateTime.now(clock));
        return toDto(persistence.guardar(caja));
    }

    @Override
    @Transactional(readOnly = true)
    public CajaCanchaDto obtener(Long id) {
        return toDto(buscar(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CajaCanchaDto> listar(PageQuery query) {
        return persistence.listar(query).map(this::toDto);
    }

    @Override
    @Transactional
    public MovimientoCajaDto registrarMovimiento(MovimientoCajaCommand command) {
        Long cajaId = exigirCajaAbiertaId();
        MovimientoCaja movimiento = MovimientoCaja.crear(cajaId, command.tipo(), command.metodo(),
                command.concepto(), command.monto(), currentUser.get().id());
        if (movimiento.esEgresoEfectivo() && movimiento.getMonto().compareTo(efectivoEsperado(cajaId)) > 0) {
            throw new DomainException("RN-MCA-03: el egreso en efectivo supera el efectivo disponible en caja");
        }
        return MovimientoCajaDto.fromDomain(persistence.guardarMovimiento(movimiento));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<MovimientoCajaDto> listarMovimientos(Long cajaId, PageQuery query) {
        buscar(cajaId);
        return persistence.listarMovimientos(cajaId, query).map(MovimientoCajaDto::fromDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Long exigirCajaAbiertaId() {
        return persistence.buscarAbierta()
                .orElseThrow(() -> new DomainException("RN-G05: no hay una caja de cancha abierta; ábrela primero"))
                .getId();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal efectivoEsperado(Long cajaId) {
        return persistence.resumen(cajaId).efectivoEsperado();
    }

    private CajaCancha buscar(Long id) {
        return persistence.buscarPorId(id)
                .orElseThrow(() -> new CajaCanchaNotFoundException("Caja con id " + id + " no existe"));
    }

    private CajaCanchaDto toDto(CajaCancha caja) {
        String advertencia = caja.estaAbierta() && caja.getFecha().isBefore(OffsetDateTime.now(clock).toLocalDate())
                ? "RN-CAJ-06: la caja abierta es de un día anterior (" + caja.getFecha() + "); se recomienda cerrarla"
                : null;
        return CajaCanchaDto.of(caja, persistence.resumen(caja.getId()), currentUser.get().isAdmin(), advertencia);
    }
}
