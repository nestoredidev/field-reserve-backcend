package nestoredidev.usuario.infrastructure.adapter.input.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nestoredidev.common.infrastructure.web.ApiResponses;
import nestoredidev.common.infrastructure.web.PageParams;
import nestoredidev.common.utils.pagination.PageResponse;
import nestoredidev.usuario.application.ports.input.UsuarioServicePort;
import nestoredidev.usuario.application.ports.input.dto.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/usuarios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioController {
    private final UsuarioServicePort service;

    @PostMapping
    public ResponseEntity<PageResponse<UsuarioDto>> crear(@Valid @RequestBody CreateUsuarioCommand command) {
        return ApiResponses.created(service.crear(command), "Usuario creado");
    }

    @GetMapping
    public ResponseEntity<PageResponse<UsuarioDto>> listar(@Valid PageParams params) {
        return ApiResponses.page(service.listar(params.toQuery("username", "id", "nombre", "username", "rol",
                "activo", "createdAt")), "Usuarios obtenidos");
    }

    @GetMapping("/{id}")
    public ResponseEntity<PageResponse<UsuarioDto>> obtener(@PathVariable Long id) {
        return ApiResponses.ok(service.obtener(id), "Usuario encontrado");
    }

    @PutMapping("/{id}")
    public ResponseEntity<PageResponse<UsuarioDto>> actualizar(@PathVariable Long id,
                                                               @Valid @RequestBody UpdateUsuarioCommand command) {
        return ApiResponses.ok(service.actualizar(id, command), "Usuario actualizado");
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<Void> cambiarPassword(@PathVariable Long id,
                                                @Valid @RequestBody ChangePasswordCommand command) {
        service.cambiarPassword(id, command);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activar")
    public ResponseEntity<PageResponse<UsuarioDto>> activar(@PathVariable Long id) {
        return ApiResponses.ok(service.activar(id), "Usuario activado");
    }

    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<PageResponse<UsuarioDto>> desactivar(@PathVariable Long id) {
        return ApiResponses.ok(service.desactivar(id), "Usuario desactivado");
    }
}
