package nestoredidev.reserva.application.service;

import nestoredidev.cajacancha.application.ports.input.CajaCanchaServicePort;
import nestoredidev.cancha.application.ports.input.CanchaServicePort;
import nestoredidev.cancha.application.ports.input.TarifaServicePort;
import nestoredidev.cancha.application.ports.input.dto.CanchaDto;
import nestoredidev.cliente.application.ports.input.ClienteServicePort;
import nestoredidev.cliente.application.ports.input.dto.ClienteDto;
import nestoredidev.common.application.CurrentUser;
import nestoredidev.common.application.CurrentUserPort;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;
import nestoredidev.common.domain.MetodoPago;
import nestoredidev.common.domain.Money;
import nestoredidev.common.exception.ConflictException;
import nestoredidev.common.exception.DomainException;
import nestoredidev.common.exception.ForbiddenOperationException;
import nestoredidev.reserva.application.ports.input.ReservaServicePort;
import nestoredidev.reserva.application.ports.input.dto.*;
import nestoredidev.reserva.application.ports.output.ReservaPersistencePort;
import nestoredidev.reserva.domain.exception.InvalidReservaException;
import nestoredidev.reserva.domain.exception.ReservaNotFoundException;
import nestoredidev.reserva.domain.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReservaService implements ReservaServicePort {
    private final ReservaPersistencePort persistence;
    private final CanchaServicePort canchaService;
    private final TarifaServicePort tarifaService;
    private final ClienteServicePort clienteService;
    private final CajaCanchaServicePort cajaService;
    private final CurrentUserPort currentUser;
    private final Clock clock;
    private final BigDecimal adelantoPorcentaje;

    public ReservaService(ReservaPersistencePort persistence, CanchaServicePort canchaService,
                          TarifaServicePort tarifaService, ClienteServicePort clienteService,
                          CajaCanchaServicePort cajaService, CurrentUserPort currentUser, Clock clock,
                          @Value("${app.reserva.adelanto-porcentaje}") BigDecimal adelantoPorcentaje) {
        this.persistence = persistence;
        this.canchaService = canchaService;
        this.tarifaService = tarifaService;
        this.clienteService = clienteService;
        this.cajaService = cajaService;
        this.currentUser = currentUser;
        this.clock = clock;
        this.adelantoPorcentaje = adelantoPorcentaje;
    }

    // ------------------------------------------------------------------ creación

    @Override
    @Transactional
    public ReservaDto crear(CreateReservaCommand cmd) {
        OffsetDateTime ahora = ahora();
        OffsetDateTime inicio = aOffset(cmd.inicio());
        OffsetDateTime fin = aOffset(cmd.fin());
        CanchaDto cancha = canchaService.obtener(cmd.canchaId());
        exigirCanchaActiva(cancha);
        ClienteDto cliente = resolverCliente(cmd);

        Reserva reserva = Reserva.crearReserva(cancha.id(), cliente.id(), inicio, fin,
                tarifasVigentes(cancha.id(), diaDe(inicio)), cmd.origen(), cmd.observacion(),
                currentUser.get().id(), ahora);
        validarSinCruce(reserva, null);

        List<String> advertencias = new ArrayList<>();
        long inasistencias = persistence.contarPorClienteYEstado(cliente.id(), EstadoReserva.NO_ASISTIO);
        if (inasistencias >= 2) {
            advertencias.add("RN-CLI-04: el cliente tiene " + inasistencias
                    + " inasistencias; se exige adelanto y no se confirma sin pago");
        }

        Reserva guardada = persistence.guardar(reserva);
        if (cmd.pagoInicial() != null) {
            if (cmd.pagoInicial().esDevolucion()) {
                throw new InvalidReservaException("El pago inicial no puede ser una devolución");
            }
            aplicarPago(guardada, cmd.pagoInicial());
        }
        return toDto(guardada, advertencias, null);
    }

    @Override
    @Transactional
    public ReservaDto crearMantenimiento(CreateMantenimientoCommand cmd) {
        exigirAdmin("RN-RES-20: solo el ADMIN registra mantenimientos");
        OffsetDateTime inicio = aOffset(cmd.inicio());
        OffsetDateTime fin = aOffset(cmd.fin());
        CanchaDto cancha = canchaService.obtener(cmd.canchaId());
        exigirCanchaActiva(cancha);

        Reserva mantenimiento = Reserva.crearMantenimiento(cancha.id(), inicio, fin,
                tarifasVigentes(cancha.id(), diaDe(inicio)), cmd.motivo(), currentUser.get().id(), ahora());
        validarSinCruce(mantenimiento, null); // RN-RES-22: lista las reservas en conflicto con su teléfono
        return toDto(persistence.guardar(mantenimiento), null, null);
    }

    // ------------------------------------------------------------------ consultas

    @Override
    @Transactional(readOnly = true)
    public ReservaDto obtener(Long id) {
        return toDto(buscar(id), null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ReservaDto> listar(ReservaFiltro filtro, PageQuery query) {
        PageResult<Reserva> page = persistence.listar(filtro, query);
        List<Reserva> reservas = page.content();
        Map<Long, BigDecimal> pagados = persistence.pagadoNetoPorReservas(
                reservas.stream().map(Reserva::getId).toList());
        Map<Long, ClienteDto> clientes = clientesDe(reservas);
        List<ReservaDto> dtos = reservas.stream().map(r -> {
            ClienteDto c = r.getClienteId() == null ? null : clientes.get(r.getClienteId());
            return ReservaDto.of(r, pagados.getOrDefault(r.getId(), Money.ZERO), r.adelantoMinimo(adelantoPorcentaje),
                    c == null ? null : c.nombre(), c == null ? null : c.telefono(), null, null);
        }).toList();
        return new PageResult<>(dtos, page.page(), page.size(), page.totalElements(), page.totalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SlotDto> disponibilidad(Long canchaId, LocalDate fecha) {
        CanchaDto cancha = canchaService.obtener(canchaId);
        ZoneId zona = clock.getZone();
        OffsetDateTime inicioDia = fecha.atStartOfDay(zona).toOffsetDateTime();
        OffsetDateTime finDia = fecha.plusDays(1).atStartOfDay(zona).toOffsetDateTime();
        short dia = (short) fecha.getDayOfWeek().getValue();
        List<TarifaVigente> tarifas = tarifasVigentes(cancha.id(), dia);
        List<Reserva> ocupadas = persistence.buscarCruces(cancha.id(), inicioDia, finDia, null);
        OffsetDateTime ahora = ahora();
        List<SlotDto> slots = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            LocalTime desde = LocalTime.of(h, 0);
            BigDecimal precio = Reserva.precioDeLaHora(tarifas, dia, desde);
            if (precio == null) {
                continue;
            }
            OffsetDateTime slotInicio = fecha.atTime(desde).atZone(zona).toOffsetDateTime();
            OffsetDateTime slotFin = slotInicio.plusHours(1);
            boolean libre = cancha.activa()
                    && ocupadas.stream().noneMatch(o -> o.getInicio().isBefore(slotFin) && o.getFin().isAfter(slotInicio))
                    && !slotInicio.isBefore(ahora.truncatedTo(java.time.temporal.ChronoUnit.HOURS));
            slots.add(new SlotDto(desde, desde.plusHours(1), precio, libre));
        }
        return slots;
    }

    // ------------------------------------------------------------------ cambios de estado

    @Override
    @Transactional
    public ReservaDto reprogramar(Long id, ReprogramarCommand cmd) {
        Reserva reserva = buscarParaActualizar(id);
        CanchaDto cancha = canchaService.obtener(reserva.getCanchaId());
        exigirCanchaActiva(cancha);
        OffsetDateTime inicio = aOffset(cmd.inicio());
        OffsetDateTime fin = aOffset(cmd.fin());
        reserva.reprogramar(inicio, fin, tarifasVigentes(cancha.id(), diaDe(inicio)), ahora());
        validarSinCruce(reserva, reserva.getId());

        if (!reserva.esMantenimiento()) {
            BigDecimal pagado = persistence.pagadoNeto(reserva.getId());
            BigDecimal exceso = pagado.subtract(reserva.getMontoTotal());
            if (exceso.signum() > 0) { // RN-RES-17: se devuelve la diferencia
                Long cajaId = cajaService.exigirCajaAbiertaId();
                if (exceso.compareTo(cajaService.efectivoEsperado(cajaId)) > 0) {
                    throw new DomainException("RN-MCA-03: no hay efectivo suficiente en caja para devolver S/ " + exceso);
                }
                persistence.guardarPago(PagoReserva.crear(reserva.getId(), cajaId, TipoPagoReserva.DEVOLUCION,
                        MetodoPago.EFECTIVO, exceso, null, null, currentUser.get().id()));
                pagado = pagado.subtract(exceso);
            }
            reserva.confirmarSiCorresponde(pagado, adelantoPorcentaje);
        }
        return toDto(persistence.guardar(reserva), null, null);
    }

    @Override
    @Transactional
    public ReservaDto cancelar(Long id, CancelarReservaCommand cmd) {
        Reserva reserva = buscarParaActualizar(id);
        if (reserva.esMantenimiento()) {
            exigirAdmin("RN-RES-23: solo el ADMIN cancela mantenimientos");
        }
        OffsetDateTime ahora = ahora();
        boolean conDevolucion = reserva.cancelacionConDevolucion(ahora);
        reserva.cancelar(cmd.motivo());
        persistence.guardar(reserva);
        BigDecimal sugerida = reserva.esMantenimiento() ? null
                : (conDevolucion ? persistence.pagadoNeto(reserva.getId()) : Money.ZERO);
        return toDto(reserva, null, sugerida);
    }

    @Override
    @Transactional
    public ReservaDto marcarNoAsistio(Long id) {
        Reserva reserva = buscarParaActualizar(id);
        reserva.marcarNoAsistio(ahora());
        return toDto(persistence.guardar(reserva), null, null);
    }

    @Override
    @Transactional
    public ReservaDto completar(Long id) {
        Reserva reserva = buscarParaActualizar(id);
        reserva.completar(persistence.pagadoNeto(id), ahora());
        return toDto(persistence.guardar(reserva), null, null);
    }

    @Override
    @Transactional
    public ReservaDto cambiarMonto(Long id, CambiarMontoCommand cmd) {
        exigirAdmin("RN-RES-10: solo el ADMIN cambia el monto de una reserva");
        Reserva reserva = buscarParaActualizar(id);
        BigDecimal pagado = persistence.pagadoNeto(id);
        reserva.cambiarMonto(cmd.monto(), cmd.observacion(), pagado);
        reserva.confirmarSiCorresponde(pagado, adelantoPorcentaje);
        reserva.completarSiCorresponde(pagado, ahora());
        return toDto(persistence.guardar(reserva), null, null);
    }

    // ------------------------------------------------------------------ pagos

    @Override
    @Transactional
    public PagoReservaDto registrarPago(Long reservaId, PagoReservaCommand cmd) {
        Reserva reserva = buscarParaActualizar(reservaId);
        return PagoReservaDto.fromDomain(aplicarPago(reserva, cmd));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PagoReservaDto> listarPagos(Long reservaId) {
        buscar(reservaId);
        return persistence.listarPagos(reservaId).stream().map(PagoReservaDto::fromDomain).toList();
    }

    private PagoReserva aplicarPago(Reserva reserva, PagoReservaCommand cmd) {
        if (reserva.esMantenimiento()) {
            throw new DomainException("RN-PAG-02: un mantenimiento no recibe pagos");
        }
        CurrentUser user = currentUser.get();
        Long cajaId = cajaService.exigirCajaAbiertaId(); // RN-PAG-01 / RN-G05
        BigDecimal pagado = persistence.pagadoNeto(reserva.getId());
        BigDecimal monto = Money.of(cmd.monto());
        TipoPagoReserva tipo;

        if (cmd.esDevolucion()) {
            if (!user.isAdmin()) {
                throw new ForbiddenOperationException("RN-PAG-07: solo el ADMIN registra devoluciones");
            }
            boolean hayExceso = pagado.compareTo(reserva.getMontoTotal()) > 0;
            if (reserva.getEstado() != EstadoReserva.CANCELADA && !hayExceso) {
                throw new DomainException("RN-PAG-07: la devolución aplica a reservas canceladas o con pago en exceso");
            }
            if (monto.compareTo(pagado) > 0) {
                throw new DomainException("RN-PAG-07: la devolución no puede superar lo pagado (S/ " + pagado + ")");
            }
            if (cmd.metodo() == MetodoPago.EFECTIVO && monto.compareTo(cajaService.efectivoEsperado(cajaId)) > 0) {
                throw new DomainException("RN-MCA-03: no hay efectivo suficiente en caja para devolver S/ " + monto);
            }
            tipo = TipoPagoReserva.DEVOLUCION;
        } else {
            if (!reserva.getEstado().esActiva()) {
                throw new DomainException("RN-PAG-02: no se registran pagos en una reserva " + reserva.getEstado());
            }
            BigDecimal saldo = reserva.getMontoTotal().subtract(pagado);
            if (saldo.signum() <= 0) {
                throw new DomainException("La reserva ya está pagada por completo");
            }
            if (monto.compareTo(saldo) > 0) {
                throw new DomainException("RN-PAG-03: el monto supera el saldo pendiente (S/ " + saldo + ")");
            }
            tipo = monto.compareTo(saldo) == 0 ? TipoPagoReserva.SALDO : TipoPagoReserva.ADELANTO; // RN-PAG-04
        }

        PagoReserva pago = PagoReserva.crear(reserva.getId(), cajaId, tipo, cmd.metodo(), monto,
                cmd.nroOperacion(), cmd.voucherUrl(), user.id());
        if (pago.getNroOperacion() != null && persistence.existePagoConOperacion(pago.getMetodo(), pago.getNroOperacion())) {
            throw new ConflictException("RN-PAG-05: el número de operación " + pago.getNroOperacion()
                    + " ya fue registrado con " + pago.getMetodo());
        }
        PagoReserva guardado = persistence.guardarPago(pago);

        if (tipo != TipoPagoReserva.DEVOLUCION) { // RN-PAG-08
            BigDecimal nuevoPagado = pagado.add(monto);
            reserva.confirmarSiCorresponde(nuevoPagado, adelantoPorcentaje);
            reserva.completarSiCorresponde(nuevoPagado, ahora());
            persistence.guardar(reserva);
        }
        return guardado;
    }

    // ------------------------------------------------------------------ proceso automático

    @Override
    @Transactional
    public int cancelarVencidas() {
        int canceladas = 0;
        for (Reserva candidata : persistence.buscarPendientesConAdelantoVencido(ahora())) {
            Reserva r = persistence.buscarPorIdParaActualizar(candidata.getId()).orElse(null);
            if (r != null && r.adelantoVencido(ahora())) {
                r.cancelar("Adelanto no pagado");
                persistence.guardar(r);
                canceladas++;
            }
        }
        return canceladas;
    }

    // ------------------------------------------------------------------ auxiliares

    private Reserva buscar(Long id) {
        return persistence.buscarPorId(id).orElseThrow(() -> new ReservaNotFoundException(id));
    }

    private Reserva buscarParaActualizar(Long id) {
        return persistence.buscarPorIdParaActualizar(id).orElseThrow(() -> new ReservaNotFoundException(id));
    }

    private OffsetDateTime ahora() {
        return OffsetDateTime.now(clock);
    }

    private OffsetDateTime aOffset(LocalDateTime local) {
        return local.atZone(clock.getZone()).toOffsetDateTime();
    }

    private short diaDe(OffsetDateTime fecha) {
        return (short) fecha.atZoneSameInstant(clock.getZone()).getDayOfWeek().getValue();
    }

    private void exigirAdmin(String mensaje) {
        if (!currentUser.get().isAdmin()) {
            throw new ForbiddenOperationException(mensaje);
        }
    }

    private void exigirCanchaActiva(CanchaDto cancha) {
        if (!cancha.activa()) {
            throw new DomainException("RN-CAN-02: la cancha '" + cancha.nombre() + "' está inactiva");
        }
    }

    private ClienteDto resolverCliente(CreateReservaCommand cmd) {
        if (cmd.clienteId() != null) {
            return clienteService.obtener(cmd.clienteId());
        }
        if (cmd.clienteNombre() == null || cmd.clienteNombre().isBlank()
                || cmd.clienteTelefono() == null || cmd.clienteTelefono().isBlank()) {
            throw new InvalidReservaException("Indica clienteId, o clienteNombre y clienteTelefono");
        }
        return clienteService.buscarOCrear(cmd.clienteNombre(), cmd.clienteTelefono());
    }

    private List<TarifaVigente> tarifasVigentes(Long canchaId, short dia) {
        return tarifaService.listar(canchaId, true).stream()
                .filter(t -> t.diaSemana() == null || t.diaSemana() == dia)
                .map(t -> new TarifaVigente(t.diaSemana(), t.horaInicio(), t.horaFin(), t.precioHora()))
                .toList();
    }

    /** RN-RES-06 / RN-RES-22: una hora solo puede estar ocupada por una reserva o un mantenimiento. */
    private void validarSinCruce(Reserva r, Long excluirId) {
        List<Reserva> cruces = persistence.buscarCruces(r.getCanchaId(), r.getInicio(), r.getFin(), excluirId);
        if (cruces.isEmpty()) {
            return;
        }
        Map<Long, ClienteDto> clientes = clientesDe(cruces);
        ZoneId zona = clock.getZone();
        List<String> detalle = cruces.stream().map(c -> {
            String horas = c.getInicio().atZoneSameInstant(zona).toLocalDateTime() + " - "
                    + c.getFin().atZoneSameInstant(zona).toLocalTime();
            if (c.esMantenimiento()) {
                return "Mantenimiento #" + c.getId() + " " + horas;
            }
            ClienteDto cl = clientes.get(c.getClienteId());
            return "Reserva #" + c.getId() + " " + horas + " [" + c.getEstado() + "]"
                    + (cl == null ? "" : " " + cl.nombre() + " (" + cl.telefono() + ")");
        }).toList();
        throw new ConflictException("RN-RES-06: el horario se cruza con otra reserva o mantenimiento", detalle);
    }

    private Map<Long, ClienteDto> clientesDe(Collection<Reserva> reservas) {
        Set<Long> ids = reservas.stream().map(Reserva::getClienteId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return clienteService.obtenerPorIds(ids).stream()
                .collect(Collectors.toMap(ClienteDto::id, Function.identity()));
    }

    private ReservaDto toDto(Reserva r, List<String> advertencias, BigDecimal devolucionSugerida) {
        ClienteDto cliente = r.getClienteId() == null ? null : clienteService.obtener(r.getClienteId());
        return ReservaDto.of(r, persistence.pagadoNeto(r.getId()), r.adelantoMinimo(adelantoPorcentaje),
                cliente == null ? null : cliente.nombre(), cliente == null ? null : cliente.telefono(),
                advertencias, devolucionSugerida);
    }
}
