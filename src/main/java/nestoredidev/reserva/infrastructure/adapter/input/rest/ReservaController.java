package nestoredidev.reserva.infrastructure.adapter.input.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nestoredidev.common.infrastructure.web.ApiResponses;
import nestoredidev.common.infrastructure.web.PageParams;
import nestoredidev.common.utils.pagination.PageResponse;
import nestoredidev.reserva.application.ports.input.ReservaServicePort;
import nestoredidev.reserva.application.ports.input.dto.*;
import nestoredidev.reserva.domain.model.EstadoReserva;
import nestoredidev.reserva.domain.model.TipoReserva;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v2")
@RequiredArgsConstructor
public class ReservaController {
    private final ReservaServicePort service;

    @PostMapping("/reservas")
    public ResponseEntity<PageResponse<ReservaDto>> crear(@Valid @RequestBody CreateReservaCommand command) {
        return ApiResponses.created(service.crear(command), "Reserva creada");
    }

    @GetMapping("/reservas")
    public ResponseEntity<PageResponse<ReservaDto>> listar(
            @RequestParam(required = false) Long canchaId,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) EstadoReserva estado,
            @RequestParam(required = false) TipoReserva tipo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @Valid PageParams params) {
        return ApiResponses.page(service.listar(new ReservaFiltro(canchaId, clienteId, estado, tipo, desde, hasta),
                params.toQuery("inicio", "id", "inicio", "fin", "estado", "montoTotal", "createdAt")),
                "Reservas obtenidas");
    }

    @GetMapping("/reservas/disponibilidad")
    public ResponseEntity<PageResponse<SlotDto>> disponibilidad(
            @RequestParam Long canchaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ApiResponses.list(service.disponibilidad(canchaId, fecha), "Disponibilidad del día");
    }

    @GetMapping("/reservas/{id}")
    public ResponseEntity<PageResponse<ReservaDto>> obtener(@PathVariable Long id) {
        return ApiResponses.ok(service.obtener(id), "Reserva encontrada");
    }

    @PutMapping("/reservas/{id}/reprogramar")
    public ResponseEntity<PageResponse<ReservaDto>> reprogramar(@PathVariable Long id,
                                                                @Valid @RequestBody ReprogramarCommand command) {
        return ApiResponses.ok(service.reprogramar(id, command), "Reserva reprogramada");
    }

    @PostMapping("/reservas/{id}/cancelar")
    public ResponseEntity<PageResponse<ReservaDto>> cancelar(@PathVariable Long id,
                                                             @Valid @RequestBody CancelarReservaCommand command) {
        return ApiResponses.ok(service.cancelar(id, command), "Reserva cancelada");
    }

    @PostMapping("/reservas/{id}/no-asistio")
    public ResponseEntity<PageResponse<ReservaDto>> noAsistio(@PathVariable Long id) {
        return ApiResponses.ok(service.marcarNoAsistio(id), "Reserva marcada como NO_ASISTIO");
    }

    @PostMapping("/reservas/{id}/completar")
    public ResponseEntity<PageResponse<ReservaDto>> completar(@PathVariable Long id) {
        return ApiResponses.ok(service.completar(id), "Reserva completada");
    }

    @PatchMapping("/reservas/{id}/monto")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<ReservaDto>> cambiarMonto(@PathVariable Long id,
                                                                 @Valid @RequestBody CambiarMontoCommand command) {
        return ApiResponses.ok(service.cambiarMonto(id, command), "Monto actualizado");
    }

    @PostMapping("/reservas/{id}/pagos")
    public ResponseEntity<PageResponse<PagoReservaDto>> registrarPago(@PathVariable Long id,
                                                                      @Valid @RequestBody PagoReservaCommand command) {
        return ApiResponses.created(service.registrarPago(id, command), "Pago registrado");
    }

    @GetMapping("/reservas/{id}/pagos")
    public ResponseEntity<PageResponse<PagoReservaDto>> listarPagos(@PathVariable Long id) {
        return ApiResponses.list(service.listarPagos(id), "Pagos obtenidos");
    }

    @PostMapping("/mantenimientos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<ReservaDto>> crearMantenimiento(
            @Valid @RequestBody CreateMantenimientoCommand command) {
        return ApiResponses.created(service.crearMantenimiento(command), "Mantenimiento registrado");
    }
}
