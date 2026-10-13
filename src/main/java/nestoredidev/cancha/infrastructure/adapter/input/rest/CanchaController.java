package nestoredidev.cancha.infrastructure.adapter.input.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nestoredidev.cancha.application.ports.input.CanchaServicePort;
import nestoredidev.cancha.application.ports.input.TarifaServicePort;
import nestoredidev.cancha.application.ports.input.dto.*;
import nestoredidev.common.infrastructure.web.ApiResponses;
import nestoredidev.common.infrastructure.web.PageParams;
import nestoredidev.common.utils.pagination.PageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2")
@RequiredArgsConstructor
public class CanchaController {
    private final CanchaServicePort canchaService;
    private final TarifaServicePort tarifaService;

    // ---- Canchas ----

    @PostMapping("/canchas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<CanchaDto>> crear(@Valid @RequestBody CanchaCommand command) {
        return ApiResponses.created(canchaService.crear(command), "Cancha creada");
    }

    @GetMapping("/canchas")
    public ResponseEntity<PageResponse<CanchaDto>> listar(@RequestParam(required = false) Boolean activa,
                                                          @Valid PageParams params) {
        return ApiResponses.page(canchaService.listar(activa,
                params.toQuery("nombre", "id", "nombre", "activa", "createdAt")), "Canchas obtenidas");
    }

    @GetMapping("/canchas/{id}")
    public ResponseEntity<PageResponse<CanchaDto>> obtener(@PathVariable Long id) {
        return ApiResponses.ok(canchaService.obtener(id), "Cancha encontrada");
    }

    @PutMapping("/canchas/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<CanchaDto>> actualizar(@PathVariable Long id,
                                                              @Valid @RequestBody CanchaCommand command) {
        return ApiResponses.ok(canchaService.actualizar(id, command), "Cancha actualizada");
    }

    @PatchMapping("/canchas/{id}/activar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<CanchaDto>> activar(@PathVariable Long id) {
        return ApiResponses.ok(canchaService.activar(id), "Cancha activada");
    }

    @PatchMapping("/canchas/{id}/desactivar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<CanchaDto>> desactivar(@PathVariable Long id) {
        return ApiResponses.ok(canchaService.desactivar(id), "Cancha desactivada");
    }

    // ---- Tarifas ----

    @PostMapping("/canchas/{canchaId}/tarifas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<TarifaDto>> crearTarifa(@PathVariable Long canchaId,
                                                               @Valid @RequestBody CreateTarifaCommand command) {
        return ApiResponses.created(tarifaService.crear(canchaId, command), "Tarifa creada");
    }

    @GetMapping("/canchas/{canchaId}/tarifas")
    public ResponseEntity<PageResponse<TarifaDto>> listarTarifas(
            @PathVariable Long canchaId, @RequestParam(defaultValue = "true") boolean soloActivas) {
        return ApiResponses.list(tarifaService.listar(canchaId, soloActivas), "Tarifas obtenidas");
    }

    @GetMapping("/tarifas/{id}")
    public ResponseEntity<PageResponse<TarifaDto>> obtenerTarifa(@PathVariable Long id) {
        return ApiResponses.ok(tarifaService.obtener(id), "Tarifa encontrada");
    }

    @PatchMapping("/tarifas/{id}/desactivar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<TarifaDto>> desactivarTarifa(@PathVariable Long id) {
        return ApiResponses.ok(tarifaService.desactivar(id), "Tarifa desactivada");
    }

    @PostMapping("/tarifas/{id}/cambiar-precio")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<TarifaDto>> cambiarPrecio(@PathVariable Long id,
                                                                 @Valid @RequestBody CambiarPrecioCommand command) {
        return ApiResponses.created(tarifaService.cambiarPrecio(id, command),
                "Tarifa anterior desactivada y nueva tarifa creada");
    }
}
