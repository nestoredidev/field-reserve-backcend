package nestoredidev.bodega.categoria.infrastructure.adapter.input.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nestoredidev.bodega.categoria.application.ports.input.CategoriaServicePort;
import nestoredidev.bodega.categoria.application.ports.input.dto.CategoriaCommand;
import nestoredidev.bodega.categoria.application.ports.input.dto.CategoriaDto;
import nestoredidev.common.infrastructure.web.ApiResponses;
import nestoredidev.common.infrastructure.web.PageParams;
import nestoredidev.common.utils.pagination.PageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/bodega/categorias")
@RequiredArgsConstructor
public class CategoriaController {
    private final CategoriaServicePort service;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<CategoriaDto>> crear(@Valid @RequestBody CategoriaCommand command) {
        return ApiResponses.created(service.crear(command), "Categoría creada");
    }

    @GetMapping
    public ResponseEntity<PageResponse<CategoriaDto>> listar(@Valid PageParams params) {
        return ApiResponses.page(service.listar(params.toQuery("nombre", "id", "nombre")), "Categorías obtenidas");
    }

    @GetMapping("/{id}")
    public ResponseEntity<PageResponse<CategoriaDto>> obtener(@PathVariable Long id) {
        return ApiResponses.ok(service.obtener(id), "Categoría encontrada");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<CategoriaDto>> actualizar(@PathVariable Long id,
                                                                 @Valid @RequestBody CategoriaCommand command) {
        return ApiResponses.ok(service.actualizar(id, command), "Categoría actualizada");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
