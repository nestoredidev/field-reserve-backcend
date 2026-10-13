package nestoredidev.bodega.proveedor.infrastructure.adapter.input.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nestoredidev.bodega.proveedor.application.ports.input.ProveedorServicePort;
import nestoredidev.bodega.proveedor.application.ports.input.dto.ProveedorCommand;
import nestoredidev.bodega.proveedor.application.ports.input.dto.ProveedorDto;
import nestoredidev.common.infrastructure.web.ApiResponses;
import nestoredidev.common.infrastructure.web.PageParams;
import nestoredidev.common.utils.pagination.PageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/bodega/proveedores")
@RequiredArgsConstructor
public class ProveedorController {
    private final ProveedorServicePort service;

    @PostMapping
    public ResponseEntity<PageResponse<ProveedorDto>> crear(@Valid @RequestBody ProveedorCommand command) {
        return ApiResponses.created(service.crear(command), "Proveedor creado");
    }

    @GetMapping
    public ResponseEntity<PageResponse<ProveedorDto>> listar(@RequestParam(required = false) String q,
                                                             @RequestParam(required = false) Boolean activo,
                                                             @Valid PageParams params) {
        return ApiResponses.page(service.listar(q, activo,
                params.toQuery("nombre", "id", "nombre", "ruc", "activo", "createdAt")), "Proveedores obtenidos");
    }

    @GetMapping("/{id}")
    public ResponseEntity<PageResponse<ProveedorDto>> obtener(@PathVariable Long id) {
        return ApiResponses.ok(service.obtener(id), "Proveedor encontrado");
    }

    @PutMapping("/{id}")
    public ResponseEntity<PageResponse<ProveedorDto>> actualizar(@PathVariable Long id,
                                                                 @Valid @RequestBody ProveedorCommand command) {
        return ApiResponses.ok(service.actualizar(id, command), "Proveedor actualizado");
    }

    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<ProveedorDto>> activar(@PathVariable Long id) {
        return ApiResponses.ok(service.activar(id), "Proveedor activado");
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<ProveedorDto>> desactivar(@PathVariable Long id) {
        return ApiResponses.ok(service.desactivar(id), "Proveedor desactivado");
    }
}
