package nestoredidev.usuario.application.ports.input.dto;

import nestoredidev.common.domain.Rol;
import nestoredidev.usuario.domain.model.Usuario;

import java.time.OffsetDateTime;

/** Nunca incluye el hash (RN-USU-02). */
public record UsuarioDto(Long id, String nombre, String username, Rol rol, boolean activo, OffsetDateTime createdAt) {
    public static UsuarioDto fromDomain(Usuario u) {
        return new UsuarioDto(u.getId(), u.getNombre(), u.getUsername(), u.getRol(), u.isActivo(), u.getCreatedAt());
    }
}
