package nestoredidev.bodega.venta.infrastructure.adapter.input.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nestoredidev.bodega.compra.application.ports.input.dto.AnularCommand;
import nestoredidev.bodega.venta.application.ports.input.VentaServicePort;
import nestoredidev.bodega.venta.application.ports.input.dto.CreateVentaCommand;
import nestoredidev.bodega.venta.application.ports.input.dto.VentaDto;
import nestoredidev.bodega.venta.domain.model.EstadoVenta;
import nestoredidev.common.infrastructure.web.ApiResponses;
import nestoredidev.common.infrastructure.web.PageParams;
import nestoredidev.common.utils.pagination.PageResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v2/bodega/ventas")
@RequiredArgsConstructor
public class VentaController {
    private final VentaServicePort service;

    @PostMapping
    public ResponseEntity<PageResponse<VentaDto>> crear(@Valid @RequestBody CreateVentaCommand command) {
        return ApiResponses.created(service.crear(command), "Venta registrada");
    }

    @GetMapping
    public ResponseEntity<PageResponse<VentaDto>> listar(
            @RequestParam(required = false) Long cajaId,
            @RequestParam(required = false) EstadoVenta estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @Valid PageParams params) {
        return ApiResponses.page(service.listar(cajaId, estado, desde, hasta,
                params.toQuery("fecha", "id", "fecha", "total", "estado")), "Ventas obtenidas");
    }

    @GetMapping("/{id}")
    public ResponseEntity<PageResponse<VentaDto>> obtener(@PathVariable Long id) {
        return ApiResponses.ok(service.obtener(id), "Venta encontrada");
    }

    @PostMapping("/{id}/anular")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<VentaDto>> anular(@PathVariable Long id,
                                                         @Valid @RequestBody AnularCommand command) {
        return ApiResponses.ok(service.anular(id, command), "Venta anulada");
    }
}
