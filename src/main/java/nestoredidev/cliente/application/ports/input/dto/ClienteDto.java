package nestoredidev.cliente.application.ports.input.dto;

import nestoredidev.cliente.domain.model.Cliente;

import java.time.OffsetDateTime;

public record ClienteDto(Long id, String nombre, String telefono, String notas, OffsetDateTime createdAt) {
    public static ClienteDto fromDomain(Cliente c) {
        return new ClienteDto(c.getId(), c.getNombre(), c.getTelefono(), c.getNotas(), c.getCreatedAt());
    }
}
