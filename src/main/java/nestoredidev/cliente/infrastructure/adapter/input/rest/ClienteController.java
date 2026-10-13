package nestoredidev.cliente.infrastructure.adapter.input.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nestoredidev.cliente.application.ports.input.ClienteServicePort;
import nestoredidev.cliente.application.ports.input.dto.ClienteCommand;
import nestoredidev.cliente.application.ports.input.dto.ClienteDto;
import nestoredidev.common.infrastructure.web.ApiResponses;
import nestoredidev.common.infrastructure.web.PageParams;
import nestoredidev.common.utils.pagination.PageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/clientes")
@RequiredArgsConstructor
public class ClienteController {
    private final ClienteServicePort service;

    @PostMapping
    public ResponseEntity<PageResponse<ClienteDto>> crear(@Valid @RequestBody ClienteCommand command) {
        return ApiResponses.created(service.crear(command), "Cliente creado");
    }

    @GetMapping
    public ResponseEntity<PageResponse<ClienteDto>> listar(@RequestParam(required = false) String q,
                                                           @Valid PageParams params) {
        return ApiResponses.page(service.listar(q, params.toQuery("nombre", "id", "nombre", "telefono", "createdAt")),
                "Clientes obtenidos");
    }

    @GetMapping("/{id}")
    public ResponseEntity<PageResponse<ClienteDto>> obtener(@PathVariable Long id) {
        return ApiResponses.ok(service.obtener(id), "Cliente encontrado");
    }

    @GetMapping("/telefono/{telefono}")
    public ResponseEntity<PageResponse<ClienteDto>> obtenerPorTelefono(@PathVariable String telefono) {
        return ApiResponses.ok(service.obtenerPorTelefono(telefono), "Cliente encontrado");
    }

    @PutMapping("/{id}")
    public ResponseEntity<PageResponse<ClienteDto>> actualizar(@PathVariable Long id,
                                                               @Valid @RequestBody ClienteCommand command) {
        return ApiResponses.ok(service.actualizar(id, command), "Cliente actualizado");
    }
}
