package nestoredidev.bodega.producto.application.ports.input.dto;

import jakarta.validation.constraints.*;
import nestoredidev.bodega.producto.domain.model.TipoProducto;

import java.math.BigDecimal;

/** Cuerpo para crear y actualizar un producto. {@code stockInicial} solo se usa al crear. */
public record ProductoCommand(
        @Size(max = 30, message = "codigoBarras no debe exceder 30 caracteres")
        String codigoBarras,

        @NotBlank(message = "nombre es obligatorio")
        @Size(max = 120, message = "nombre no debe exceder 120 caracteres")
        String nombre,

        @NotNull(message = "categoriaId es obligatorio") Long categoriaId,
        TipoProducto tipo,

        @NotNull(message = "precioCompra es obligatorio")
        @DecimalMin(value = "0.00", message = "precioCompra no puede ser negativo")
        @DecimalMax(value = "99999999.99", message = "precioCompra es demasiado grande")
        BigDecimal precioCompra,

        @NotNull(message = "precioVenta es obligatorio")
        @DecimalMin(value = "0.00", message = "precioVenta no puede ser negativo")
        @DecimalMax(value = "99999999.99", message = "precioVenta es demasiado grande")
        BigDecimal precioVenta,

        @Min(value = 0, message = "stockMinimo no puede ser negativo") Integer stockMinimo,
        @Min(value = 1, message = "unidadesPorCaja debe ser al menos 1") Integer unidadesPorCaja,
        Long envaseId,
        @Min(value = 0, message = "stockInicial no puede ser negativo") Integer stockInicial
) {
}
