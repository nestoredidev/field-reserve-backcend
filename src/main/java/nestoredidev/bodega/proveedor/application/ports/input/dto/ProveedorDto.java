package nestoredidev.bodega.proveedor.application.ports.input.dto;

import nestoredidev.bodega.proveedor.domain.model.Proveedor;

import java.time.OffsetDateTime;

public record ProveedorDto(Long id, String nombre, String ruc, String telefono, boolean activo,
                           OffsetDateTime createdAt) {
    public static ProveedorDto fromDomain(Proveedor p) {
        return new ProveedorDto(p.getId(), p.getNombre(), p.getRuc(), p.getTelefono(), p.isActivo(), p.getCreatedAt());
    }
}
