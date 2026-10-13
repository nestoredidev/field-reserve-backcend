package nestoredidev.bodega.caja.infrastructure.adapter.input.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nestoredidev.bodega.caja.application.ports.input.CajaBodegaServicePort;
import nestoredidev.bodega.caja.application.ports.input.dto.*;
import nestoredidev.common.infrastructure.web.ApiResponses;
import nestoredidev.common.infrastructure.web.PageParams;
import nestoredidev.common.utils.pagination.PageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/bodega/cajas")
@RequiredArgsConstructor
public class CajaBodegaController {
    private final CajaBodegaServicePort service;

    @PostMapping("/abrir")
    public ResponseEntity<PageResponse<CajaBodegaDto>> abrir(@Valid @RequestBody AbrirCajaCommand command) {
        return ApiResponses.created(service.abrir(command), "Caja abierta");
    }

    @GetMapping("/actual")
    public ResponseEntity<PageResponse<CajaBodegaDto>> actual() {
        return ApiResponses.ok(service.actual(), "Caja abierta actual");
    }

    @PostMapping("/cerrar")
    public ResponseEntity<PageResponse<CajaBodegaDto>> cerrar(@Valid @RequestBody CerrarCajaCommand command) {
        return ApiResponses.ok(service.cerrar(command), "Caja cerrada");
    }

    @GetMapping
    public ResponseEntity<PageResponse<CajaBodegaDto>> listar(@Valid PageParams params) {
        return ApiResponses.page(service.listar(params.toQuery("abiertaAt", "id", "fecha", "estado", "abiertaAt")),
                "Cajas obtenidas");
    }

    @GetMapping("/{id}")
    public ResponseEntity<PageResponse<CajaBodegaDto>> obtener(@PathVariable Long id) {
        return ApiResponses.ok(service.obtener(id), "Caja encontrada");
    }

    @PostMapping("/movimientos")
    public ResponseEntity<PageResponse<MovimientoBodegaDto>> registrarMovimiento(
            @Valid @RequestBody MovimientoBodegaCommand command) {
        return ApiResponses.created(service.registrarMovimiento(command), "Movimiento registrado");
    }

    @GetMapping("/{id}/movimientos")
    public ResponseEntity<PageResponse<MovimientoBodegaDto>> movimientos(@PathVariable Long id,
                                                                       @Valid PageParams params) {
        return ApiResponses.page(service.listarMovimientos(id,
                params.toQuery("fecha", "id", "fecha", "tipo", "monto")), "Movimientos obtenidos");
    }
}
