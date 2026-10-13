package nestoredidev.cajacancha.infrastructure.adapter.input.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nestoredidev.cajacancha.application.ports.input.CajaCanchaServicePort;
import nestoredidev.cajacancha.application.ports.input.dto.*;
import nestoredidev.common.infrastructure.web.ApiResponses;
import nestoredidev.common.infrastructure.web.PageParams;
import nestoredidev.common.utils.pagination.PageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/cancha/cajas")
@RequiredArgsConstructor
public class CajaCanchaController {
    private final CajaCanchaServicePort service;

    @PostMapping("/abrir")
    public ResponseEntity<PageResponse<CajaCanchaDto>> abrir(@Valid @RequestBody AbrirCajaCommand command) {
        return ApiResponses.created(service.abrir(command), "Caja abierta");
    }

    @GetMapping("/actual")
    public ResponseEntity<PageResponse<CajaCanchaDto>> actual() {
        return ApiResponses.ok(service.actual(), "Caja abierta actual");
    }

    @PostMapping("/cerrar")
    public ResponseEntity<PageResponse<CajaCanchaDto>> cerrar(@Valid @RequestBody CerrarCajaCommand command) {
        return ApiResponses.ok(service.cerrar(command), "Caja cerrada");
    }

    @GetMapping
    public ResponseEntity<PageResponse<CajaCanchaDto>> listar(@Valid PageParams params) {
        return ApiResponses.page(service.listar(params.toQuery("abiertaAt", "id", "fecha", "estado", "abiertaAt")),
                "Cajas obtenidas");
    }

    @GetMapping("/{id}")
    public ResponseEntity<PageResponse<CajaCanchaDto>> obtener(@PathVariable Long id) {
        return ApiResponses.ok(service.obtener(id), "Caja encontrada");
    }

    @PostMapping("/movimientos")
    public ResponseEntity<PageResponse<MovimientoCajaDto>> registrarMovimiento(
            @Valid @RequestBody MovimientoCajaCommand command) {
        return ApiResponses.created(service.registrarMovimiento(command), "Movimiento registrado");
    }

    @GetMapping("/{id}/movimientos")
    public ResponseEntity<PageResponse<MovimientoCajaDto>> movimientos(@PathVariable Long id,
                                                                       @Valid PageParams params) {
        return ApiResponses.page(service.listarMovimientos(id,
                params.toQuery("fecha", "id", "fecha", "tipo", "monto")), "Movimientos obtenidos");
    }
}
