package nestoredidev.cancha.application.ports.input.dto;

import nestoredidev.cancha.domain.model.Cancha;

import java.time.OffsetDateTime;

public record CanchaDto(Long id, String nombre, String descripcion, boolean activa, OffsetDateTime createdAt) {
    public static CanchaDto fromDomain(Cancha c) {
        return new CanchaDto(c.getId(), c.getNombre(), c.getDescripcion(), c.isActiva(), c.getCreatedAt());
    }
}
