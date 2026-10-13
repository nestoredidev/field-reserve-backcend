package nestoredidev.bodega.producto.infrastructure.adapter.input.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nestoredidev.bodega.producto.application.ports.input.InventarioServicePort;
import nestoredidev.bodega.producto.application.ports.input.ProductoServicePort;
import nestoredidev.bodega.producto.application.ports.input.dto.AjusteInventarioCommand;
import nestoredidev.bodega.producto.application.ports.input.dto.MovimientoInventarioDto;
import nestoredidev.bodega.producto.application.ports.input.dto.ProductoCommand;
import nestoredidev.bodega.producto.application.ports.input.dto.ProductoDto;
import nestoredidev.bodega.producto.domain.model.TipoMovimientoInventario;
import nestoredidev.bodega.producto.domain.model.TipoProducto;
import nestoredidev.common.infrastructure.web.ApiResponses;
import nestoredidev.common.infrastructure.web.PageParams;
import nestoredidev.common.utils.pagination.PageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/bodega")
@RequiredArgsConstructor
public class ProductoController {
    private final ProductoServicePort productoService;
    private final InventarioServicePort inventarioService;

    // ---- Productos (escritura solo ADMIN: RN-PRO-05) ----

    @PostMapping("/productos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<ProductoDto>> crear(@Valid @RequestBody ProductoCommand command) {
        return ApiResponses.created(productoService.crear(command), "Producto creado");
    }

    @GetMapping("/productos")
    public ResponseEntity<PageResponse<ProductoDto>> listar(@RequestParam(required = false) String q,
                                                            @RequestParam(required = false) Long categoriaId,
                                                            @RequestParam(required = false) TipoProducto tipo,
                                                            @RequestParam(required = false) Boolean activo,
                                                            @Valid PageParams params) {
        return ApiResponses.page(productoService.listar(q, categoriaId, tipo, activo,
                params.toQuery("nombre", "id", "nombre", "codigoBarras", "precioVenta", "stock", "createdAt")),
                "Productos obtenidos");
    }

    @GetMapping("/productos/stock-bajo")
    public ResponseEntity<PageResponse<ProductoDto>> stockBajo() {
        return ApiResponses.list(productoService.stockBajo(), "Productos para reponer");
    }

    @GetMapping("/productos/codigo-barras/{codigo}")
    public ResponseEntity<PageResponse<ProductoDto>> porCodigoBarras(@PathVariable String codigo) {
        return ApiResponses.ok(productoService.obtenerPorCodigoBarras(codigo), "Producto encontrado");
    }

    @GetMapping("/productos/{id}")
    public ResponseEntity<PageResponse<ProductoDto>> obtener(@PathVariable Long id) {
        return ApiResponses.ok(productoService.obtener(id), "Producto encontrado");
    }

    @PutMapping("/productos/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<ProductoDto>> actualizar(@PathVariable Long id,
                                                                @Valid @RequestBody ProductoCommand command) {
        return ApiResponses.ok(productoService.actualizar(id, command), "Producto actualizado");
    }

    @PatchMapping("/productos/{id}/activar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<ProductoDto>> activar(@PathVariable Long id) {
        return ApiResponses.ok(productoService.activar(id), "Producto activado");
    }

    @PatchMapping("/productos/{id}/desactivar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<ProductoDto>> desactivar(@PathVariable Long id) {
        return ApiResponses.ok(productoService.desactivar(id), "Producto desactivado");
    }

    // ---- Kardex ----

    @PostMapping("/inventario/ajustes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<MovimientoInventarioDto>> ajustar(
            @Valid @RequestBody AjusteInventarioCommand command) {
        return ApiResponses.created(inventarioService.ajustar(command), "Movimiento de inventario registrado");
    }

    @GetMapping("/inventario/movimientos")
    public ResponseEntity<PageResponse<MovimientoInventarioDto>> movimientos(
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) TipoMovimientoInventario tipo,
            @Valid PageParams params) {
        return ApiResponses.page(inventarioService.listarMovimientos(productoId, tipo,
                params.toQuery("fecha", "id", "fecha", "tipo", "cantidad")), "Kardex obtenido");
    }
}
