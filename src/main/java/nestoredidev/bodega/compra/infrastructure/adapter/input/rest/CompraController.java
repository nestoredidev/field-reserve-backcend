package nestoredidev.bodega.compra.infrastructure.adapter.input.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nestoredidev.bodega.compra.application.ports.input.CompraServicePort;
import nestoredidev.bodega.compra.application.ports.input.dto.AnularCommand;
import nestoredidev.bodega.compra.application.ports.input.dto.CompraDto;
import nestoredidev.bodega.compra.application.ports.input.dto.CreateCompraCommand;
import nestoredidev.bodega.compra.domain.model.EstadoCompra;
import nestoredidev.common.infrastructure.web.ApiResponses;
import nestoredidev.common.infrastructure.web.PageParams;
import nestoredidev.common.utils.pagination.PageResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v2/bodega/compras")
@RequiredArgsConstructor
public class CompraController {
    private final CompraServicePort service;

    @PostMapping
    public ResponseEntity<PageResponse<CompraDto>> crear(@Valid @RequestBody CreateCompraCommand command) {
        return ApiResponses.created(service.crear(command), "Compra registrada");
    }

    @GetMapping
    public ResponseEntity<PageResponse<CompraDto>> listar(
            @RequestParam(required = false) Long proveedorId,
            @RequestParam(required = false) EstadoCompra estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @Valid PageParams params) {
        return ApiResponses.page(service.listar(proveedorId, estado, desde, hasta,
                params.toQuery("fecha", "id", "fecha", "total", "estado")), "Compras obtenidas");
    }

    @GetMapping("/{id}")
    public ResponseEntity<PageResponse<CompraDto>> obtener(@PathVariable Long id) {
        return ApiResponses.ok(service.obtener(id), "Compra encontrada");
    }

    @PostMapping("/{id}/anular")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<CompraDto>> anular(@PathVariable Long id,
                                                          @Valid @RequestBody AnularCommand command) {
        return ApiResponses.ok(service.anular(id, command), "Compra anulada");
    }
}
