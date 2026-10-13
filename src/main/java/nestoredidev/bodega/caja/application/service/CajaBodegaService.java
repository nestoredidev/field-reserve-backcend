package nestoredidev.bodega.caja.application.service;

import lombok.RequiredArgsConstructor;
import nestoredidev.bodega.caja.application.ports.input.CajaBodegaServicePort;
import nestoredidev.bodega.caja.application.ports.input.dto.*;
import nestoredidev.bodega.caja.application.ports.output.CajaBodegaPersistencePort;
import nestoredidev.bodega.caja.domain.exception.CajaBodegaNotFoundException;
import nestoredidev.bodega.caja.domain.model.CajaBodega;
import nestoredidev.bodega.caja.domain.model.MovimientoBodega;
import nestoredidev.common.application.CurrentUserPort;
import nestoredidev.common.domain.MetodoPago;
import nestoredidev.common.domain.TipoMovimientoCaja;
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
public class CajaBodegaService implements CajaBodegaServicePort {
    private final CajaBodegaPersistencePort persistence;
    private final CurrentUserPort currentUser;
    private final Clock clock;

    @Override
    @Transactional
    public CajaBodegaDto abrir(AbrirCajaCommand command) {
        if (persistence.buscarAbierta().isPresent()) {
            throw new ConflictException("RN-CAJ-01: ya existe una caja de bodega abierta");
        }
        CajaBodega caja = persistence.guardar(
                CajaBodega.abrir(command.montoApertura(), currentUser.get().id(), OffsetDateTime.now(clock)));
        return toDto(caja);
    }

    @Override
    @Transactional(readOnly = true)
    public CajaBodegaDto actual() {
        return toDto(persistence.buscarAbierta()
                .orElseThrow(() -> new CajaBodegaNotFoundException("No hay una caja de bodega abierta")));
    }

    @Override
    @Transactional
    public CajaBodegaDto cerrar(CerrarCajaCommand command) {
        CajaBodega caja = persistence.buscarAbierta()
                .orElseThrow(() -> new CajaBodegaNotFoundException("No hay una caja de bodega abierta"));
        BigDecimal esperado = persistence.resumen(caja.getId()).efectivoEsperado();
        caja.cerrar(command.montoCierreContado(), esperado, command.observacion(), currentUser.get().id(),
                OffsetDateTime.now(clock));
        return toDto(persistence.guardar(caja));
    }

    @Override
    @Transactional(readOnly = true)
    public CajaBodegaDto obtener(Long id) {
        return toDto(buscar(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CajaBodegaDto> listar(PageQuery query) {
        return persistence.listar(query).map(this::toDto);
    }

    @Override
    @Transactional
    public MovimientoBodegaDto registrarMovimiento(MovimientoBodegaCommand command) {
        Long cajaId = exigirCajaAbiertaId();
        MovimientoBodega movimiento = MovimientoBodega.crear(cajaId, command.tipo(), command.metodo(),
                command.concepto(), command.monto(), currentUser.get().id());
        if (movimiento.esEgresoEfectivo() && movimiento.getMonto().compareTo(efectivoEsperado(cajaId)) > 0) {
            throw new DomainException("RN-MCA-03: el egreso en efectivo supera el efectivo disponible en caja");
        }
        return MovimientoBodegaDto.fromDomain(persistence.guardarMovimiento(movimiento));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<MovimientoBodegaDto> listarMovimientos(Long cajaId, PageQuery query) {
        buscar(cajaId);
        return persistence.listarMovimientos(cajaId, query).map(MovimientoBodegaDto::fromDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Long exigirCajaAbiertaId() {
        return persistence.buscarAbierta()
                .orElseThrow(() -> new DomainException("RN-G05: no hay una caja de bodega abierta; ábrela primero"))
                .getId();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal efectivoEsperado(Long cajaId) {
        return persistence.resumen(cajaId).efectivoEsperado();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean estaAbierta(Long cajaId) {
        return persistence.buscarPorId(cajaId).map(CajaBodega::estaAbierta).orElse(false);
    }

    @Override
    @Transactional
    public void registrarEgresoEfectivo(String concepto, BigDecimal monto) {
        registrarMovimiento(new MovimientoBodegaCommand(TipoMovimientoCaja.EGRESO, MetodoPago.EFECTIVO, concepto, monto));
    }

    private CajaBodega buscar(Long id) {
        return persistence.buscarPorId(id)
                .orElseThrow(() -> new CajaBodegaNotFoundException("Caja con id " + id + " no existe"));
    }

    private CajaBodegaDto toDto(CajaBodega caja) {
        String advertencia = caja.estaAbierta() && caja.getFecha().isBefore(OffsetDateTime.now(clock).toLocalDate())
                ? "RN-CAJ-06: la caja abierta es de un día anterior (" + caja.getFecha() + "); se recomienda cerrarla"
                : null;
        return CajaBodegaDto.of(caja, persistence.resumen(caja.getId()), currentUser.get().isAdmin(), advertencia);
    }
}
